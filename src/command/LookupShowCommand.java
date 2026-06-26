package command;

/**
 * LookupShowCommand - Lệnh tìm kiếm từ tương tự và hiển thị danh sách để chọn
 */
import entities.Request;
import entities.Definition;
import entities.Sentence;
import entities.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class LookupShowCommand extends Command {

    private final Scanner scanner;

    public LookupShowCommand(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void execute(Request request) {
        List<Word> words = service.lookupSimilar(request.getKeyword());
        String search = request.getKeyword().toLowerCase(Locale.ROOT);

        if (words.isEmpty()) {
            System.out.println("Không tìm thấy từ phù hợp: " + request.getKeyword());
            return;
        }

        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            System.out.println((i + 1) + ". " + word.getKeyword() + " - " + summary(word, search));
        }

        System.out.print("Chọn số thứ tự từ (0 để hủy): ");
        String input = scanner.nextLine().trim();

        int choice;
        try {
            choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Lựa chọn không hợp lệ.");
            return;
        }

        if (choice == 0) {
            System.out.println("Đã hủy.");
            return;
        }

        if (choice < 1 || choice > words.size()) {
            System.out.println("Lựa chọn không hợp lệ.");
            return;
        }

        // Call lookup command directly for the selected word
        LookupCommand lookupCommand = new LookupCommand(scanner);
        lookupCommand.execute(new Request("lookup", words.get(choice - 1).getKeyword(), new ArrayList<>()));
    }

    private void printWord(Word word) {
        System.out.println("Word: " + valueOrEmpty(word.getKeyword()));
        printIfNotBlank("Pronounce", word.getPronunciation());
        printRelationMap("Synonyms", word.getSynonyms());
        printRelationMap("Antonyms", word.getAntonyms());

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            Sentence sentence = definition.getSentence();

            System.out.println("Definition " + (i + 1) + ":");
            printIfNotBlank("Class", definition.getType());
            printIfNotBlank("Meaning", definition.getMeaning());

            if (sentence != null) {
                printIfNotBlank("Example", sentence.getContent());
                printIfNotBlank("Example's meaning", sentence.getMeaning());
            }
        }
    }

    private String summary(Word word, String search) {
        if (word.getDefinitions().isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        boolean keywordMatches = contains(word.getKeyword(), search);

        for (Definition definition : word.getDefinitions()) {
            Sentence sentence = definition.getSentence();

            if (keywordMatches
                    || contains(definition.getType(), search)
                    || contains(definition.getMeaning(), search)
                    || (sentence != null && (contains(sentence.getContent(), search)
                    || contains(sentence.getMeaning(), search)))) {
                parts.add(definitionSummary(definition));
            }
        }

        if (!parts.isEmpty()) {
            return String.join("; ", parts);
        }

        if (mapContains(word.getSynonyms(), search)) {
            return "Synonyms: " + flattenMapValues(word.getSynonyms());
        }

        if (mapContains(word.getAntonyms(), search)) {
            return "Antonyms: " + flattenMapValues(word.getAntonyms());
        }

        return definitionSummary(word.getDefinitions().get(0));
    }

    private String definitionSummary(Definition definition) {
        String type = valueOrEmpty(definition.getType());
        String meaning = valueOrEmpty(definition.getMeaning());

        if (type.isEmpty()) {
            return meaning;
        }

        if (meaning.isEmpty()) {
            return type;
        }

        return type + ": " + meaning;
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

    private boolean mapContains(java.util.Map<String, java.util.LinkedList<String>> map, String search) {
        if (map == null) {
            return false;
        }

        for (java.util.LinkedList<String> values : map.values()) {
            if (listContains(values, search)) {
                return true;
            }
        }

        return false;
    }

    private String flattenMapValues(java.util.Map<String, java.util.LinkedList<String>> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        java.util.List<String> allValues = new java.util.ArrayList<>();
        for (java.util.LinkedList<String> values : map.values()) {
            allValues.addAll(values);
        }

        return String.join(", ", allValues);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private void printIfNotBlank(String label, String value) {
        if (value != null && !value.isBlank()) {
            System.out.println(label + ": " + value);
        }
    }

    private void printRelationMap(String label, java.util.Map<String, java.util.LinkedList<String>> relationMap) {
        if (relationMap != null && !relationMap.isEmpty()) {
            for (java.util.Map.Entry<String, java.util.LinkedList<String>> entry : relationMap.entrySet()) {
                String wordType = entry.getKey();
                java.util.LinkedList<String> values = entry.getValue();
                if (!values.isEmpty()) {
                    System.out.println(label + " (" + wordType + "): " + String.join(", ", values));
                }
            }
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
