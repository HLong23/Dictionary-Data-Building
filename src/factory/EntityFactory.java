package factory;

/**
 * EntityFactory - Factory pattern để tạo các entity
 */
import entities.*;

public class EntityFactory {

    public static Word createWord(String keyword) {
        return new Word(keyword);
    }

    public static Definition createDefinition(String type, String meaning, Sentence sentence) {
        return new Definition(type, meaning, sentence);
    }

    public static Sentence createSentence(String content, String meaning) {
        return new Sentence(content, meaning);
    }
}
