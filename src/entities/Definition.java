package entities;
/**
 * Definition - Định nghĩa của từ (loại từ, nghĩa, ví dụ)
 */

public class Definition {

    private String type;
    private String meaning;
    private Sentence sentence;

    public Definition(String type, String meaning, Sentence sentence) {
        this.type = type;
        this.meaning = meaning;
        this.sentence = sentence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
    }
}
