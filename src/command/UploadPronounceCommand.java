package command;

/**
 * UploadPronounceCommand - Lệnh tải file âm thanh phát âm cho từ
 */
import entities.Request;

public class UploadPronounceCommand extends Command {

    @Override
    public void execute(Request request) {
        if (request.getParams().isEmpty()) {
            throw new IllegalArgumentException("Usage: upload pronounce <word> <file-path>");
        }

        service.uploadPronounce(request.getKeyword(), request.getParams().get(0));
        System.out.println("Uploaded pronunciation for: " + request.getKeyword());
    }
}
