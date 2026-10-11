package me.ancal.wmdroid.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Korean lunar dates supplied by a resource pack; no built-in year table.
 * Load off the UI thread. The immutable result remains valid after a pack change.
 * Gregorian civil dates stay the Calendar provider and selection identities.
 */
public final class WmLunarCalendar {
    public static final String RESOURCE_ID = "assets/calendar/lunar/korean.json";
    private static final int MAX_BYTES = 32768;
    private static final int[] CIVIL_MONTH_LENGTHS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private final int[] years, starts;
    private final String[] days, months, leapMonths;

    public static final class Date {
        public final int year, month, day, monthLength;
        public final boolean leapMonth;
        private Date(int year, int month, int day, boolean leapMonth, int monthLength) {
            this.year = year; this.month = month; this.day = day;
            this.leapMonth = leapMonth; this.monthLength = monthLength;
        }
    }

    private WmLunarCalendar(int[] years, String[] days, String[] months, String[] leapMonths) {
        this.years = years; this.days = days; this.months = months; this.leapMonths = leapMonths;
        starts = new int[years.length + 1];
        for (int i = 0; i < years.length; i++) {
            int total = 0, word = years[i];
            for (int month = 1; month <= 12; month++) total += monthLength(word, month, false);
            if ((word & 15) != 0) total += monthLength(word, word & 15, true);
            starts[i + 1] = starts[i] + total;
        }
    }

    /** Null if the pack or resource is absent. Invalid data throws IOException.
     * Opens and closes its own pack stream; does not modify the active pack.
     */
    public static WmLunarCalendar load(WmResourcePack pack) throws IOException {
        if (pack == null || !pack.ids().contains(RESOURCE_ID)) return null;
        byte[] data;
        try (InputStream input = pack.open(RESOURCE_ID); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_BYTES) throw new IOException("Lunar data too large");
                output.write(buffer, 0, count);
            }
            data = output.toByteArray();
        }
        try {
            String text = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data)).toString();
            JSONTokener tokens = new JSONTokener(text);
            Object value = tokens.nextValue();
            if (!(value instanceof JSONObject) || tokens.nextClean() != 0) throw new IOException("Invalid lunar JSON");
            JSONObject root = (JSONObject) value;
            if (integer(root.get("version")) != 1 || integer(root.get("first_year")) != 1900
                    || !"1900-01-31".equals(root.get("epoch"))) throw new IOException("Unsupported lunar schema");
            JSONArray source = root.getJSONArray("years");
            if (source.length() != 150) throw new IOException("Invalid lunar year count");
            int[] years = new int[150];
            for (int i = 0; i < years.length; i++) {
                int word = integer(source.get(i));
                if (word < 0 || word > 0x1ffff || (word & 15) > 12) throw new IOException("Invalid lunar year word");
                years[i] = word;
            }
            JSONObject captions = root.getJSONObject("captions");
            return new WmLunarCalendar(years, captions(captions, "days", 29),
                captions(captions, "months", 12), captions(captions, "leap_months", 12));
        } catch (JSONException error) {
            throw new IOException("Invalid lunar data", error);
        }
    }

    private static int integer(Object value) throws IOException {
        if (!(value instanceof Integer) && !(value instanceof Long)) throw new IOException("Lunar integer required");
        long number = ((Number) value).longValue();
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new IOException("Lunar integer outside range");
        return (int) number;
    }

    private static String[] captions(JSONObject object, String key, int count) throws JSONException, IOException {
        JSONArray source = object.getJSONArray(key);
        if (source.length() != count) throw new IOException("Invalid lunar caption count");
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            Object value = source.get(i);
            if (!(value instanceof String)) throw new IOException("Lunar caption text required");
            String text = (String) value;
            if (text.trim().length() == 0 || text.length() > 64) throw new IOException("Invalid lunar caption length");
            for (int j = 0; j < text.length(); j++) {
                char c = text.charAt(j);
                if (Character.isHighSurrogate(c)) {
                    if (++j >= text.length() || !Character.isLowSurrogate(text.charAt(j))) throw new IOException("Invalid lunar caption surrogate");
                } else if (Character.isLowSurrogate(c) || Character.isISOControl(c)) {
                    throw new IOException("Invalid lunar caption character");
                }
            }
            result[i] = text;
        }
        return result;
    }

    /** Invalid Gregorian dates and dates outside the supplied 150 years return null.
     * Civil input is independent of device timezone. January is month1.
     */
    public Date convert(int year, int month, int day) {
        // Even 150 maximal 13-month years end before 2062; bound arithmetic first.
        if (year < 1900 || year > 2061 || month < 1 || month > 12) return null;
        boolean leap = leapYear(year);
        int civilLength = CIVIL_MONTH_LENGTHS[month - 1] + (month == 2 && leap ? 1 : 0);
        if (day < 1 || day > civilLength) return null;
        int before = year - 1;
        int offset = (year - 1900) * 365 + before / 4 - 1899 / 4
            - before / 100 + 1899 / 100 + before / 400 - 1899 / 400 + day - 1 - 30;
        for (int i = 1; i < month; i++) offset += CIVIL_MONTH_LENGTHS[i - 1];
        if (month > 2 && leap) offset++;
        if (offset < 0 || offset >= starts[years.length]) return null;
        int low = 0, high = years.length;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (starts[middle] <= offset) low = middle; else high = middle;
        }
        int remaining = offset - starts[low], word = years[low];
        for (int lunarMonth = 1; lunarMonth <= 12; lunarMonth++) {
            int length = monthLength(word, lunarMonth, false);
            if (remaining < length) return new Date(1900 + low, lunarMonth, remaining + 1, false, length);
            remaining -= length;
            if ((word & 15) == lunarMonth) {
                length = monthLength(word, lunarMonth, true);
                if (remaining < length) return new Date(1900 + low, lunarMonth, remaining + 1, true, length);
                remaining -= length;
            }
        }
        return null;
    }

    /** Day1 uses its month caption; other days use day2..30 captions. Null returns null. */
    public String caption(Date date) {
        if (date == null) return null;
        return date.day == 1 ? (date.leapMonth ? leapMonths : months)[date.month - 1] : days[date.day - 2];
    }

    private static int monthLength(int word, int month, boolean leap) {
        return 29 + ((word >>> (leap ? 16 : 16 - month)) & 1);
    }
    private static boolean leapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
