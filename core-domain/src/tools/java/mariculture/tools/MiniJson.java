package mariculture.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reads a flat JSON object of string values (lang files). Anything else is rejected. */
final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    static Map<String, String> readStringMap(String json) {
        MiniJson p = new MiniJson(json);
        Map<String, String> out = new LinkedHashMap<>();
        p.ws();
        p.expect('{');
        p.ws();
        if (p.peek() == '}') return out;
        while (true) {
            p.ws();
            String k = p.string();
            p.ws();
            p.expect(':');
            p.ws();
            String v = p.string();
            if (out.put(k, v) != null) throw new IllegalArgumentException("Duplicate key " + k);
            p.ws();
            char c = p.next();
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Expected , or } at " + p.i);
        }
        return out;
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() {
        return s.charAt(i);
    }

    private char next() {
        if (i >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        return s.charAt(i++);
    }

    private void expect(char c) {
        if (next() != c) throw new IllegalArgumentException("Expected " + c + " at " + (i - 1));
    }
}
