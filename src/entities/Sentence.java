package entities;
/**
 * Sentence - Câu ví dụ và nghĩa của câu
 */
public class Sentence {

    private String content;
    private String meaning;

    public Sentence(String content, String meaning) {
        this.content = content;
        this.meaning = meaning;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }
}
