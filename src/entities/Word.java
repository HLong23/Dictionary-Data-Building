package entities;
/**
 * Word - Đối tượng từ trong từ điển với định nghĩa, phát âm, từ đồng nghĩa/trái nghĩa
 */
import java.util.LinkedList;

public class Word {

    private String keyword;
    private String pronunciation;
    private String pronounceAudioPath;
    private LinkedList<Definition> definitions;
    private LinkedList<String> synonyms;
    private LinkedList<String> antonyms;

    public Word(String keyword) {
        this.keyword = keyword;
        definitions = new LinkedList<>();
        synonyms = new LinkedList<>();
        antonyms = new LinkedList<>();
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
    }

    public void addSynonym(String synonym) {
        synonyms.add(synonym);
    }

    public void addAntonym(String antonym) {
        antonyms.add(antonym);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LinkedList<Definition> getDefinitions() {
        return definitions;
    }

    public LinkedList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(LinkedList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public LinkedList<String> getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(LinkedList<String> antonyms) {
        this.antonyms = antonyms;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }

    public String getPronounceAudioPath() {
        return pronounceAudioPath;
    }

    public void setPronounceAudioPath(String pronounceAudioPath) {
        this.pronounceAudioPath = pronounceAudioPath;
    }
}
