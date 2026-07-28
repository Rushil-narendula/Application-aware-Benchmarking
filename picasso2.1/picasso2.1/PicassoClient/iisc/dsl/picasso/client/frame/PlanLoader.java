package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.common.ds.TreeNode;
import iisc.dsl.picasso.util.JsonPlanParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanLoader {
    private final JsonPlanParser parser;

    public PlanLoader() {
        this(new JsonPlanParser());
    }

    public PlanLoader(JsonPlanParser parser) {
        this.parser = parser;
    }

    public List<TreeNode> loadPlans(String fileName, String key) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty.");
        }
        if (key == null || (!"original".equalsIgnoreCase(key) && !"synthetic".equalsIgnoreCase(key))) {
            throw new IllegalArgumentException("Key must be 'original' or 'synthetic'.");
        }

        Path path = resolvePath(fileName);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Unable to locate plan file: " + fileName);
        }

        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return parsePlans(content, key.toLowerCase(Locale.ROOT));
    }

    private List<TreeNode> parsePlans(String content, String key) {
        List<TreeNode> plans = new ArrayList<TreeNode>();
        List<String> planObjects = extractPlanObjects(content, key);
        for (String planJson : planObjects) {
            TreeNode tree = parser.parse(planJson);
            if (tree != null) {
                plans.add(tree);
            }
        }
        return plans;
    }

    private List<String> extractPlanObjects(String content, String key) {
        List<String> planObjects = new ArrayList<String>();
        int arrayStart = findFirstNonWhitespace(content, 0);
        if (arrayStart < 0 || content.charAt(arrayStart) != '[') {
            throw new IllegalArgumentException("Expected a JSON array at the top level.");
        }

        int arrayEnd = findMatchingBracket(content, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            throw new IllegalArgumentException("Malformed JSON array.");
        }

        String arrayContent = content.substring(arrayStart + 1, arrayEnd);
        int searchIndex = 0;
        while (searchIndex < arrayContent.length()) {
            int objectStart = findNextObjectStart(arrayContent, searchIndex);
            if (objectStart < 0) {
                break;
            }

            String objectJson = readBalancedObject(arrayContent, objectStart);
            if (objectJson != null) {
                String planJson = extractNamedObject(objectJson, key);
                if (planJson != null) {
                    planObjects.add(planJson);
                }
                searchIndex = objectStart + objectJson.length();
            } else {
                searchIndex = objectStart + 1;
            }
        }
        return planObjects;
    }

    private String extractNamedObject(String objectJson, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:");
        Matcher matcher = pattern.matcher(objectJson);
        if (!matcher.find()) {
            return null;
        }

        int colonIndex = objectJson.indexOf(':', matcher.end() - 1);
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = skipWhitespace(objectJson, colonIndex + 1);
        if (valueStart >= objectJson.length() || objectJson.charAt(valueStart) != '{') {
            return null;
        }

        return readBalancedObject(objectJson, valueStart);
    }

    private String readBalancedObject(String content, int startIndex) {
        if (startIndex >= content.length() || content.charAt(startIndex) != '{') {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        for (int i = startIndex; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inString) {
                if (ch == '\\') {
                    i++;
                    continue;
                }
                if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(startIndex, i + 1);
                }
            }
        }
        return null;
    }

    private int findNextObjectStart(String content, int fromIndex) {
        boolean inString = false;
        for (int i = fromIndex; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inString) {
                if (ch == '\\') {
                    i++;
                    continue;
                }
                if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                return i;
            }
        }
        return -1;
    }

    private int findMatchingBracket(String content, int startIndex, char opening, char closing) {
        int depth = 0;
        boolean inString = false;
        for (int i = startIndex; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inString) {
                if (ch == '\\') {
                    i++;
                    continue;
                }
                if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == opening) {
                depth++;
            } else if (ch == closing) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findFirstNonWhitespace(String content, int fromIndex) {
        for (int i = fromIndex; i < content.length(); i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int skipWhitespace(String content, int fromIndex) {
        int i = fromIndex;
        while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
            i++;
        }
        return i;
    }

    private Path resolvePath(String fileName) {
        Path direct = Paths.get(fileName);
        if (Files.exists(direct)) {
            return direct;
        }

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path current = cwd;
        while (current != null) {
            Path candidate = current.resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }

        return direct;
    }
}
