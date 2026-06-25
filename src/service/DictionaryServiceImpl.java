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
    private static final String SYNONYMS_FILE = "synonyms.def";
    private static final String ANTONYMS_FILE = "antonyms.def";
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

        return readWord(path, loadRelationProperties(SYNONYMS_FILE), loadRelationProperties(ANTONYMS_FILE));
    }

    @Override
    public List<Word> lookupSimilar(String keyword) {
        ensureDatabaseDir();

        String search = keyword.toLowerCase(Locale.ROOT);
        List<Word> results = new ArrayList<>();
        Properties synonyms = loadRelationProperties(SYNONYMS_FILE);
        Properties antonyms = loadRelationProperties(ANTONYMS_FILE);

        try (Stream<Path> paths = Files.list(Paths.get(DATABASE_DIR))) {
            paths
                    .filter(this::isWordFile)
                    .map(path -> readWord(path, synonyms, antonyms))
                    .filter(word -> wordMatches(word, search))
                    .sorted(Comparator.comparing(Word::getKeyword))
                    .forEach(results::add);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read database", e);
        }

        return results;
    }

    @Override
    public void define(Request request) {
        Word word = lookup(request.getKeyword());

        if (word == null) {
            word = new Word(request.getKeyword());
        }

        save(word);
    }

    @Override
    public void save(Word word) {
        ensureDatabaseDir();

        Properties properties = new Properties();
        properties.setProperty("word", valueOrEmpty(word.getKeyword()));
        properties.setProperty("pronounce", valueOrEmpty(word.getPronunciation()));
        properties.setProperty("pronounceAudio", valueOrEmpty(word.getPronounceAudioPath()));
        properties.setProperty("definition.count", String.valueOf(word.getDefinitions().size()));

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            Sentence sentence = definition.getSentence();
            String prefix = "definition." + i + ".";

            properties.setProperty(prefix + "type", valueOrEmpty(definition.getType()));
            properties.setProperty(prefix + "meaning", valueOrEmpty(definition.getMeaning()));
            properties.setProperty(prefix + "example", sentence == null ? "" : valueOrEmpty(sentence.getContent()));
            properties.setProperty(prefix + "exampleMeaning", sentence == null ? "" : valueOrEmpty(sentence.getMeaning()));
        }

        try (FileOutputStream output = new FileOutputStream(wordFile(word.getKeyword()).toFile())) {
            properties.store(output, "Dictionary word");
        } catch (IOException e) {
            throw new RuntimeException("Cannot save word: " + word.getKeyword(), e);
        }
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

        Word word = readWord(oldWordPath, loadRelationProperties(SYNONYMS_FILE), loadRelationProperties(ANTONYMS_FILE));
        word.setKeyword(newKeyword);
        renamePronounceAudio(word, newKeyword);
        save(word);

        if (!samePath(oldWordPath, newWordPath)) {
            try {
                Files.deleteIfExists(oldWordPath);
            } catch (IOException e) {
                throw new RuntimeException("Cannot remove old word file: " + oldKeyword, e);
            }
        }

        migrateRelationKeyword(SYNONYMS_FILE, oldKeyword, newKeyword);
        migrateRelationKeyword(ANTONYMS_FILE, oldKeyword, newKeyword);
    }

    @Override
    public void drop(String keyword) {
        Word word = lookup(keyword);

        try {
            Files.deleteIfExists(wordFile(keyword));
            removeRelationKeyword(SYNONYMS_FILE, keyword);
            removeRelationKeyword(ANTONYMS_FILE, keyword);
            deletePronounceAudio(word);
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
    public void uploadPronounce(String keyword, String sourcePath) {
        Word word = lookup(keyword);

        if (word == null) {
            throw new IllegalArgumentException("Word not found: " + keyword);
        }

        Path source = Paths.get(sourcePath);

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("File not found: " + sourcePath);
        }

        String fileName = sanitizeFileName(keyword) + getExtension(source.getFileName().toString());
        Path target = Paths.get(DATABASE_DIR, fileName);

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            word.setPronounceAudioPath(target.toString());
            save(word);
        } catch (IOException e) {
            throw new RuntimeException("Cannot upload pronunciation file", e);
        }
    }

    private List<Word> allWords() {
        ensureDatabaseDir();

        List<Word> words = new ArrayList<>();
        Properties synonyms = loadRelationProperties(SYNONYMS_FILE);
        Properties antonyms = loadRelationProperties(ANTONYMS_FILE);

        try (Stream<Path> paths = Files.list(Paths.get(DATABASE_DIR))) {
            paths
                    .filter(this::isWordFile)
                    .map(path -> readWord(path, synonyms, antonyms))
                    .sorted(Comparator.comparing(Word::getKeyword))
                    .forEach(words::add);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read database", e);
        }

        return words;
    }

    private Word readWord(Path path) {
        return readWord(path, loadRelationProperties(SYNONYMS_FILE), loadRelationProperties(ANTONYMS_FILE));
    }

    private Word readWord(Path path, Properties synonyms, Properties antonyms) {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(path.toFile())) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read word file: " + path, e);
        }

        Word word = new Word(properties.getProperty("word", ""));
        word.setPronunciation(properties.getProperty("pronounce", ""));
        word.setPronounceAudioPath(properties.getProperty("pronounceAudio", ""));
        readDefinitions(properties, word);
        word.setSynonyms(readRelation(synonyms, word.getKeyword()));
        word.setAntonyms(readRelation(antonyms, word.getKeyword()));

        return word;
    }

    private void writeWord(PrintWriter writer, Word word) {
        writer.println("Word: " + valueOrEmpty(word.getKeyword()));
        writer.println("Pronounce: " + valueOrEmpty(word.getPronunciation()));
        writeList(writer, "Synonyms", word.getSynonyms());
        writeList(writer, "Antonyms", word.getAntonyms());

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            Sentence sentence = definition.getSentence();

            writer.println("Definition " + (i + 1) + ":");
            writer.println("Class: " + valueOrEmpty(definition.getType()));
            writer.println("Meaning: " + valueOrEmpty(definition.getMeaning()));
            writer.println("Example: " + (sentence == null ? "" : valueOrEmpty(sentence.getContent())));
            writer.println("Example's meaning: " + (sentence == null ? "" : valueOrEmpty(sentence.getMeaning())));
        }
    }

    private void readDefinitions(Properties properties, Word word) {
        int count = parseInt(properties.getProperty("definition.count"), 0);

        for (int i = 0; i < count; i++) {
            String prefix = "definition." + i + ".";
            addDefinition(
                    word,
                    properties.getProperty(prefix + "type", ""),
                    properties.getProperty(prefix + "meaning", ""),
                    properties.getProperty(prefix + "example", ""),
                    properties.getProperty(prefix + "exampleMeaning", "")
            );
        }

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

    private LinkedList<String> readRelation(Properties properties, String keyword) {

        LinkedList<String> values = new LinkedList<>();
        String raw = properties.getProperty(sanitizeFileName(keyword), "");

        for (String value : raw.split(",")) {
            String clean = value.trim();

            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }

        return values;
    }

    private void migrateRelationKeyword(String fileName, String oldKeyword, String newKeyword) {
        Properties properties = loadRelationProperties(fileName);
        String oldKey = sanitizeFileName(oldKeyword);
        String newKey = sanitizeFileName(newKeyword);

        Set<String> newValues = parseValues(properties.getProperty(newKey, ""));
        newValues.addAll(parseValues(properties.getProperty(oldKey, "")));
        properties.remove(oldKey);

        if (!newValues.isEmpty()) {
            properties.setProperty(newKey, joinValues(newValues));
        }

        replaceRelationValue(properties, oldKeyword, newKeyword);
        storeRelationProperties(fileName, properties);
    }

    private void removeRelationKeyword(String fileName, String keyword) {
        Properties properties = loadRelationProperties(fileName);
        properties.remove(sanitizeFileName(keyword));
        removeRelationValue(properties, keyword);
        storeRelationProperties(fileName, properties);
    }

    private void replaceRelationValue(Properties properties, String oldKeyword, String newKeyword) {
        for (String key : properties.stringPropertyNames()) {
            Set<String> values = new LinkedHashSet<>();

            for (String value : parseValues(properties.getProperty(key, ""))) {
                if (sameKeyword(value, oldKeyword)) {
                    values.add(newKeyword);
                } else {
                    values.add(value);
                }
            }

            if (values.isEmpty()) {
                properties.remove(key);
            } else {
                properties.setProperty(key, joinValues(values));
            }
        }
    }

    private void removeRelationValue(Properties properties, String keyword) {
        for (String key : properties.stringPropertyNames()) {
            Set<String> values = new LinkedHashSet<>();

            for (String value : parseValues(properties.getProperty(key, ""))) {
                if (!sameKeyword(value, keyword)) {
                    values.add(value);
                }
            }

            if (values.isEmpty()) {
                properties.remove(key);
            } else {
                properties.setProperty(key, joinValues(values));
            }
        }
    }

    private Set<String> parseValues(String raw) {
        Set<String> values = new LinkedHashSet<>();

        for (String value : valueOrEmpty(raw).split(",")) {
            String clean = value.trim();

            if (!clean.isEmpty()) {
                values.add(clean);
            }
        }

        return values;
    }

    private String joinValues(Set<String> values) {
        return String.join(",", values);
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

    private boolean wordMatches(Word word, String search) {
        if (contains(word.getKeyword(), search)
                || listContains(word.getSynonyms(), search)
                || listContains(word.getAntonyms(), search)) {
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

    private boolean isWordFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".def")
                && !fileName.equals(SYNONYMS_FILE)
                && !fileName.equals(ANTONYMS_FILE);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void renamePronounceAudio(Word word, String newKeyword) {
        String audioPath = word.getPronounceAudioPath();

        if (isBlank(audioPath)) {
            return;
        }

        Path oldAudioPath = resolvePath(audioPath);

        if (!isInsideDatabase(oldAudioPath) || !Files.exists(oldAudioPath)) {
            return;
        }

        Path newAudioPath = Paths.get(DATABASE_DIR, sanitizeFileName(newKeyword)
                + getExtension(oldAudioPath.getFileName().toString()));

        if (samePath(oldAudioPath, newAudioPath)) {
            word.setPronounceAudioPath(newAudioPath.toString());
            return;
        }

        if (Files.exists(newAudioPath)) {
            throw new IllegalArgumentException("Pronunciation audio already exists: " + newAudioPath);
        }

        try {
            Files.move(oldAudioPath, newAudioPath);
            word.setPronounceAudioPath(newAudioPath.toString());
        } catch (IOException e) {
            throw new RuntimeException("Cannot rename pronunciation audio", e);
        }
    }

    private void deletePronounceAudio(Word word) throws IOException {
        if (word == null || isBlank(word.getPronounceAudioPath())) {
            return;
        }

        Path audioPath = resolvePath(word.getPronounceAudioPath());

        if (isInsideDatabase(audioPath)) {
            Files.deleteIfExists(audioPath);
        }
    }

    private boolean samePath(Path first, Path second) {
        return resolvePath(first.toString()).equals(resolvePath(second.toString()));
    }

    private boolean isInsideDatabase(Path path) {
        Path databasePath = Paths.get(DATABASE_DIR).toAbsolutePath().normalize();
        return path.toAbsolutePath().normalize().startsWith(databasePath);
    }

    private Path resolvePath(String path) {
        Path resolved = Paths.get(path);

        if (!resolved.isAbsolute()) {
            resolved = Paths.get("").toAbsolutePath().resolve(resolved);
        }

        return resolved.normalize();
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
            createFileIfMissing(Paths.get(DATABASE_DIR, SYNONYMS_FILE));
            createFileIfMissing(Paths.get(DATABASE_DIR, ANTONYMS_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create database folder", e);
        }
    }

    private void createFileIfMissing(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }
}
