package command;

/**
 * ExportCommand - Lệnh xuất toàn bộ từ điển ra file text
 */
import entities.Request;

public class ExportCommand extends Command {

    @Override
    public void execute(Request request) {
        service.export();
        System.out.println("Exported to: dictionary.txt");
    }
}
