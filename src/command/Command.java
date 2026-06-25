package command;

/**
 * Command - Lớp trừu tượng cơ sở cho tất cả các lệnh
 */
import entities.Request;
import service.DictionaryService;
import service.DictionaryServiceImpl;
import java.util.Scanner;

public abstract class Command {

    protected final DictionaryService service;
    protected final Scanner scanner;

    protected Command() {
        this(null);
    }

    protected Command(Scanner scanner) {
        this.service = DictionaryServiceImpl.getInstance();
        this.scanner = scanner;
    }

    public abstract void execute(Request request);
}
