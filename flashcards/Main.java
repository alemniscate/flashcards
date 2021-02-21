package flashcards;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

public class Main {

    public static void main(String[] args) {

        Arguments arguments = new Arguments(args);
        String importFileName = arguments.get("-import", "");
        String exportFileName = arguments.get("-export", "");

        Log log = new Log();

        Action ac = new Action(log, importFileName, exportFileName);

        while(true) {
            log.out("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):");
            String command = log.in();
            switch (command) {
                case "add":
                    ac.add();
                    break;
                case "remove":
                    ac.remove();
                    break;
                case "import":
                    ac.importAction();
                    break;
                case "export":
                    ac.export();
                    break;
                case "ask":
                    ac.ask();
                    break;
                case "exit":
                    ac.exit();
                    break;
                case "log":
                    ac.log();
                    break;
                case "hardest card":
                    ac.hardest();
                    break;
                case "reset stats":
                    ac.reset();
                    break;
            }
            if ("exit".equals(command)) {
                break;
            }
            log.out("");
        }

        log.finish();
    }
}

class Log {

    List<String> logList;
    Scanner scanner;

    Log() {
        logList = new ArrayList<>();
        scanner = new Scanner(System.in);
    }

    void out(String output) {
        System.out.println(output);
        logList.add(output);
    }

    String in() {
        String input = scanner.nextLine();
        logList.add(input);
        return input;
    }

    void save(String fileName) {
        StringBuilder sb = new StringBuilder();
        logList.stream().forEach(s -> sb.append(s + "\n"));    
        String text = sb.toString();
        WriteText.writeAll(fileName, text);
    }

    void finish() {
        scanner.close();
    }
}

class Action {

    Map<String, String> map;
    Map<String, Integer> mapStats;
    Log log;
    String importFileName;
    String exportFileName;

    Action(Log log, String importFileName, String exportFileName) {
        this.log = log;
        this.importFileName = importFileName;
        this.exportFileName = exportFileName;
        map = new LinkedHashMap<>();
        mapStats = new LinkedHashMap<>();

        if (!importFileName.isEmpty()) {
            load(importFileName);
        }
    }

    void add() {
        log.out("The Card:");
        String term = log.in();
        if (map.containsKey(term)) {
            log.out(String.format("The card \"%s\" already exists.", term));
            return;
        }
        log.out("The definition of the card:");
        String definition = log.in();
        if (map.containsValue(definition)) {
            log.out(String.format("The definition \"%s\" already exists.", definition));
            return;
        }
        map.put(term, definition);
        mapStats.put(term, 0);
        log.out(String.format("The pair (\"%s\":\"%s\") has been added.", term, definition));
    }

    void remove() {
        log.out("Which card?");
        String term = log.in();
        if (map.containsKey(term)) {
            map.remove(term);
            mapStats.remove(term);
            log.out("The card has been removed.");
        } else {
            log.out(String.format("Can't remove \"%s\": there is no such card.", term));
        }
    }

    static String getTermFromDefinition(String answer, Map<String, String> map) {
        String term = "";
        for (var entry: map.entrySet()) {
            String definition = entry.getValue();
            if (answer.equals(definition)) {
                term = entry.getKey();
                return term;
            }
        }
        return term;
    }

    void ask() {
        log.out("How many times to ask?");
        int n = Integer.parseInt(log.in());
        List<String> terms = new ArrayList<>(map.keySet());
        int m = terms.size();
        for (int i = 0; i < n; i++) {
            int index = i % m;
            String term = terms.get(index);
            log.out(String.format("Print the definition of \"%s\":", term));
            String answer = log.in();
            String definition = map.get(term);
            if (answer.equals(definition)) {
                log.out("Correct!");
            } else {
                mapStats.put(term, mapStats.get(term) + 1);
                String term2= getTermFromDefinition(answer, map);
                if (term2.isEmpty()) {
                    log.out(String.format("Wrong. The right answer is \"%s\".", definition));
                } else {
                    log.out(String.format("Wrong. The right answer is \"%s\", but your definition is correct for \"%s\".", definition, term2));
                }
            }
        }

    }

    void importAction() {
        log.out("File name:");
        String fileName = log.in();
        load(fileName);
    }

    void load(String fileName) {
        if (!ReadText.isExist(fileName)) {
            log.out("File not found.");
            return;
        }
        String text = ReadText.readAll(fileName);
        text = text.replaceAll("\r", "");
        String[] items = text.split("\n");
        for (int i = 0; i < items.length; i+= 3) {
            String term = items[i];
            String definition = items[i + 1];
            int mistakes = Integer.parseInt(items[i + 2]);
            map.put(term, definition);
            mapStats.put(term, mistakes);
        }
        log.out(String.format("%d cards have been loaded.", items.length / 3));
    } 

    void export() {
        log.out("File name:");
        String fileName = log.in();
        save(fileName);
    }

    void save(String fileName) {
        StringBuilder sb = new StringBuilder();
        for (String term: map.keySet()) {
            sb.append(term + "\n");
            sb.append(map.get(term) + "\n");
            sb.append(mapStats.get(term) + "\n");
        }
        String text = sb.toString();
        WriteText.writeAll(fileName, text);
        log.out(String.format("%d cards have been saved.", map.entrySet().size()));
    }

    void exit() {
        log.out("Bye bye!");
        if (!exportFileName.isEmpty()) {
            save(exportFileName);
        }
    }

    void log() {
        log.out("File name:");
        String fileName = log.in();
        log.save(fileName);
        log.out("The log has been saved.");
    }

    void hardest() {
        int max = mapStats.values().stream().max(Comparator.naturalOrder()).orElse(0);
        if (max == 0) {
            log.out("There are no cards with errors.");
            return;
        }
        List<String> list = mapStats.entrySet().stream().filter(e -> e.getValue() == max).map(e -> e.getKey()).collect(Collectors.toList());
        if (list.size() == 1) {
            log.out(String.format("The hardest card is \"%s\". You have %d errors answering it.", list.get(0), max));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"" + list.get(i) + "\"");
            }
            String terms = sb.toString();
            log.out(String.format("The hardest cards are %s. You have %d errors answering them.", terms, max));
        }
    }

    void reset() {
        for (String term: mapStats.keySet()) {
            mapStats.put(term, 0);
        }
        log.out("Card statistics have been reset.");
    }
 }


class ReadText {

    static boolean isExist(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    static String getAbsolutePath(String fileName) {
        File file = new File(fileName);
        return file.getAbsolutePath();
    }

    static String readAllWithoutEol(String fileName) {
        String text = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));   
            text =  br.lines().collect(Collectors.joining());        
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return text;
    }

    static List<String> readLines(String fileName) {
        List<String> lines = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));   
            lines =  br.lines().collect(Collectors.toList());        
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return lines;
    }

    static String readAll(String fileName) {
        char[] cbuf = new char[4096];
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));           
            while (true) {
                int length = br.read(cbuf, 0, cbuf.length);
                if (length != -1) {
                    sb.append(cbuf, 0, length);
                }
                if (length < cbuf.length) {
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sb.toString();
    }
}

class WriteText {

    static void writeAll(String fileName, String text) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            bw.write(text, 0, text.length());
            bw.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class Arguments {

    Map<String, String> argMap;

    Arguments(String[] args) {
        
        List<String> argList = Arrays.asList(args);
        argMap = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            argMap.put(argList.get(i), argList.get(i + 1));
        }
    }

    String get(String key, String defaultValue) {
        if (argMap.isEmpty()) {
            return defaultValue;
        }

        if (argMap.get(key) == null) {
            return defaultValue;
        }

        return argMap.get(key);
    }
}