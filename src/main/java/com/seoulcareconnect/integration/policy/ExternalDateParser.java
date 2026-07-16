package com.seoulcareconnect.integration.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExternalDateParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyyMMddHHmm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    );

    private static final Pattern DATE_TOKEN = Pattern.compile(
            "([0-9]{4})\\s*(?:년|[./-])?\\s*"
                    + "([0-9]{1,2})\\s*(?:월|[./-])?\\s*"
                    + "([0-9]{1,2})\\s*(?:일)?"
    );

    public LocalDate parseSingle(String value) {
        String normalized = normalize(value);
        if (normalized == null || containsAlwaysText(normalized)) return null;

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(normalized, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }

        Matcher matcher = DATE_TOKEN.matcher(normalized);
        if (matcher.find()) return tokenToDate(matcher);
        return null;
    }

    public DateRange parseRange(String value) {
        String normalized = normalize(value);
        if (normalized == null || containsAlwaysText(normalized)) {
            return new DateRange(null, null);
        }

        Matcher matcher = DATE_TOKEN.matcher(normalized);
        LocalDate first = null;
        LocalDate second = null;

        if (matcher.find()) first = tokenToDate(matcher);
        if (matcher.find()) second = tokenToDate(matcher);

        return new DateRange(first, second);
    }

    public boolean containsAlwaysText(String... values) {
        if (values == null) return false;

        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null && (
                    normalized.contains("상시")
                            || normalized.contains("수시")
                            || normalized.contains("연중")
                            || normalized.contains("계속 모집")
                            || normalized.contains("예산 소진")
            )) {
                return true;
            }
        }
        return false;
    }

    private LocalDate tokenToDate(Matcher matcher) {
        try {
            return LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}