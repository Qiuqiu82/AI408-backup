package org.example.ai408.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DateUtils {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private DateUtils() {
    }

    public static String todayKey() {
        return LocalDate.now(SHANGHAI).toString();
    }
}
