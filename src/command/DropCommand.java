package command;

/**
 * DropCommand - Lệnh xóa từ khỏi từ điển
 */
import entities.Request;

public class DropCommand extends Command {

    @Override
    public void execute(Request request) {
        service.drop(request.getKeyword());
        System.out.println("Đã xóa: " + request.getKeyword());
    }
}
