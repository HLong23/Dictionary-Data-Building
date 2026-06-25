package command;
/**
 * LookupCommand - Lệnh tra cứu từ chính xác trong từ điển
 */
import entities.Request;
import entities.Definition;
import entities.Sentence;
import entities.Word;
import java.util.Scanner;
import java.io.File;

public class LookupCommand extends Command {

    public LookupCommand() {
        super();
    }

    public LookupCommand(Scanner scanner) {
        super(scanner);
    }

    @Override
    public void execute(Request request) {
        Word word = service.lookup(request.getKeyword());

        if (word == null) {
            System.out.println("Word not found: " + request.getKeyword());
            return;
        }

        System.out.println("Word: " + word.getKeyword());

        printIfNotBlank("Pronounce", word.getPronunciation());
        printList("Synonyms", word.getSynonyms());
        printList("Antonyms", word.getAntonyms());
        printIfNotBlank("Pronounce audio", word.getPronounceAudioPath());

        for (int i = 0; i < word.getDefinitions().size(); i++) {
            Definition definition = word.getDefinitions().get(i);
            Sentence sentence = definition.getSentence();

            System.out.println("----------------------------");
            System.out.println("Definition " + (i + 1) + ":");
            printIfNotBlank("Class", definition.getType());
            printIfNotBlank("Meaning", definition.getMeaning());

            if (sentence != null) {
                printIfNotBlank("Example", sentence.getContent());
                printIfNotBlank("Example's meaning", sentence.getMeaning());
            }
        }

        if (scanner != null) {
            String audioPath = "database/" + word.getKeyword() + ".mp3";
            System.out.print("\nPress 1 to pronounce or any other key to exit: ");
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("1")) {
                playAudioLoop(audioPath);
            }
        }
    }

    private void playAudioLoop(String audioPath) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            System.out.println("Not find file pronunciation: " + audioPath);
            return;
        }

        while (true) {
            playAudio(audioPath);
            System.out.print("\nPress 1 to play again or any other key to exit: ");
            String choice = scanner.nextLine().trim();

            if (!choice.equals("1")) {
                break;
            }
        }
    }

    private void playAudio(String audioPath) {
        try {
            // Tạo file VBS tạm để phát âm thanh ngầm
            File vbsFile = File.createTempFile("play_audio", ".vbs");
            vbsFile.deleteOnExit();
            
            String vbsContent = "Set Sound = CreateObject(\"WMPlayer.OCX.7\")\n" +
                               "Sound.URL = \"" + new File(audioPath).getAbsolutePath() + "\"\n" +
                               "Sound.Controls.play\n" +
                               "Do While Sound.playState <> 1\n" +
                               "  WScript.Sleep 100\n" +
                               "Loop";
            
            java.nio.file.Files.write(vbsFile.toPath(), vbsContent.getBytes());
            
            // Chạy file VBS
            Process process = Runtime.getRuntime().exec("wscript \"" + vbsFile.getAbsolutePath() + "\"");
            process.waitFor();
            
            System.out.println("Completed.");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void printIfNotBlank(String label, String value) {
        if (value != null && !value.isBlank()) {
            System.out.println(label + ": " + value);
        }
    }

    private void printList(String label, java.util.List<String> values) {
        if (values != null && !values.isEmpty()) {
            System.out.println(label + ": " + String.join(", ", values));
        }
    }
}
