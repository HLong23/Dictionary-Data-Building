package service;

/**
 * DictionaryService - Interface định nghĩa các thao tác với từ điển
 */
import entities.*;

public interface DictionaryService {

    Word lookup(String keyword);

    java.util.List<Word> lookupSimilar(String keyword);

    void save(Word word);

    void rename(String oldKeyword, String newKeyword);

    void drop(String keyword);

    void export();

    void copyPronounceFile(String keyword, String sourcePath);

    void export(String path);
}
