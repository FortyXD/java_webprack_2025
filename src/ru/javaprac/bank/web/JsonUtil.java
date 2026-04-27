package ru.javaprac.bank.web;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.Map;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        StringBuilder out = new StringBuilder();
        appendJson(out, value);
        return out.toString();
    }

    private static void appendJson(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String || value instanceof Character || value instanceof Enum<?> ||
                value instanceof TemporalAccessor) {
            appendString(out, value.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(out, map);
        } else if (value instanceof Iterable<?> iterable) {
            appendIterable(out, iterable.iterator());
        } else if (value.getClass().isArray()) {
            appendArray(out, value);
        } else {
            appendString(out, value.toString());
        }
    }

    private static void appendMap(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            appendString(out, String.valueOf(entry.getKey()));
            out.append(':');
            appendJson(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendIterable(StringBuilder out, Iterator<?> iterator) {
        out.append('[');
        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            appendJson(out, iterator.next());
        }
        out.append(']');
    }

    private static void appendArray(StringBuilder out, Object array) {
        out.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                out.append(',');
            }
            appendJson(out, Array.get(array, i));
        }
        out.append(']');
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
