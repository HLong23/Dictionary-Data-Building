/**
 * Main - Điểm nhập chính của ứng dụng từ điển
 * Xử lý input từ người dùng và điều phối các lệnh
 */
import controller.DictionaryController;
import entities.Request;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        DictionaryController controller = new DictionaryController(scanner);

        while (true) {

            System.out.print("Action: ");

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Bye!");
                break;
            }

            try {
                Request request = parseRequest(input);
                controller.handle(request);
            } catch (Exception e) {
                System.out.println("Invalid command!");
                System.out.println(e.getMessage());
            }

            System.out.println();
        }
    }

    private static Request parseRequest(String input) {

        String[] parts = input.split("\\s+");

        String action = parts[0].toLowerCase();

        switch (action) {

            case "help": {

                return new Request("help", "", new ArrayList<>());
            }

            case "lookup": {

                if (parts.length < 2) {
                    throw new IllegalArgumentException("Usage: lookup <word>");
                }

                return new Request("lookup", parts[1], new ArrayList<>());
            }

            case "lookupshow": {

                if (parts.length < 2) {
                    throw new IllegalArgumentException("Usage: lookupshow <word>");
                }

                return new Request("lookupshow", parts[1], new ArrayList<>());
            }

            case "drop": {

                if (parts.length < 2) {
                    throw new IllegalArgumentException("Usage: drop <word>");
                }

                return new Request("drop", parts[1], new ArrayList<>());
            }

            case "export": {

                if (parts.length > 1) {
                    throw new IllegalArgumentException("Usage: export");
                }

                return new Request("export", "", new ArrayList<>());
            }

            case "define": {

                if (parts.length < 2) {
                    throw new IllegalArgumentException("Usage: define <word>");
                }

                return new Request("define", parts[1], new ArrayList<>());
            }

            default:
                throw new IllegalArgumentException("Unknown action");
        }
    }

}
