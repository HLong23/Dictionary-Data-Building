package service;

/**
 * DictionaryService - Interface định nghĩa các thao tác với từ điển
 */
import entities.*;

public interface DictionaryService {

    Word lookup(String keyword);

    java.util.List<Word> lookupSimilar(String keyword);

    void define(Request request);

    void save(Word word);

    void rename(String oldKeyword, String newKeyword);

    void drop(String keyword);

    void export();

    void uploadPronounce(String keyword, String sourcePath);

    void export(String path);
}
