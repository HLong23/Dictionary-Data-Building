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
            System.out.println("No matching words: " + request.getKeyword());
            return;
        }

        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            System.out.println((i + 1) + ". " + word.getKeyword() + " - " + summary(word, search));
        }

        System.out.print("Choose word number (0 to cancel): ");
        String input = scanner.nextLine().trim();

        int choice;
        try {
            choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        if (choice == 0) {
            System.out.println("Cancelled.");
            return;
        }

        if (choice < 1 || choice > words.size()) {
            System.out.println("Invalid choice.");
            return;
        }

        printWord(words.get(choice - 1));
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

        if (listContains(word.getSynonyms(), search)) {
            return "Synonyms: " + String.join(", ", word.getSynonyms());
        }

        if (listContains(word.getAntonyms(), search)) {
            return "Antonyms: " + String.join(", ", word.getAntonyms());
        }

        return definitionSummary(word.getDefinitions().getFirst());
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
