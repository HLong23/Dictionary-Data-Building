package command;

/**
 * DefineCommand - Lệnh thêm/sửa từ với nhiều định nghĩa
 */
import entities.Definition;
import entities.Request;
import entities.Sentence;
import entities.Word;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DefineCommand extends Command {

    private final Scanner scanner;

    public DefineCommand(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void execute(Request request) {
        Word word = service.lookup(request.getKeyword());

        if (word == null) {
            word = new Word(request.getKeyword());
            fillNewWord(word);
            service.save(word);
            System.out.println("Đã thêm từ: " + word.getKeyword());
            return;
        }

        System.out.println("Từ đã tồn tại.");
        showWord(word);
        editWord(word);
    }

    private void fillNewWord(Word word) {
        System.out.println("Từ: " + word.getKeyword());
        word.setPronunciation(readOptional("Phiên âm: "));
        word.addDefinition(readDefinition());
        handlePronounceFile(word);
    }

    private void editWord(Word word) {
        while (true) {
            System.out.println("Chọn mục để sửa:");
            System.out.println("1. Từ");
            System.out.println("2. Phiên âm");
            System.out.println("3. File phát âm");
            System.out.println("4. Thêm định nghĩa");

            int offset = 4;
            for (int i = 0; i < word.getDefinitions().size(); i++) {
                System.out.println((offset + i + 1) + ". " + definitionLabel(word, i));
            }

            // Add synonym/antonym options for valid word types
            int synonymOption = offset + word.getDefinitions().size() + 1;
            int antonymOption = offset + word.getDefinitions().size() + 2;

            if (hasSynonymableType(word)) {
                System.out.println(synonymOption + ". Sửa từ đồng nghĩa");
            }
            if (hasAntonymableType(word)) {
                System.out.println(antonymOption + ". Sửa từ trái nghĩa");
            }

            System.out.println("0. Hủy");
            System.out.print("Lựa chọn: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                editKeyword(word);
                continue;
            }

            if (choice.equals("2")) {
                word.setPronunciation(readOptional("Phiên âm: "));
                service.save(word);
                System.out.println("Đã cập nhật: " + word.getKeyword());
                continue;
            }

            if (choice.equals("3")) {
                handlePronounceFile(word);
                service.save(word);
                System.out.println("Đã cập nhật: " + word.getKeyword());
                continue;
            }

            if (choice.equals("4")) {
                word.addDefinition(readDefinition());
                service.save(word);
                System.out.println("Đã cập nhật: " + word.getKeyword());
                continue;
            }

            if (choice.equals(String.valueOf(synonymOption)) && hasSynonymableType(word)) {
                editSynonyms(word);
                continue;
            }

            if (choice.equals(String.valueOf(antonymOption)) && hasAntonymableType(word)) {
                editAntonyms(word);
                continue;
            }

            if (choice.equals("0")) {
                System.out.println("Đã hủy.");
                return;
            }

            int definitionIndex;
            try {
                definitionIndex = Integer.parseInt(choice) - offset - 1;
            } catch (NumberFormatException e) {
                System.out.println("Lựa chọn không hợp lệ.");
                continue;
            }

            if (definitionIndex < 0 || definitionIndex >= word.getDefinitions().size()) {
                System.out.println("Lựa chọn không hợp lệ.");
                continue;
            }

            editDefinition(word, definitionIndex);
        }
    }

    private void editKeyword(Word word) {
        String oldKeyword = word.getKeyword();
        String newKeyword = readRequired("Từ: ");

        if (!oldKeyword.equalsIgnoreCase(newKeyword)) {
            service.rename(oldKeyword, newKeyword);
            word.setKeyword(newKeyword);
        } else {
            word.setKeyword(newKeyword);
            service.save(word);
        }

        System.out.println("Đã cập nhật: " + word.getKeyword());
    }

    private void editDefinition(Word word, int definitionIndex) {
        Definition definition = word.getDefinitions().get(definitionIndex);

        while (true) {
            System.out.println("Đang sửa " + definitionLabel(word, definitionIndex));
            System.out.println("1. Loại từ");
            System.out.println("2. Nghĩa");
            System.out.println("3. Ví dụ");
            System.out.println("4. Nghĩa ví dụ");
            System.out.println("5. Tất cả");
            System.out.println("6. Xóa định nghĩa");
            System.out.println("0. Hủy");
            System.out.print("Lựa chọn: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    definition.setType(readRequired("Loại từ: "));
                    service.save(word);
                    System.out.println("Đã cập nhật: " + word.getKeyword());
                    continue;
                case "2":
                    definition.setMeaning(readRequired("Nghĩa: "));
                    service.save(word);
                    System.out.println("Đã cập nhật: " + word.getKeyword());
                    continue;
                case "3":
                    sentence(definition).setContent(readOptional("Ví dụ: "));
                    service.save(word);
                    System.out.println("Đã cập nhật: " + word.getKeyword());
                    continue;
                case "4":
                    sentence(definition).setMeaning(readOptional("Nghĩa ví dụ: "));
                    service.save(word);
                    System.out.println("Đã cập nhật: " + word.getKeyword());
                    continue;
                case "5":
                    replaceDefinitionFields(definition);
                    service.save(word);
                    System.out.println("Đã cập nhật: " + word.getKeyword());
                    continue;
                case "6":
                    if (word.getDefinitions().size() == 1) {
                        System.out.println("Một từ phải có ít nhất một định nghĩa.");
                        continue;
                    }

                    word.getDefinitions().remove(definitionIndex);
                    service.save(word);
                    System.out.println("Đã xóa định nghĩa.");
                    return;
                case "0":
                    System.out.println("Đã hủy.");
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ.");
                    continue;
            }
        }
    }

    private Definition readDefinition() {
        String type = readValidClass();
        String meaning = readRequired("Nghĩa: ");
        String example = readOptional("Ví dụ: ");
        String exampleMeaning = readOptional("Nghĩa ví dụ: ");

        return new Definition(type, meaning, new Sentence(example, exampleMeaning));
    }

    private void replaceDefinitionFields(Definition definition) {
        definition.setType(readValidClass());
        definition.setMeaning(readRequired("Nghĩa: "));
        Sentence sentence = sentence(definition);
        sentence.setContent(readOptional("Ví dụ: "));
        sentence.setMeaning(readOptional("Nghĩa ví dụ: "));
    }

    private Sentence sentence(Definition definition) {
        if (definition.getSentence() == null) {
            definition.setSentence(new Sentence("", ""));
        }

        return definition.getSentence();
    }

    private String definitionLabel(Word word, int index) {
        Definition definition = word.getDefinitions().get(index);
        String type = valueOrEmpty(definition.getType());

        if (type.isEmpty()) {
            type = "definition";
        }

        int number = definitionNumber(word, index, type);
        String meaning = valueOrEmpty(definition.getMeaning());
        String label = type + " " + number;

        if (meaning.isEmpty()) {
            return label;
        }

        return label + " - " + meaning;
    }

    private int definitionNumber(Word word, int targetIndex, String targetType) {
        Map<String, Integer> counts = new HashMap<>();

        for (int i = 0; i <= targetIndex; i++) {
            String type = valueOrEmpty(word.getDefinitions().get(i).getType());

            if (type.isEmpty()) {
                type = "definition";
            }

            String key = type.toLowerCase();
            int next = counts.getOrDefault(key, 0) + 1;
            counts.put(key, next);

            if (i == targetIndex) {
                return next;
            }
        }

        return 1;
    }

    private String readRequired(String label) {
        while (true) {
            String value = readOptional(label);

            if (!value.isBlank()) {
                return value;
            }

            System.out.println("Trường này là bắt buộc.");
        }
    }

    private String readOptional(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private void showWord(Word word) {
        System.out.println("Từ: " + valueOrEmpty(word.getKeyword()));
        System.out.println("Phiên âm: " + valueOrEmpty(word.getPronunciation()));

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

            System.out.println("----------------------------");
            System.out.println("Loại từ: " + type);

            for (Definition definition : definitions) {
                java.util.LinkedList<Sentence> examples = definition.getExamples();

                System.out.println("  Định nghĩa " + defCount + ":");
                System.out.println("    Nghĩa: " + valueOrEmpty(definition.getMeaning()));

                if (examples != null && !examples.isEmpty()) {
                    for (int j = 0; j < examples.size(); j++) {
                        Sentence sentence = examples.get(j);
                        System.out.println("    Ví dụ " + (j + 1) + ": " + valueOrEmpty(sentence.getContent()));
                        System.out.println("      Nghĩa ví dụ: " + valueOrEmpty(sentence.getMeaning()));
                    }
                }
                defCount++;
            }
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void handlePronounceFile(Word word) {
        System.out.print("Đường dẫn file phát âm (hoặc Enter để bỏ qua): ");
        String filePath = scanner.nextLine().trim();

        if (!filePath.isBlank()) {
            try {
                service.copyPronounceFile(word.getKeyword(), filePath);
                System.out.println("Đã sao chép file phát âm thành công.");
            } catch (Exception e) {
                System.out.println("Lỗi sao chép file phát âm: " + e.getMessage());
            }
        }
    }

    private String readValidClass() {
        String[] validClasses = {"Noun", "Pronoun", "Verb", "Adjective", "Adverb", "Preposition", "Conjunction", "Interjection"};

        while (true) {
            System.out.println("Chọn Loại từ:");
            for (int i = 0; i < validClasses.length; i++) {
                System.out.println((i + 1) + ". " + validClasses[i]);
            }
            System.out.print("Nhập số (1-8): ");

            String choice = scanner.nextLine().trim();
            try {
                int index = Integer.parseInt(choice) - 1;
                if (index >= 0 && index < validClasses.length) {
                    return validClasses[index];
                }
            } catch (NumberFormatException e) {
                // Invalid input, loop again
            }
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private boolean hasSynonymableType(Word word) {
        for (Definition definition : word.getDefinitions()) {
            String type = definition.getType();
            if (type != null && (type.toLowerCase().startsWith("noun") ||
                type.toLowerCase().startsWith("verb") ||
                type.toLowerCase().startsWith("adjective") ||
                type.toLowerCase().startsWith("adverb"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAntonymableType(Word word) {
        return hasSynonymableType(word); // Same types for antonyms
    }

    private void editSynonyms(Word word) {
        System.out.println("Sửa từ đồng nghĩa cho: " + word.getKeyword());

        // Get available word types for this word
        java.util.Set<String> availableTypes = new java.util.LinkedHashSet<>();
        for (Definition definition : word.getDefinitions()) {
            String type = definition.getType();
            if (type != null && isSynonymableType(type)) {
                String normalizedType = normalizeWordType(type);
                if (normalizedType != null) {
                    availableTypes.add(normalizedType);
                }
            }
        }

        if (availableTypes.isEmpty()) {
            System.out.println("Từ này không có loại từ hỗ trợ từ đồng nghĩa (chỉ hỗ trợ noun, verb, adjective, adverb).");
            return;
        }

        // Show current synonyms by available types
        for (String normalizedType : availableTypes) {
            System.out.println("Loại từ: " + normalizedType);
            java.util.Map<String, java.util.LinkedList<String>> synonyms = word.getSynonyms();
            if (synonyms != null && synonyms.containsKey(normalizedType)) {
                java.util.LinkedList<String> typeSynonyms = synonyms.get(normalizedType);
                System.out.println("  Từ đồng nghĩa hiện tại: " + String.join(", ", typeSynonyms));
            } else {
                System.out.println("  Chưa có từ đồng nghĩa");
            }
        }

        System.out.println("1. Thêm từ đồng nghĩa");
        System.out.println("2. Xóa từ đồng nghĩa");
        System.out.println("0. Quay lại");
        System.out.print("Lựa chọn: ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            addSynonym(word, availableTypes);
        } else if (choice.equals("2")) {
            removeSynonym(word, availableTypes);
        }
    }

    private void addSynonym(Word word, java.util.Set<String> availableTypes) {
        System.out.println("Các loại từ có sẵn cho từ này:");
        int index = 1;
        for (String type : availableTypes) {
            System.out.println(index + ". " + type);
            index++;
        }
        System.out.print("Chọn loại từ: ");

        String choice = scanner.nextLine().trim();
        try {
            int typeIndex = Integer.parseInt(choice) - 1;
            if (typeIndex < 0 || typeIndex >= availableTypes.size()) {
                System.out.println("Lựa chọn không hợp lệ.");
                return;
            }

            String[] typesArray = availableTypes.toArray(new String[0]);
            String wordType = typesArray[typeIndex];

            System.out.print("Nhập từ đồng nghĩa (chỉ cần 1 từ trong họ): ");
            String synonymWord = scanner.nextLine().trim();

            if (synonymWord.isBlank()) {
                System.out.println("Không được để trống từ đồng nghĩa.");
                return;
            }

            try {
                service.addSynonym(word.getKeyword(), wordType, synonymWord);
                System.out.println("Đã thêm từ đồng nghĩa thành công.");
            } catch (Exception e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void removeSynonym(Word word, java.util.Set<String> availableTypes) {
        System.out.println("Các loại từ có sẵn cho từ này:");
        int index = 1;
        for (String type : availableTypes) {
            System.out.println(index + ". " + type);
            index++;
        }
        System.out.print("Chọn loại từ để xóa: ");

        String choice = scanner.nextLine().trim();
        try {
            int typeIndex = Integer.parseInt(choice) - 1;
            if (typeIndex < 0 || typeIndex >= availableTypes.size()) {
                System.out.println("Lựa chọn không hợp lệ.");
                return;
            }

            String[] typesArray = availableTypes.toArray(new String[0]);
            String wordType = typesArray[typeIndex];

            try {
                service.removeSynonym(word.getKeyword(), wordType);
                System.out.println("Đã xóa từ đồng nghĩa thành công.");
            } catch (Exception e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void editAntonyms(Word word) {
        System.out.println("Sửa từ trái nghĩa cho: " + word.getKeyword());

        // Get available word types for this word
        java.util.Set<String> availableTypes = new java.util.LinkedHashSet<>();
        for (Definition definition : word.getDefinitions()) {
            String type = definition.getType();
            if (type != null && isSynonymableType(type)) {
                String normalizedType = normalizeWordType(type);
                if (normalizedType != null) {
                    availableTypes.add(normalizedType);
                }
            }
        }

        if (availableTypes.isEmpty()) {
            System.out.println("Từ này không có loại từ hỗ trợ từ trái nghĩa (chỉ hỗ trợ noun, verb, adjective, adverb).");
            return;
        }

        // Show current antonyms by available types
        java.util.Map<String, java.util.LinkedList<String>> antonyms = word.getAntonyms();
        if (antonyms != null && !antonyms.isEmpty()) {
            for (String normalizedType : availableTypes) {
                if (antonyms.containsKey(normalizedType)) {
                    System.out.println("Loại từ: " + normalizedType);
                    System.out.println("  Từ trái nghĩa hiện tại: " + String.join(", ", antonyms.get(normalizedType)));
                }
            }
        } else {
            System.out.println("Chưa có từ trái nghĩa");
        }

        System.out.println("1. Thêm từ trái nghĩa");
        System.out.println("2. Xóa từ trái nghĩa");
        System.out.println("0. Quay lại");
        System.out.print("Lựa chọn: ");

        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            addAntonym(word, availableTypes);
        } else if (choice.equals("2")) {
            removeAntonym(word, availableTypes);
        }
    }

    private void addAntonym(Word word, java.util.Set<String> availableTypes) {
        System.out.println("Các loại từ có sẵn cho từ này:");
        int index = 1;
        for (String type : availableTypes) {
            System.out.println(index + ". " + type);
            index++;
        }
        System.out.print("Chọn loại từ: ");

        String choice = scanner.nextLine().trim();
        try {
            int typeIndex = Integer.parseInt(choice) - 1;
            if (typeIndex < 0 || typeIndex >= availableTypes.size()) {
                System.out.println("Lựa chọn không hợp lệ.");
                return;
            }

            String[] typesArray = availableTypes.toArray(new String[0]);
            String antonymType = typesArray[typeIndex];

            System.out.print("Nhập từ trái nghĩa: ");
            String antonymWord = scanner.nextLine().trim();

            if (antonymWord.isBlank()) {
                System.out.println("Không được để trống từ trái nghĩa.");
                return;
            }

            try {
                service.addAntonym(word.getKeyword(), antonymWord, antonymType);
                System.out.println("Đã thêm từ trái nghĩa thành công.");
            } catch (Exception e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void removeAntonym(Word word, java.util.Set<String> availableTypes) {
        System.out.println("Các loại từ có sẵn cho từ này:");
        int index = 1;
        for (String type : availableTypes) {
            System.out.println(index + ". " + type);
            index++;
        }
        System.out.print("Chọn loại từ để xóa: ");

        String choice = scanner.nextLine().trim();
        try {
            int typeIndex = Integer.parseInt(choice) - 1;
            if (typeIndex < 0 || typeIndex >= availableTypes.size()) {
                System.out.println("Lựa chọn không hợp lệ.");
                return;
            }

            String[] typesArray = availableTypes.toArray(new String[0]);
            String antonymType = typesArray[typeIndex];

            try {
                service.removeAntonym(word.getKeyword(), antonymType);
                System.out.println("Đã xóa từ trái nghĩa thành công.");
            } catch (Exception e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private boolean isSynonymableType(String type) {
        if (type == null) return false;
        String lower = type.toLowerCase();
        return lower.startsWith("noun") || lower.startsWith("verb") ||
               lower.startsWith("adjective") || lower.startsWith("adverb");
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
}
