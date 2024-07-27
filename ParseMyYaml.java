import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML decoder: It reads a YAML file and converts it to a Map<String, Object> object.
 */

public class ParseMyYaml {

    private static final String DEFAULT_DELIMITER = "/";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private final String indent;
    private final KeyPathFactory keyPathFactory;

    public ParseMyYaml() {
        this(2, DEFAULT_DELIMITER);
    }

    public ParseMyYaml(int indentSize) {
        this(indentSize, DEFAULT_DELIMITER);
    }

    public ParseMyYaml(String delimiter) {
        this(2, delimiter);
    }

    public ParseMyYaml(int indentSize, String delimiter) {
        if (indentSize < 2) {
            throw new IllegalArgumentException("indentSize must be >= 2");
        }
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("delimiter must not be null or empty");
        }

        this.indent = " ".repeat(indentSize);
        this.keyPathFactory = new KeyPathFactory(delimiter);
    }

    public Map<String, Object> decode(String filePath) throws IOException {
        return decode(new FileInputStream(filePath), DEFAULT_CHARSET);
    }

    public Map<String, Object> decode(InputStream inputStream) throws IOException {
        return decode(inputStream, DEFAULT_CHARSET);
    }

    public Map<String, Object> decode(String filePath, String charset) throws IOException {
        return decode(new FileInputStream(filePath), charset);
    }

    public Map<String, Object> decode(InputStream inputStream, String charset) throws IOException {
        Map<String, Object> yamlMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        List<Object> list = null;
        String currentKeyPath = "";

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            String[] keyValue = line.split(":");
            String key = keyValue[0];
            int indentLevel = (int) (Math.floor(key.lastIndexOf(indent) * 0.5) + 1);

            if (key.trim().startsWith("-")) {
                if (list == null) {
                    list = new ArrayList<>();
                }
                String[] listValue = line.split("-");
                list.add(listValue.length == 2 ? listValue[1].trim() : "");
            } else {
                if (list != null && !list.isEmpty()) {
                    yamlMap.put(currentKeyPath, new ArrayList<>(list));
                    list.clear();
                    list = null;
                }

                currentKeyPath = keyPathFactory.createKeyPath(indentLevel, key);

                if (keyValue.length > 1) {
                    String value = keyValue[1].trim().replace("\"", "");
                    yamlMap.put(currentKeyPath, value);
                }
            }
        }

        if (list != null && !list.isEmpty()) {
            yamlMap.put(currentKeyPath, new ArrayList<>(list));
        }

        keyPathFactory.clear();
        return yamlMap;
    }

    public Map<String, Object> decodeOrEmpty(String filePath) {
        return decodeOrEmpty(filePath, DEFAULT_CHARSET);
    }

    public Map<String, Object> decodeOrEmpty(InputStream inputStream) {
        return decodeOrEmpty(inputStream, DEFAULT_CHARSET);
    }

    public Map<String, Object> decodeOrEmpty(String filePath, String charset) {
        try {
            return decode(new FileInputStream(filePath), charset);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public Map<String, Object> decodeOrEmpty(InputStream inputStream, String charset) {
        try {
            return decode(inputStream, charset);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static void dump(String filePath) {
        dump(filePath, DEFAULT_CHARSET);
    }

    public static void dump(InputStream inputStream) {
        dump(inputStream, DEFAULT_CHARSET);
    }

    public static void dump(String filePath, String charset) {
        try {
            dump(new FileInputStream(filePath), charset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void dump(InputStream inputStream, String charset) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            StringBuilder builder = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");
            boolean isEmpty = true;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                if (!isEmpty) {
                    builder.append(lineSeparator);
                }

                builder.append(line);
                isEmpty = false;
            }

            System.out.println(builder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class KeyPathFactory {
        private final List<String> keys = new ArrayList<>();
        private final String delimiter;

        public KeyPathFactory(String delimiter) {
            this.delimiter = delimiter;
        }

        public String createKeyPath(int index, String key) {
            key = key.trim();

            if (index < keys.size()) {
                keys.set(index, key);
                for (int i = keys.size() - 1; i > index; i--) {
                    keys.remove(i);
                }
            } else {
                keys.add(key);
            }

            return String.join(delimiter, keys);
        }

        public void clear() {
            keys.clear();
        }
    }
}
