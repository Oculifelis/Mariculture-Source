package mariculture.domain.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict JSON parser for data-pack content, so content rules can be parsed and validated without a
 * loader. Objects become {@code Map<String,Object>} (insertion ordered), arrays {@code List},
 * numbers {@code Double}, plus String, Boolean and null. Duplicate keys and trailing data are errors.
 */
public final class JsonReader {
    private final String s;
    private int i;

    private JsonReader(String s) {
        this.s = s;
    }

    public static Object parse(String json) {
        JsonReader r = new JsonReader(json);
        r.ws();
        Object v = r.value();
        r.ws();
        if (r.i != json.length()) throw r.error("Trailing data");
        return v;
    }

    private Object value() {
        if (i >= s.length()) throw error("Unexpected end");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            if (peek() != '"') throw error("Expected key");
            String k = string();
            ws();
            expect(':');
            ws();
            if (m.containsKey(k)) throw error("Duplicate key '" + k + "'");
            m.put(k, value());
            ws();
            char c = next();
            if (c == '}') return m;
            if (c != ',') throw error("Expected , or }");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = next();
            if (c == ']') return l;
            if (c != ',') throw error("Expected , or ]");
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') return sb.toString();
            if (c < 0x20) throw error("Control character in string");
            if (c != '\\') { sb.append(c); continue; }
            char e = next();
            switch (e) {
                case '"': case '\\': case '/': sb.append(e); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw error("Bad unicode escape");
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: throw error("Bad escape \\" + e);
            }
        }
    }

    private Double number() {
        int start = i;
        if (peek() == '-') i++;
        while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) i++;
        try {
            return Double.valueOf(s.substring(start, i));
        } catch (NumberFormatException e) {
            throw error("Bad number");
        }
    }

    private void literal(String word) {
        if (!s.startsWith(word, i)) throw error("Expected " + word);
        i += word.length();
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() {
        if (i >= s.length()) throw error("Unexpected end");
        return s.charAt(i);
    }

    private char next() {
        char c = peek();
        i++;
        return c;
    }

    private void expect(char c) {
        if (next() != c) throw error("Expected '" + c + "'");
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " at offset " + i);
    }
}
