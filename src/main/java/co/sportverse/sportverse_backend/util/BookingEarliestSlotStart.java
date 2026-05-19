package co.sportverse.sportverse_backend.util;

import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives the earliest slot start instant for a booking using {@code slotIds} (e.g. {@code 8:00AM-9:00AM})
 * or embedded {@code slots} documents.
 */
public final class BookingEarliestSlotStart {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern AMPM_TIME = Pattern.compile(
            "^(\\d{1,2}):(\\d{2})\\s*([AP]M)$",
            Pattern.CASE_INSENSITIVE);

    private BookingEarliestSlotStart() {}

    public static ZonedDateTime earliestStart(Document booking) {
        return earliestStart(booking, DEFAULT_ZONE);
    }

    public static ZonedDateTime earliestStart(Document booking, ZoneId zone) {
        if (booking == null) {
            throw new IllegalArgumentException("booking is required");
        }
        String dateStr = booking.getString("date");
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Booking has no date");
        }
        LocalDate date = LocalDate.parse(dateStr.trim(), ISO_DATE);

        List<LocalTime> starts = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<String> slotIds = (List<String>) booking.get("slotIds");
        if (slotIds != null) {
            for (String slotId : slotIds) {
                if (slotId == null || slotId.isBlank()) {
                    continue;
                }
                String trimmed = slotId.trim();
                if (trimmed.contains("-")) {
                    String first = trimmed.split("-", 2)[0].trim();
                    starts.add(parseAmPmTime(first));
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Document> slotDocs = (List<Document>) booking.get("slots");
        if (slotDocs != null) {
            for (Document slot : slotDocs) {
                if (slot == null) {
                    continue;
                }
                String slotId = slot.getString("slotId");
                if (slotId != null && slotId.contains("-")) {
                    String first = slotId.split("-", 2)[0].trim();
                    starts.add(parseAmPmTime(first));
                    continue;
                }
                String startTimeAmPm = slot.getString("startTimeAmPm");
                if (startTimeAmPm != null && !startTimeAmPm.isBlank()) {
                    starts.add(parseAmPmTime(startTimeAmPm.trim()));
                    continue;
                }
                String startTime = slot.getString("startTime");
                String amPm = slot.getString("startTimeAmPm");
                if (startTime != null && !startTime.isBlank() && amPm != null && !amPm.isBlank()) {
                    String merged = (startTime.trim() + amPm.trim()).replaceAll("\\s+", "");
                    starts.add(parseAmPmTime(merged));
                }
            }
        }

        if (starts.isEmpty()) {
            throw new IllegalArgumentException("Cannot determine slot start time from booking");
        }

        LocalTime min = starts.stream().min(LocalTime::compareTo).orElseThrow();
        return min.atDate(date).atZone(zone);
    }

    private static LocalTime parseAmPmTime(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty slot time");
        }
        String compact = text.replaceAll("\\s+", "");
        Matcher m = AMPM_TIME.matcher(compact);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unrecognized slot time format: " + text);
        }
        int hour12 = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        boolean pm = m.group(3).equalsIgnoreCase("PM");
        if (hour12 < 1 || hour12 > 12) {
            throw new IllegalArgumentException("Invalid hour in slot time: " + text);
        }
        int h24 = hour12 % 12 + (pm ? 12 : 0);
        return LocalTime.of(h24, minute);
    }
}
