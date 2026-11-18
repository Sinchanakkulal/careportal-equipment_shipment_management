package common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    private DateTimeUtil(){ }

    private static final DateTimeFormatter ISO_DATE_TIME = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(ISO_DATE_TIME);
    }
}
