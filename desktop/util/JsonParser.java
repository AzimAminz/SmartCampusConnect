package util;

import java.util.*;
import java.util.regex.*;

public class JsonParser {
    
    /**
     * Parses a flat JSON object into key-value pairs.
     */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }
        
        // Match key-value pairs: "key": "value" or "key": 123 or "key": true/false/null
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([\\w\\.\\-\\:]+))");
        Matcher matcher = pattern.matcher(json);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if ("null".equals(value)) {
                value = null;
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Parses a JSON array containing flat objects into a list of maps.
     */
    public static List<Map<String, String>> parseList(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        
        trimmed = trimmed.trim();
        if (trimmed.isEmpty()) {
            return list;
        }
        
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(trimmed);
        
        while (matcher.find()) {
            String objContent = matcher.group(0);
            list.add(parseObject(objContent));
        }
        
        return list;
    }
}
