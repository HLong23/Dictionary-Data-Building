package command;

/**
 * HelpCommand - Lệnh hiển thị hướng dẫn sử dụng các lệnh
 */
import entities.Request;

public class HelpCommand extends Command {

    @Override
    public void execute(Request request) {
        System.out.println("Available commands:");
        System.out.println("  help");
        System.out.println("    Show all commands and examples.");
        System.out.println("    Example: help");
        System.out.println();
        System.out.println("  lookup <word>");
        System.out.println("    Find a word in the dictionary.");
        System.out.println("    Example: lookup apple");
        System.out.println();
        System.out.println("  lookupshow <keyword>");
        System.out.println("    Show matching words, then choose one to view.");
        System.out.println("    Example: lookupshow app");
        System.out.println();
        System.out.println("  define <word>");
        System.out.println("    Add or edit one word with multiple definitions.");
        System.out.println("    Example: define apple");
        System.out.println("    Existing words let you choose noun 1, verb 1, ... before editing a field.");
        System.out.println();
        System.out.println("  upload pronounce <word> <file-path>");
        System.out.println("    Attach a pronunciation audio file to a word.");
        System.out.println("    Example: upload pronounce apple C:\\Users\\longp\\Desktop\\apple.mp3");
        System.out.println();
        System.out.println("  drop <word>");
        System.out.println("    Remove a word from the dictionary.");
        System.out.println("    Example: drop apple");
        System.out.println();
        System.out.println("  export");
        System.out.println("    Export all words to dictionary.txt.");
        System.out.println("    Example: export");
        System.out.println();
        System.out.println("Shared relation files:");
        System.out.println("  database\\synonyms.def");
        System.out.println("  database\\antonyms.def");
        System.out.println("    Format: word=value 1,value 2");
        System.out.println();
        System.out.println("  exit");
        System.out.println("    Close the program.");
        System.out.println("    Example: exit");
    }
}
