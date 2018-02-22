package uk.gov.dvsa.motr.notifications.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatterForSmsDisplay {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    public static String asFormattedForSmsDate(LocalDate date) {
        return date.format(formatter);
    }
}
