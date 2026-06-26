package service;

/**
 * DictionaryServiceImpl - Implementation của DictionaryService với Singleton pattern
 * Quản lý lưu trữ từ điển bằng file system (database/)
 */
import entities.Request;
import entities.Definition;
import entities.Sentence;
import entities.Word;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

public class DictionaryServiceImpl implements DictionaryService {

    private static final String DATABASE_DIR = "database";
    private static final String EXPORT_FILE = "dictionary.txt";
    private static final String SYNONYMS_NOUN_FILE = "synonymsNoun.def";
    private static final String SYNONYMS_VERB_FILE = "synonymsVerb.def";
    private static final String SYNONYMS_ADJECTIVE_FILE = "synonymsAdjective.def";
    private static final String SYNONYMS_ADVERB_FILE = "synonymsAdverb.def";
    private static DictionaryServiceImpl instance;

    private DictionaryServiceImpl() {
        ensureDatabaseDir();
    }

    public static DictionaryServiceImpl getInstance() {
        if (instance == null) {
            instance = new DictionaryServiceImpl();
        }

        return instance;
    }

    @Override
    public Word lookup(String keyword) {
        Path path = wordFile(keyword);

        if (!Files.exists(path)) {
            return null;
        }

        Word word = readWord(path);
        word.setSynonyms(readSynonyms(word));
        word.setAntonyms(readAntonyms(keyword));
        return word;
    }

    @Override
    public List<Word> lookupSimilar(String keyword) {
        ensureDatabaseDir();

        String search = keyword.toLowerCase(Locale.ROOT);
        List<Word> results = new ArrayList<>();

        try (Stream<Path> paths = Files.list(Paths.get(DATABASE_DIR))) {
            paths
                    .filter(this::isWordFile)
                    .map(path -> {
                        Word word = readWord(path);
                        word.setSynonyms(readSynonyms(word));
                        word.setAntonyms(readAntonyms(word.getKeyword()));
                        return word;
                    })
                    .filter(word -> wordMatches(word, search))
                    .sorted(Comparator.comparing(Word::getKeyword))
                    .forEach(results::add);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read database", e);
        }

        return results;
    }

    @Override
    public void save(Word word) {
        ensureDatabaseDir();

        Properties properties = new Properties();
        properties.setProperty("word", valueOrEmpty(word.getKeyword()));
        properties.setProperty("pronounce", valueOrEmpty(word.getPronunciation()));
        properties.setProperty("definition.count", String.valueOf(word.getDefinitions().size()));

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            java.util.LinkedList<Sentence> examples = definition.getExamples();
            String prefix = "definition." + i + ".";

            properties.setProperty(prefix + "type", valueOrEmpty(definition.getType()));
            properties.setProperty(prefix + "meaning", valueOrEmpty(definition.getMeaning()));
            properties.setProperty(prefix + "example.count", String.valueOf(examples != null ? examples.size() : 0));

            if (examples != null) {
                for (int j = 0; j < examples.size(); j++) {
                    Sentence sentence = examples.get(j);
                    String examplePrefix = prefix + "example." + j + ".";
                    properties.setProperty(examplePrefix + "content", valueOrEmpty(sentence.getContent()));
                    properties.setProperty(examplePrefix + "meaning", valueOrEmpty(sentence.getMeaning()));
                }
            }
        }

        try (FileOutputStream output = new FileOutputStream(wordFile(word.getKeyword()).toFile())) {
            properties.store(output, "Dictionary word");
        } catch (IOException e) {
            throw new RuntimeException("Cannot save word: " + word.getKeyword(), e);
        }

        // Save antonyms to per-word file
        saveAntonyms(word.getKeyword(), word.getAntonyms());
    }

    @Override
    public void rename(String oldKeyword, String newKeyword) {
        ensureDatabaseDir();

        Path oldWordPath = wordFile(oldKeyword);
        Path newWordPath = wordFile(newKeyword);

        if (!Files.exists(oldWordPath)) {
            throw new IllegalArgumentException("Word not found: " + oldKeyword);
        }

        if (!samePath(oldWordPath, newWordPath) && Files.exists(newWordPath)) {
            throw new IllegalArgumentException("Word already exists: " + newKeyword);
        }

        Word word = readWord(oldWordPath);
        word.setKeyword(newKeyword);

        // Rename pronounce audio file if exists
        Path oldAudioPath = Paths.get(DATABASE_DIR, sanitizeFileName(oldKeyword) + ".mp3");
        Path newAudioPath = Paths.get(DATABASE_DIR, sanitizeFileName(newKeyword) + ".mp3");
        if (Files.exists(oldAudioPath) && !samePath(oldAudioPath, newAudioPath)) {
            try {
                Files.move(oldAudioPath, newAudioPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Cannot rename pronunciation audio", e);
            }
        }

        // Rename antonyms file if exists
        Path oldAntonymsPath = antonymFile(oldKeyword);
        Path newAntonymsPath = antonymFile(newKeyword);
        if (Files.exists(oldAntonymsPath) && !samePath(oldAntonymsPath, newAntonymsPath)) {
            try {
                Files.move(oldAntonymsPath, newAntonymsPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Cannot rename antonyms file", e);
            }
        }

        // Update synonyms in type-specific files
        updateSynonymsInFiles(word, oldKeyword, newKeyword);

        save(word);

        if (!samePath(oldWordPath, newWordPath)) {
            try {
                Files.deleteIfExists(oldWordPath);
            } catch (IOException e) {
                throw new RuntimeException("Cannot remove old word file: " + oldKeyword, e);
            }
        }
    }

    @Override
    public void drop(String keyword) {
        try {
            Files.deleteIfExists(wordFile(keyword));
            // Remove from synonyms files
            removeSynonymFromAllFiles(keyword);
            // Delete antonyms file
            Files.deleteIfExists(antonymFile(keyword));
            // Delete pronounce audio file if exists
            Path audioPath = Paths.get(DATABASE_DIR, sanitizeFileName(keyword) + ".mp3");
            Files.deleteIfExists(audioPath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot drop word: " + keyword, e);
        }
    }

    @Override
    public void export() {
        export(EXPORT_FILE);
    }

    @Override
    public void export(String path) {
        List<Word> words = allWords();

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8))) {
            for (Word word : words) {
                writeWord(writer, word);
                writer.println();
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot export dictionary", e);
        }
    }

    @Override
    public void copyPronounceFile(String keyword, String sourcePath) {
        Path source = Paths.get(sourcePath);

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("File not found: " + sourcePath);
        }

        Path target = Paths.get(DATABASE_DIR, sanitizeFileName(keyword) + ".mp3");

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy pronunciation file", e);
        }
    }

    @Override
    public void addSynonym(String keyword, String wordType, String synonymWord) {
        String normalizedType = normalizeWordType(wordType);
        if (normalizedType == null) {
            throw new IllegalArgumentException("Invalid word type for synonyms: " + wordType);
        }

        String fileName = getSynonymFileName(normalizedType);
        if (fileName == null) {
            throw new IllegalArgumentException("Cannot find synonym file for type: " + normalizedType);
        }

        Path filePath = Paths.get(DATABASE_DIR, fileName);
        Properties properties = new Properties();

        if (Files.exists(filePath)) {
            try (FileInputStream input = new FileInputStream(filePath.toFile())) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException("Cannot read synonyms file: " + fileName, e);
            }
        }

        // Find existing group containing the synonym word or create new group
        String sanitizedKeyword = sanitizeFileName(keyword).toLowerCase();
        String sanitizedSynonym = sanitizeFileName(synonymWord).toLowerCase();
        boolean found = false;

        for (String key : properties.stringPropertyNames()) {
            String[] words = properties.getProperty(key, "").split("=");
            Set<String> updatedWords = new LinkedHashSet<>();

            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                if (cleanWord.equals(sanitizedSynonym) || cleanWord.equals(sanitizedKeyword)) {
                    updatedWords.add(sanitizedKeyword);
                    updatedWords.add(sanitizedSynonym);
                    found = true;
                } else {
                    updatedWords.add(cleanWord);
                }
            }

            if (found) {
                properties.setProperty(key, String.join("=", updatedWords));
                break;
            }
        }

        if (!found) {
            // Create new group
            String newKey = "group" + (properties.stringPropertyNames().size() + 1);
            properties.setProperty(newKey, sanitizedKeyword + "=" + sanitizedSynonym);
        }

        try (FileOutputStream output = new FileOutputStream(filePath.toFile())) {
            properties.store(output, "Synonyms " + normalizedType);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save synonyms file: " + fileName, e);
        }
    }

    @Override
    public void removeSynonym(String keyword, String wordType) {
        String normalizedType = normalizeWordType(wordType);
        if (normalizedType == null) {
            throw new IllegalArgumentException("Invalid word type for synonyms: " + wordType);
        }

        String fileName = getSynonymFileName(normalizedType);
        if (fileName == null) {
            throw new IllegalArgumentException("Cannot find synonym file for type: " + normalizedType);
        }

        Path filePath = Paths.get(DATABASE_DIR, fileName);
        if (!Files.exists(filePath)) {
            return;
        }

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filePath.toFile())) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read synonyms file: " + fileName, e);
        }

        String sanitizedKeyword = sanitizeFileName(keyword).toLowerCase();
        boolean updated = false;

        for (String key : properties.stringPropertyNames()) {
            String[] words = properties.getProperty(key, "").split("=");
            Set<String> updatedWords = new LinkedHashSet<>();

            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                if (!cleanWord.equals(sanitizedKeyword)) {
                    updatedWords.add(cleanWord);
                } else {
                    updated = true;
                }
            }

            if (updated) {
                if (updatedWords.isEmpty()) {
                    properties.remove(key);
                } else {
                    properties.setProperty(key, String.join("=", updatedWords));
                }
                break;
            }
        }

        if (updated) {
            try (FileOutputStream output = new FileOutputStream(filePath.toFile())) {
                properties.store(output, "Synonyms " + normalizedType);
            } catch (IOException e) {
                throw new RuntimeException("Cannot save synonyms file: " + fileName, e);
            }
        }
    }

    @Override
    public void addAntonym(String keyword, String antonymWord, String antonymType) {
        Word word = lookup(keyword);
        if (word == null) {
            throw new IllegalArgumentException("Word not found: " + keyword);
        }

        Map<String, LinkedList<String>> antonyms = word.getAntonyms();
        if (antonyms == null) {
            antonyms = new HashMap<>();
            word.setAntonyms(antonyms);
        }

        LinkedList<String> typeAntonyms = antonyms.computeIfAbsent(antonymType, k -> new LinkedList<>());
        if (!typeAntonyms.contains(antonymWord)) {
            typeAntonyms.add(antonymWord);
        }

        saveAntonyms(keyword, antonyms);
    }

    @Override
    public void removeAntonym(String keyword, String antonymType) {
        Word word = lookup(keyword);
        if (word == null) {
            throw new IllegalArgumentException("Word not found: " + keyword);
        }

        Map<String, LinkedList<String>> antonyms = word.getAntonyms();
        if (antonyms != null) {
            antonyms.remove(antonymType);
            saveAntonyms(keyword, antonyms);
        }
    }

    private List<Word> allWords() {
        ensureDatabaseDir();

        List<Word> words = new ArrayList<>();

        try (Stream<Path> paths = Files.list(Paths.get(DATABASE_DIR))) {
            paths
                    .filter(this::isWordFile)
                    .map(path -> {
                        Word word = readWord(path);
                        word.setSynonyms(readSynonyms(word));
                        word.setAntonyms(readAntonyms(word.getKeyword()));
                        return word;
                    })
                    .sorted(Comparator.comparing(Word::getKeyword))
                    .forEach(words::add);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read database", e);
        }

        return words;
    }

    private Word readWord(Path path) {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(path.toFile())) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read word file: " + path, e);
        }

        Word word = new Word(properties.getProperty("word", ""));
        word.setPronunciation(properties.getProperty("pronounce", ""));
        readDefinitions(properties, word);

        return word;
    }

    private void writeWord(PrintWriter writer, Word word) {
        writer.println("═══════════════════════════════════════════════════════════════");
        writer.println("📖 " + valueOrEmpty(word.getKeyword()));
        if (!valueOrEmpty(word.getPronunciation()).isEmpty()) {
            writer.println("   🔊 " + valueOrEmpty(word.getPronunciation()));
        }

        writeMap(writer, "   🔗 Synonyms", word.getSynonyms());
        writeMap(writer, "   🔀 Antonyms", word.getAntonyms());

        // Group definitions by type
        java.util.Map<String, java.util.List<Definition>> groupedDefinitions = new java.util.LinkedHashMap<>();
        for (Definition definition : word.getDefinitions()) {
            String type = definition.getType();
            if (type == null || type.isBlank()) {
                type = "Khác";
            }
            groupedDefinitions.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(definition);
        }

        // Display grouped by type
        int defCount = 1;
        for (java.util.Map.Entry<String, java.util.List<Definition>> entry : groupedDefinitions.entrySet()) {
            String type = entry.getKey();
            java.util.List<Definition> definitions = entry.getValue();

            writer.println("   ┌─────────────────────────────────────────────────────");
            writer.println("   │ Loại từ: " + type);

            for (Definition definition : definitions) {
                java.util.LinkedList<Sentence> examples = definition.getExamples();

                writer.println("   │   Định nghĩa " + defCount + ": " + valueOrEmpty(definition.getMeaning()));

                if (examples != null && !examples.isEmpty()) {
                    for (int j = 0; j < examples.size(); j++) {
                        Sentence sentence = examples.get(j);
                        if (!valueOrEmpty(sentence.getContent()).isEmpty()) {
                            writer.println("   │      💬 " + valueOrEmpty(sentence.getContent()));
                            if (!valueOrEmpty(sentence.getMeaning()).isEmpty()) {
                                writer.println("   │         " + valueOrEmpty(sentence.getMeaning()));
                            }
                        }
                    }
                }
                defCount++;
            }
            writer.println("   └─────────────────────────────────────────────────────");
        }
        writer.println();
    }

    private void readDefinitions(Properties properties, Word word) {
        int count = parseInt(properties.getProperty("definition.count"), 0);

        for (int i = 0; i < count; i++) {
            String prefix = "definition." + i + ".";
            String type = properties.getProperty(prefix + "type", "");
            String meaning = properties.getProperty(prefix + "meaning", "");
            int exampleCount = parseInt(properties.getProperty(prefix + "example.count", "0"), 0);

            java.util.LinkedList<Sentence> examples = new java.util.LinkedList<>();
            for (int j = 0; j < exampleCount; j++) {
                String examplePrefix = prefix + "example." + j + ".";
                String content = properties.getProperty(examplePrefix + "content", "");
                String exampleMeaning = properties.getProperty(examplePrefix + "meaning", "");
                examples.add(new Sentence(content, exampleMeaning));
            }

            if (!isBlank(type) || !isBlank(meaning) || !examples.isEmpty()) {
                word.addDefinition(new Definition(type, meaning, examples));
            }
        }

        // Backward compatibility for old format
        if (word.getDefinitions().isEmpty()) {
            addDefinition(
                    word,
                    properties.getProperty("class", ""),
                    properties.getProperty("meaning", ""),
                    properties.getProperty("example", ""),
                    properties.getProperty("exampleMeaning", "")
            );
        }
    }

    private void addDefinition(Word word, String type, String meaning, String example, String exampleMeaning) {
        if (isBlank(type) && isBlank(meaning) && isBlank(example) && isBlank(exampleMeaning)) {
            return;
        }

        word.addDefinition(new Definition(type, meaning, new Sentence(example, exampleMeaning)));
    }

    private Properties loadRelationProperties(String fileName) {
        Properties properties = new Properties();
        Path path = Paths.get(DATABASE_DIR, fileName);

        try (FileInputStream input = new FileInputStream(path.toFile())) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read relation file: " + path, e);
        }

        return properties;
    }

    private Map<String, LinkedList<String>> readRelation(Properties properties, String keyword) {
        String sanitizedKeyword = sanitizeFileName(keyword).toLowerCase();
        Map<String, LinkedList<String>> result = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            String raw = properties.getProperty(key, "");
            String[] words = raw.split("=");

            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                if (cleanWord.equals(sanitizedKeyword)) {
                    // Found the group containing this keyword
                    // Extract word type from key (e.g., "noun1" -> "noun")
                    String wordType = extractWordType(key);
                    
                    LinkedList<String> synonyms = new LinkedList<>();
                    for (String w : words) {
                        String cleanW = w.trim();
                        if (!cleanW.isEmpty() && !cleanW.equalsIgnoreCase(keyword)) {
                            synonyms.add(cleanW);
                        }
                    }
                    
                    if (!synonyms.isEmpty()) {
                        result.put(wordType, synonyms);
                    }
                }
            }
        }

        return result;
    }

    private String extractWordType(String key) {
        // Extract word type from key like "noun1", "verb2", "adj3" -> "noun", "verb", "adj"
        for (int i = 0; i < key.length(); i++) {
            if (Character.isDigit(key.charAt(i))) {
                return key.substring(0, i).toLowerCase();
            }
        }
        return key.toLowerCase();
    }

    private void migrateRelationKeyword(String fileName, String oldKeyword, String newKeyword) {
        Properties properties = loadRelationProperties(fileName);
        replaceRelationValue(properties, oldKeyword, newKeyword);
        storeRelationProperties(fileName, properties);
    }

    private void removeRelationKeyword(String fileName, String keyword) {
        Properties properties = loadRelationProperties(fileName);
        removeRelationValue(properties, keyword);
        storeRelationProperties(fileName, properties);
    }

    private void replaceRelationValue(Properties properties, String oldKeyword, String newKeyword) {
        for (String key : properties.stringPropertyNames()) {
            String[] words = properties.getProperty(key, "").split("=");
            Set<String> updatedWords = new LinkedHashSet<>();
            boolean found = false;

            for (String word : words) {
                String cleanWord = word.trim();
                if (!cleanWord.isEmpty()) {
                    if (sameKeyword(cleanWord, oldKeyword)) {
                        updatedWords.add(newKeyword);
                        found = true;
                    } else {
                        updatedWords.add(cleanWord);
                    }
                }
            }

            if (found) {
                if (updatedWords.isEmpty()) {
                    properties.remove(key);
                } else {
                    properties.setProperty(key, String.join("=", updatedWords));
                }
            }
        }
    }

    private void removeRelationValue(Properties properties, String keyword) {
        for (String key : properties.stringPropertyNames()) {
            String[] words = properties.getProperty(key, "").split("=");
            Set<String> updatedWords = new LinkedHashSet<>();
            boolean found = false;

            for (String word : words) {
                String cleanWord = word.trim();
                if (!cleanWord.isEmpty()) {
                    if (!sameKeyword(cleanWord, keyword)) {
                        updatedWords.add(cleanWord);
                    } else {
                        found = true;
                    }
                }
            }

            if (found) {
                if (updatedWords.isEmpty()) {
                    properties.remove(key);
                } else {
                    properties.setProperty(key, String.join("=", updatedWords));
                }
            }
        }
    }

    private Set<String> parseValues(String raw) {
        Set<String> values = new LinkedHashSet<>();

        for (String value : valueOrEmpty(raw).split("=")) {
            String clean = value.trim();

            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }

        return values;
    }

    private String joinValues(Set<String> values) {
        return String.join("=", values);
    }

    private void storeRelationProperties(String fileName, Properties properties) {
        Path path = Paths.get(DATABASE_DIR, fileName);

        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            properties.store(output, "Dictionary relation");
        } catch (IOException e) {
            throw new RuntimeException("Cannot save relation file: " + path, e);
        }
    }

    private void writeList(PrintWriter writer, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            writer.println(label + ": " + String.join(", ", values));
        }
    }

    private void writeMap(PrintWriter writer, String label, Map<String, LinkedList<String>> map) {
        if (map != null && !map.isEmpty()) {
            for (Map.Entry<String, LinkedList<String>> entry : map.entrySet()) {
                String wordType = entry.getKey();
                LinkedList<String> values = entry.getValue();
                if (!values.isEmpty()) {
                    writer.println(label + " (" + wordType + "): " + String.join(", ", values));
                }
            }
        }
    }

    private boolean wordMatches(Word word, String search) {
        if (contains(word.getKeyword(), search)
                || mapContains(word.getSynonyms(), search)
                || mapContains(word.getAntonyms(), search)) {
            return true;
        }

        for (Definition definition : word.getDefinitions()) {
            Sentence sentence = definition.getSentence();

            if (contains(definition.getType(), search)
                    || contains(definition.getMeaning(), search)
                    || (sentence != null && (contains(sentence.getContent(), search)
                    || contains(sentence.getMeaning(), search)))) {
                return true;
            }
        }

        return false;
    }

    private boolean listContains(List<String> values, String search) {
        if (values == null) {
            return false;
        }

        for (String value : values) {
            if (contains(value, search)) {
                return true;
            }
        }

        return false;
    }

    private boolean mapContains(Map<String, LinkedList<String>> map, String search) {
        if (map == null) {
            return false;
        }

        for (LinkedList<String> values : map.values()) {
            if (listContains(values, search)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWordFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".def")
                && !fileName.equals(SYNONYMS_NOUN_FILE)
                && !fileName.equals(SYNONYMS_VERB_FILE)
                && !fileName.equals(SYNONYMS_ADJECTIVE_FILE)
                && !fileName.equals(SYNONYMS_ADVERB_FILE)
                && !fileName.endsWith("_antonym.def");
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean samePath(Path first, Path second) {
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    private boolean sameKeyword(String first, String second) {
        return valueOrEmpty(first).trim().equalsIgnoreCase(valueOrEmpty(second).trim());
    }

    private Path wordFile(String keyword) {
        return Paths.get(DATABASE_DIR, sanitizeFileName(keyword) + ".def");
    }

    private String sanitizeFileName(String value) {
        String clean = value.toLowerCase(Locale.ROOT).trim();

        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Invalid word");
        }

        if (clean.matches("[a-z0-9_-]+")) {
            return clean;
        }

        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(clean.getBytes(StandardCharsets.UTF_8));

        return "u_" + encoded;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0) {
            return "";
        }

        return fileName.substring(dotIndex);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void ensureDatabaseDir() {
        try {
            Files.createDirectories(Paths.get(DATABASE_DIR));
            createFileIfMissing(Paths.get(DATABASE_DIR, SYNONYMS_NOUN_FILE));
            createFileIfMissing(Paths.get(DATABASE_DIR, SYNONYMS_VERB_FILE));
            createFileIfMissing(Paths.get(DATABASE_DIR, SYNONYMS_ADJECTIVE_FILE));
            createFileIfMissing(Paths.get(DATABASE_DIR, SYNONYMS_ADVERB_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create database folder", e);
        }
    }

    private void createFileIfMissing(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    private Path antonymFile(String keyword) {
        return Paths.get(DATABASE_DIR, sanitizeFileName(keyword) + "_antonym.def");
    }

    private Map<String, LinkedList<String>> readSynonyms(Word word) {
        Map<String, LinkedList<String>> result = new HashMap<>();

        for (Definition definition : word.getDefinitions()) {
            String type = normalizeWordType(definition.getType());
            if (type == null) {
                continue; // Skip types that don't have synonym files
            }

            String fileName = getSynonymFileName(type);
            if (fileName == null) {
                continue;
            }

            Path filePath = Paths.get(DATABASE_DIR, fileName);
            if (!Files.exists(filePath)) {
                continue;
            }

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(filePath.toFile())) {
                properties.load(input);
            } catch (IOException e) {
                continue;
            }

            LinkedList<String> synonyms = findSynonymGroup(properties, word.getKeyword());
            if (synonyms != null && !synonyms.isEmpty()) {
                result.put(type, synonyms);
            }
        }

        return result;
    }

    private Map<String, LinkedList<String>> readAntonyms(String keyword) {
        Map<String, LinkedList<String>> result = new HashMap<>();
        Path filePath = antonymFile(keyword);

        if (!Files.exists(filePath)) {
            return result;
        }

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(filePath.toFile())) {
            properties.load(input);
        } catch (IOException e) {
            return result;
        }

        for (String key : properties.stringPropertyNames()) {
            String[] words = properties.getProperty(key, "").split("=");
            LinkedList<String> antonyms = new LinkedList<>();
            for (String word : words) {
                String cleanWord = word.trim();
                if (!cleanWord.isEmpty()) {
                    antonyms.add(cleanWord);
                }
            }
            if (!antonyms.isEmpty()) {
                result.put(key, antonyms);
            }
        }

        return result;
    }

    private void saveAntonyms(String keyword, Map<String, LinkedList<String>> antonyms) {
        if (antonyms == null || antonyms.isEmpty()) {
            try {
                Files.deleteIfExists(antonymFile(keyword));
            } catch (IOException e) {
                // Ignore
            }
            return;
        }

        Properties properties = new Properties();
        for (Map.Entry<String, LinkedList<String>> entry : antonyms.entrySet()) {
            String key = entry.getKey();
            LinkedList<String> values = entry.getValue();
            properties.setProperty(key, String.join("=", values));
        }

        try (FileOutputStream output = new FileOutputStream(antonymFile(keyword).toFile())) {
            properties.store(output, "Antonyms for " + keyword);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save antonyms for: " + keyword, e);
        }
    }

    private String normalizeWordType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        String lower = type.toLowerCase();
        if (lower.startsWith("noun") || lower.equals("n")) {
            return "noun";
        } else if (lower.startsWith("verb") || lower.equals("v")) {
            return "verb";
        } else if (lower.startsWith("adjective") || lower.startsWith("adj") || lower.equals("a")) {
            return "adjective";
        } else if (lower.startsWith("adverb") || lower.equals("adv")) {
            return "adverb";
        }

        return null;
    }

    private String getSynonymFileName(String type) {
        switch (type) {
            case "noun":
                return SYNONYMS_NOUN_FILE;
            case "verb":
                return SYNONYMS_VERB_FILE;
            case "adjective":
                return SYNONYMS_ADJECTIVE_FILE;
            case "adverb":
                return SYNONYMS_ADVERB_FILE;
            default:
                return null;
        }
    }

    private LinkedList<String> findSynonymGroup(Properties properties, String keyword) {
        String sanitizedKeyword = sanitizeFileName(keyword).toLowerCase();

        for (String key : properties.stringPropertyNames()) {
            String raw = properties.getProperty(key, "");
            String[] words = raw.split("=");

            for (String word : words) {
                String cleanWord = word.trim().toLowerCase();
                if (cleanWord.equals(sanitizedKeyword)) {
                    LinkedList<String> synonyms = new LinkedList<>();
                    for (String w : words) {
                        String cleanW = w.trim();
                        if (!cleanW.isEmpty() && !cleanW.equalsIgnoreCase(keyword)) {
                            synonyms.add(cleanW);
                        }
                    }
                    return synonyms;
                }
            }
        }

        return null;
    }

    private void updateSynonymsInFiles(Word word, String oldKeyword, String newKeyword) {
        for (Definition definition : word.getDefinitions()) {
            String type = normalizeWordType(definition.getType());
            if (type == null) {
                continue;
            }

            String fileName = getSynonymFileName(type);
            if (fileName == null) {
                continue;
            }

            Path filePath = Paths.get(DATABASE_DIR, fileName);
            if (!Files.exists(filePath)) {
                continue;
            }

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(filePath.toFile())) {
                properties.load(input);
            } catch (IOException e) {
                continue;
            }

            boolean updated = false;
            for (String key : properties.stringPropertyNames()) {
                String[] words = properties.getProperty(key, "").split("=");
                Set<String> updatedWords = new LinkedHashSet<>();

                for (String w : words) {
                    String cleanWord = w.trim();
                    if (!cleanWord.isEmpty()) {
                        if (sameKeyword(cleanWord, oldKeyword)) {
                            updatedWords.add(newKeyword);
                            updated = true;
                        } else {
                            updatedWords.add(cleanWord);
                        }
                    }
                }

                if (updated) {
                    properties.setProperty(key, String.join("=", updatedWords));
                }
            }

            if (updated) {
                try (FileOutputStream output = new FileOutputStream(filePath.toFile())) {
                    properties.store(output, "Synonyms " + type);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot update synonyms file: " + fileName, e);
                }
            }
        }
    }

    private void removeSynonymFromAllFiles(String keyword) {
        String[] synonymFiles = {
            SYNONYMS_NOUN_FILE,
            SYNONYMS_VERB_FILE,
            SYNONYMS_ADJECTIVE_FILE,
            SYNONYMS_ADVERB_FILE
        };

        for (String fileName : synonymFiles) {
            Path filePath = Paths.get(DATABASE_DIR, fileName);
            if (!Files.exists(filePath)) {
                continue;
            }

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(filePath.toFile())) {
                properties.load(input);
            } catch (IOException e) {
                continue;
            }

            boolean updated = false;
            for (String key : properties.stringPropertyNames()) {
                String[] words = properties.getProperty(key, "").split("=");
                Set<String> updatedWords = new LinkedHashSet<>();

                for (String w : words) {
                    String cleanWord = w.trim();
                    if (!cleanWord.isEmpty()) {
                        if (!sameKeyword(cleanWord, keyword)) {
                            updatedWords.add(cleanWord);
                        } else {
                            updated = true;
                        }
                    }
                }

                if (updated) {
                    if (updatedWords.isEmpty()) {
                        properties.remove(key);
                    } else {
                        properties.setProperty(key, String.join("=", updatedWords));
                    }
                }
            }

            if (updated) {
                try (FileOutputStream output = new FileOutputStream(filePath.toFile())) {
                    properties.store(output, "Synonyms");
                } catch (IOException e) {
                    throw new RuntimeException("Cannot update synonyms file: " + fileName, e);
                }
            }
        }
    }
}
