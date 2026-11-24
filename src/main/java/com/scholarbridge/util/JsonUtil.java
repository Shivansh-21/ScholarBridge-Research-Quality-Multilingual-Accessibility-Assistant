package com.scholarbridge.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {}

    public static void json(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        json(response, status, "{\"error\":\"" + escape(message) + "\"}");
    }

    public static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '"') out.append("\\\"");
            else if (c == '\\') out.append("\\\\");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < ' ') out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.toString();
    }

    public static String map(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) json.append("null");
            else if (value instanceof Number || value instanceof Boolean) json.append(value);
            else json.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return json.append('}').toString();
    }
}
