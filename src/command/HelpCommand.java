package command;

/**
 * HelpCommand - Lệnh hiển thị hướng dẫn sử dụng các lệnh
 */
import entities.Request;

public class HelpCommand extends Command {

    @Override
    public void execute(Request request) {
        System.out.println("Các lệnh có sẵn:");
        System.out.println("  help");
        System.out.println("    Hiển thị tất cả các lệnh và ví dụ.");
        System.out.println("    Ví dụ: help");
        System.out.println();
        System.out.println("  lookup <từ>");
        System.out.println("    Tìm từ trong từ điển.");
        System.out.println("    Ví dụ: lookup apple");
        System.out.println();
        System.out.println("  lookupshow <từ khóa>");
        System.out.println("    Hiển thị các từ phù hợp, sau đó chọn một để xem.");
        System.out.println("    Ví dụ: lookupshow app");
        System.out.println();
        System.out.println("  define <từ>");
        System.out.println("    Thêm hoặc sửa từ với nhiều định nghĩa.");
        System.out.println("    Ví dụ: define apple");
        System.out.println("    Từ đã có cho phép chọn danh từ 1, động từ 1, ... trước khi sửa trường.");
        System.out.println();
        System.out.println("  drop <từ>");
        System.out.println("    Xóa từ khỏi từ điển.");
        System.out.println("    Ví dụ: drop apple");
        System.out.println();
        System.out.println("  export");
        System.out.println("    Xuất tất cả từ ra dictionary.txt.");
        System.out.println("    Ví dụ: export");
        System.out.println();
        System.out.println("File quan hệ chia sẻ:");
        System.out.println("  database\\synonymsNoun.def");
        System.out.println("  database\\synonymsVerb.def");
        System.out.println("  database\\synonymsAdjective.def");
        System.out.println("  database\\synonymsAdverb.def");
        System.out.println("    Định dạng: group1=từ1=từ2=...");
        System.out.println();
        System.out.println("  exit");
        System.out.println("    Đóng chương trình.");
        System.out.println("    Ví dụ: exit");
    }
}
