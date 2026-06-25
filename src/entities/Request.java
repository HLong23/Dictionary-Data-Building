package entities;

/**
 * Request - Đối tượng chứa thông tin yêu cầu từ người dùng
 */
import java.util.List;

public class Request {

    private String action;
    private String keyword;
    private List<String> params;

    public Request(String action, String keyword, List<String> params) {
        this.action = action;
        this.keyword = keyword;
        this.params = params;
    }

    public String getAction() {
        return action;
    }

    public String getKeyword() {
        return keyword;
    }

    public List<String> getParams() {
        return params;
    }
}
