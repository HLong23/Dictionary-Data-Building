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
            System.out.println("Defined: " + word.getKeyword());
            return;
        }

        System.out.println("Word already exists.");
        showWord(word);
        editWord(word);
    }

    private void fillNewWord(Word word) {
        System.out.println("Word: " + word.getKeyword());
        word.setPronunciation(readOptional("Pronounce: "));
        word.addDefinition(readDefinition());
    }

    private void editWord(Word word) {
        while (true) {
            System.out.println("Choose item to edit:");
            System.out.println("W. Word");
            System.out.println("P. Pronounce");
            System.out.println("A. Add definition");

            for (int i = 0; i < word.getDefinitions().size(); i++) {
                System.out.println((i + 1) + ". " + definitionLabel(word, i));
            }

            System.out.println("0. Cancel");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("w")) {
                editKeyword(word);
                return;
            }

            if (choice.equalsIgnoreCase("p")) {
                word.setPronunciation(readOptional("Pronounce: "));
                service.save(word);
                System.out.println("Updated: " + word.getKeyword());
                return;
            }

            if (choice.equalsIgnoreCase("a")) {
                word.addDefinition(readDefinition());
                service.save(word);
                System.out.println("Updated: " + word.getKeyword());
                return;
            }

            if (choice.equals("0")) {
                System.out.println("Cancelled.");
                return;
            }

            int definitionIndex;
            try {
                definitionIndex = Integer.parseInt(choice) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
                continue;
            }

            if (definitionIndex < 0 || definitionIndex >= word.getDefinitions().size()) {
                System.out.println("Invalid choice.");
                continue;
            }

            editDefinition(word, definitionIndex);
            return;
        }
    }

    private void editKeyword(Word word) {
        String oldKeyword = word.getKeyword();
        String newKeyword = readRequired("Word: ");

        if (!oldKeyword.equalsIgnoreCase(newKeyword)) {
            service.rename(oldKeyword, newKeyword);
            word.setKeyword(newKeyword);
        } else {
            word.setKeyword(newKeyword);
            service.save(word);
        }

        System.out.println("Updated: " + word.getKeyword());
    }

    private void editDefinition(Word word, int definitionIndex) {
        Definition definition = word.getDefinitions().get(definitionIndex);

        while (true) {
            System.out.println("Editing " + definitionLabel(word, definitionIndex));
            System.out.println("1. Class");
            System.out.println("2. Meaning");
            System.out.println("3. Example");
            System.out.println("4. Example's meaning");
            System.out.println("5. All");
            System.out.println("6. Delete definition");
            System.out.println("0. Cancel");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    definition.setType(readRequired("Class: "));
                    break;
                case "2":
                    definition.setMeaning(readRequired("Meaning: "));
                    break;
                case "3":
                    sentence(definition).setContent(readOptional("Example: "));
                    break;
                case "4":
                    sentence(definition).setMeaning(readOptional("Example's meaning: "));
                    break;
                case "5":
                    replaceDefinitionFields(definition);
                    break;
                case "6":
                    if (word.getDefinitions().size() == 1) {
                        System.out.println("A word must have at least one definition.");
                        continue;
                    }

                    word.getDefinitions().remove(definitionIndex);
                    break;
                case "0":
                    System.out.println("Cancelled.");
                    return;
                default:
                    System.out.println("Invalid choice.");
                    continue;
            }

            service.save(word);
            System.out.println("Updated: " + word.getKeyword());
            return;
        }
    }

    private Definition readDefinition() {
        String type = readRequired("Class: ");
        String meaning = readRequired("Meaning: ");
        String example = readOptional("Example: ");
        String exampleMeaning = readOptional("Example's meaning: ");

        return new Definition(type, meaning, new Sentence(example, exampleMeaning));
    }

    private void replaceDefinitionFields(Definition definition) {
        definition.setType(readRequired("Class: "));
        definition.setMeaning(readRequired("Meaning: "));
        Sentence sentence = sentence(definition);
        sentence.setContent(readOptional("Example: "));
        sentence.setMeaning(readOptional("Example's meaning: "));
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

            System.out.println("This field is required.");
        }
    }

    private String readOptional(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private void showWord(Word word) {
        System.out.println("Word: " + valueOrEmpty(word.getKeyword()));
        System.out.println("Pronounce: " + valueOrEmpty(word.getPronunciation()));

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            Sentence sentence = definition.getSentence();

            System.out.println(definitionLabel(word, i));
            System.out.println("  Class: " + valueOrEmpty(definition.getType()));
            System.out.println("  Meaning: " + valueOrEmpty(definition.getMeaning()));
            System.out.println("  Example: " + (sentence == null ? "" : valueOrEmpty(sentence.getContent())));
            System.out.println("  Example's meaning: " + (sentence == null ? "" : valueOrEmpty(sentence.getMeaning())));
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
