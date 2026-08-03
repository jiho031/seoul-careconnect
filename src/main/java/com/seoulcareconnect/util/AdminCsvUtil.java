package com.seoulcareconnect.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class AdminCsvUtil {

    private static final String UTF_8_BOM =
            "\uFEFF";

    private AdminCsvUtil() {
    }

    public static byte[] createCsv(
            List<String> headers,
            List<List<String>> rows
    ) {
        StringBuilder csv =
                new StringBuilder();

        csv.append(UTF_8_BOM);

        appendRow(
                csv,
                headers
        );

        for (List<String> row : rows) {
            appendRow(
                    csv,
                    row
            );
        }

        return csv.toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(
            StringBuilder csv,
            List<String> values
    ) {
        for (int index = 0;
             index < values.size();
             index++) {

            if (index > 0) {
                csv.append(",");
            }

            csv.append(
                    escape(values.get(index))
            );
        }

        csv.append("\r\n");
    }

    private static String escape(
            String value
    ) {
        if (value == null) {
            return "";
        }

        String normalized =
                value.replace(
                        "\"",
                        "\"\""
                );

        boolean requiresQuotes =
                normalized.contains(",")
                        || normalized.contains("\"")
                        || normalized.contains("\n")
                        || normalized.contains("\r");

        if (requiresQuotes) {
            return "\""
                    + normalized
                    + "\"";
        }

        return normalized;
    }

    public static String safe(
            Object value
    ) {
        return value == null
                ? ""
                : String.valueOf(value);
    }
}