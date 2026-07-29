package com.seoulcareconnect.integration.policy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExternalNodeReader {

    public String firstText(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject() || fieldNames == null) return null;

        for (String fieldName : fieldNames) {
            String value = asText(node.get(fieldName));
            if (value != null) return value;
        }

        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String actual = names.next();
            for (String requested : fieldNames) {
                if (actual.equalsIgnoreCase(requested)) {
                    String value = asText(node.get(actual));
                    if (value != null) return value;
                }
            }
        }

        return null;
    }

    public List<JsonNode> findObjectsContainingAny(JsonNode root, String... fieldNames) {
        List<JsonNode> results = new ArrayList<>();
        collect(root, results, lowerSet(fieldNames));
        return results;
    }

    public String stripHtml(String value) {
        if (value == null) return null;

        String result = value
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replaceAll("[ \\t\\r\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return result.isBlank() ? null : result;
    }

    public String joinNonBlank(String separator, String... values) {
        List<String> result = new ArrayList<>();

        if (values != null) {
            for (String value : values) {
                String cleaned = stripHtml(value);
                if (cleaned != null && !result.contains(cleaned)) result.add(cleaned);
            }
        }

        return result.isEmpty() ? null : String.join(separator, result);
    }

    private void collect(JsonNode node, List<JsonNode> results, Set<String> fields) {
        if (node == null) return;

        if (node.isObject()) {
            boolean matched = false;
            Iterator<String> fieldNames = node.fieldNames();

            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (fields.contains(name.toLowerCase(Locale.ROOT))) {
                    matched = true;
                    break;
                }
            }

            if (matched) results.add(node);
            node.elements().forEachRemaining(child -> collect(child, results, fields));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> collect(child, results, fields));
        }
    }

    private Set<String> lowerSet(String... values) {
        Set<String> result = new LinkedHashSet<>();

        if (values != null) {
            for (String value : values) {
                if (value != null) result.add(value.toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isNull() || node.isContainerNode()) return null;
        String value = node.asText().trim();
        return value.isBlank() ? null : value;
    }
}
