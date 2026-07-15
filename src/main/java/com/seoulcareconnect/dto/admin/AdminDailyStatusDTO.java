package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Builder
public class AdminDailyStatusDTO {

    private LocalDate date;
    private long successCount;
    private long failCount;
    private long runCount;

    public String getDateText() {
        if (date == null) {
            return "-";
        }

        return date.format(
                DateTimeFormatter.ofPattern("M월 d일")
        );
    }

    public String getDayOfWeekText() {
        if (date == null) {
            return "-";
        }

        return date.format(
                DateTimeFormatter.ofPattern("E", Locale.KOREAN)
        );
    }

    public long getTotalCount() {
        return successCount + failCount;
    }
}