package iisc.dsl.picasso.util;

import iisc.dsl.picasso.common.ds.TreeNode;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class JsonPlanParser {
    public TreeNode parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON input is empty.");
        }

        JsonParser parser = new JsonParser(json.trim());
        Object parsed = parser.parseValue();

        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Top-level JSON value must be an object.");
        }

        Map<String, Object> root = (Map<String, Object>) parsed;
        Object original = root.get("original");
        if (original instanceof Map) {
            return buildTree((Map<String, Object>) original, null, 0);
        }
        return buildTree(root, null, 0);
    }

    private TreeNode buildTree(Map<String, Object> nodeData, TreeNode parent, int depth) {
        TreeNode node = new TreeNode(depth, parent);
        String nodeName = extractNodeName(nodeData);
        Hashtable attributes = toHashtable(nodeData.get("attributes"));
        addAttribute(attributes, "relation", nodeData.get("relation"));
        addAttribute(attributes, "alias", nodeData.get("alias"));
        addAttribute(attributes, "width", nodeData.get("width"));
        addAttribute(attributes, "id", nodeData.get("id"));

        node.setAttributes(attributes);
        node.setNodeValues(
                nodeName,
                0,
                toDouble(nodeData.get("startupCost")),
                toDouble(nodeData.get("totalCost")),
                toDouble(nodeData.get("rows")),
                null,
                null);

        Vector children = new Vector();
        Object childValue = nodeData.get("children");
        if (childValue instanceof List) {
            for (Object childObj : (List<?>) childValue) {
                if (childObj instanceof Map) {
                    TreeNode child = buildTree((Map<String, Object>) childObj, node, depth + 1);
                    child.setParent(node);
                    children.add(child);
                }
            }
        }

        node.setChildren(children);
        return node;
    }

    private String extractNodeName(Map<String, Object> nodeData) {
        Object value = nodeData.get("nodeName");
        if (value == null) {
            value = nodeData.get("node_name");
        }
        if (value == null) {
            value = nodeData.get("name");
        }
        return value == null ? "" : String.valueOf(value);
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private void addAttribute(Hashtable attributes, String key, Object value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    private Hashtable toHashtable(Object attributes) {
        Hashtable table = new Hashtable();
        if (attributes instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) attributes).entrySet()) {
                table.put(entry.getKey(), entry.getValue());
            }
        }
        return table;
    }

    private static class JsonParser {
        private final String input;
        private int index;

        private JsonParser(String input) {
            this.input = input;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input.");
            }

            char current = input.charAt(index);
            if (current == '{') {
                return parseObject();
            }
            if (current == '[') {
                return parseArray();
            }
            if (current == '"') {
                return parseString();
            }
            if (current == 't') {
                return parseLiteral("true", Boolean.TRUE);
            }
            if (current == 'f') {
                return parseLiteral("false", Boolean.FALSE);
            }
            if (current == 'n') {
                return parseLiteral("null", null);
            }
            if (current == '-' || Character.isDigit(current)) {
                return parseNumber();
            }

            throw new IllegalArgumentException("Unexpected character at position " + index + ": " + current);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();

            if (peek('}')) {
                index++;
                return new LinkedHashMap<String, Object>();
            }

            Map<String, Object> object = new LinkedHashMap<String, Object>();
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return object;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();

            if (peek(']')) {
                index++;
                return new java.util.ArrayList<Object>();
            }

            List<Object> array = new java.util.ArrayList<Object>();
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < input.length()) {
                char ch = input.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= input.length()) {
                        throw new IllegalArgumentException("Invalid escape sequence in JSON string.");
                    }
                    char escape = input.charAt(index++);
                    switch (escape) {
                        case '"':
                            builder.append('"');
                            break;
                        case '\\':
                            builder.append('\\');
                            break;
                        case '/':
                            builder.append('/');
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > input.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape in JSON string.");
                            }
                            String hex = input.substring(index, index + 4);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported escape sequence: \\" + escape);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Object parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (index < input.length() && input.charAt(index) == '.') {
                index++;
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (index < input.length() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (index < input.length() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }

            String token = input.substring(start, index);
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        }

        private Object parseLiteral(String literal, Object value) {
            if (input.startsWith(literal, index)) {
                index += literal.length();
                return value;
            }
            throw new IllegalArgumentException("Invalid literal at position " + index + ": " + literal);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= input.length() || input.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < input.length() && input.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }
    }
}
