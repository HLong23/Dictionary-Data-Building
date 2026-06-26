package entities;
/**
 * Definition - Định nghĩa của từ (loại từ, nghĩa, danh sách ví dụ)
 */
import java.util.LinkedList;

public class Definition {

    private String type;
    private String meaning;
    private LinkedList<Sentence> examples;

    public Definition(String type, String meaning, LinkedList<Sentence> examples) {
        this.type = type;
        this.meaning = meaning;
        this.examples = examples;
    }

    public Definition(String type, String meaning, Sentence sentence) {
        this.type = type;
        this.meaning = meaning;
        this.examples = new LinkedList<>();
        if (sentence != null) {
            this.examples.add(sentence);
        }
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

    public LinkedList<Sentence> getExamples() {
        return examples;
    }

    public void setExamples(LinkedList<Sentence> examples) {
        this.examples = examples;
    }

    public void addExample(Sentence example) {
        if (this.examples == null) {
            this.examples = new LinkedList<>();
        }
        this.examples.add(example);
    }

    // Deprecated method for backward compatibility
    public Sentence getSentence() {
        return (examples != null && !examples.isEmpty()) ? examples.getFirst() : null;
    }

    // Deprecated method for backward compatibility
    public void setSentence(Sentence sentence) {
        this.examples = new LinkedList<>();
        if (sentence != null) {
            this.examples.add(sentence);
        }
    }
}
