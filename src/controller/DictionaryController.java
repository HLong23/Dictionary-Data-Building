package controller;

/**
 * DictionaryController - Điều phối các lệnh từ người dùng
 */
import command.*;
import entities.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DictionaryController {

    private Map<String, Command> commands;

    public DictionaryController(Scanner scanner) {
        commands = new HashMap<>();

        commands.put("lookup", new LookupCommand(scanner));

        commands.put("define", new DefineCommand(scanner));

        commands.put("drop", new DropCommand());

        commands.put("export", new ExportCommand());

        commands.put("help", new HelpCommand());

        commands.put("upload pronounce", new UploadPronounceCommand());

        commands.put("lookupshow", new LookupShowCommand(scanner));
    }

    public void handle(Request request) {
        Command command = commands.get(request.getAction());

        if(command != null) {command.execute(request);
        }
    }
}
