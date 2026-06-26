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
            System.out.println("Không tìm thấy từ: " + request.getKeyword());
            return;
        }

        System.out.println("Từ: " + word.getKeyword());

        printIfNotBlank("Phiên âm", word.getPronunciation());
        printRelationMap("Từ đồng nghĩa", word.getSynonyms());
        printRelationMap("Từ trái nghĩa", word.getAntonyms());

        // Group definitions by type
        java.util.Map<String, java.util.List<Definition>> groupedDefinitions = new java.util.LinkedHashMap<>();
        for (Definition definition : word.getDefinitions()) {
            String type = definition.getType();
            if (type == null || type.isBlank()) {
                type = "Khác";
            }
            groupedDefinitions.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(definition);
        }

        // Display grouped by type
        int defCount = 1;
        for (java.util.Map.Entry<String, java.util.List<Definition>> entry : groupedDefinitions.entrySet()) {
            String type = entry.getKey();
            java.util.List<Definition> definitions = entry.getValue();

            System.out.println("----------------------------");
            System.out.println("Loại từ: " + type);

            for (Definition definition : definitions) {
                java.util.LinkedList<Sentence> examples = definition.getExamples();

                System.out.println("  Định nghĩa " + defCount + ":");
                printIfNotBlank("    Nghĩa", definition.getMeaning());

                if (examples != null && !examples.isEmpty()) {
                    for (int j = 0; j < examples.size(); j++) {
                        Sentence sentence = examples.get(j);
                        printIfNotBlank("    Ví dụ " + (j + 1), sentence.getContent());
                        printIfNotBlank("      Nghĩa ví dụ", sentence.getMeaning());
                    }
                }
                defCount++;
            }
        }

        if (scanner != null) {
            String audioPath = "database/" + word.getKeyword() + ".mp3";
            System.out.print("\nNhấn 1 để phát âm hoặc phím bất kỳ để thoát: ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                playAudioLoop(audioPath);
            }
        }
    }

    private void playAudioLoop(String audioPath) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            System.out.println("Không tìm thấy file phát âm: " + audioPath);
            return;
        }

        while (true) {
            playAudio(audioPath);
            System.out.print("\nNhấn 1 để phát lại hoặc phím bất kỳ để thoát: ");
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

            System.out.println("Hoàn thành.");

        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private void printIfNotBlank(String label, String value) {
        if (value != null && !value.isBlank()) {
            System.out.println(label + ": " + value);
        }
    }

    private void printRelationMap(String label, java.util.Map<String, java.util.LinkedList<String>> relationMap) {
        if (relationMap != null && !relationMap.isEmpty()) {
            for (java.util.Map.Entry<String, java.util.LinkedList<String>> entry : relationMap.entrySet()) {
                String wordType = entry.getKey();
                java.util.LinkedList<String> values = entry.getValue();
                if (!values.isEmpty()) {
                    System.out.println(label + " (" + wordType + "): " + String.join(", ", values));
                }
            }
        }
    }
}
