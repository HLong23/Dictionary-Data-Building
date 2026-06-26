package entities;
/**
 * Word - Đối tượng từ trong từ điển với định nghĩa, phát âm, từ đồng nghĩa/trái nghĩa
 */
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

public class Word {

    private String keyword;
    private String pronunciation;
    private LinkedList<Definition> definitions;
    private Map<String, LinkedList<String>> synonyms;
    private Map<String, LinkedList<String>> antonyms;

    public Word(String keyword) {
        this.keyword = keyword;
        definitions = new LinkedList<>();
        synonyms = new HashMap<>();
        antonyms = new HashMap<>();
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
    }

    public void addSynonym(String wordType, String synonym) {
        synonyms.computeIfAbsent(wordType, k -> new LinkedList<>()).add(synonym);
    }

    public void addAntonym(String wordType, String antonym) {
        antonyms.computeIfAbsent(wordType, k -> new LinkedList<>()).add(antonym);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LinkedList<Definition> getDefinitions() {
        // Sort definitions alphabetically by type
        definitions.sort((d1, d2) -> {
            String type1 = d1.getType() == null ? "" : d1.getType().toLowerCase();
            String type2 = d2.getType() == null ? "" : d2.getType().toLowerCase();
            return type1.compareTo(type2);
        });
        return definitions;
    }

    public Map<String, LinkedList<String>> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(Map<String, LinkedList<String>> synonyms) {
        this.synonyms = synonyms;
    }

    public Map<String, LinkedList<String>> getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(Map<String, LinkedList<String>> antonyms) {
        this.antonyms = antonyms;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }
}
