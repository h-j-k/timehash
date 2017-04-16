package com.ikueb;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * A utilities class for hashing to and unhashing from a short date-time representation,
 * with up to nanosecond precision.
 *
 * @see SubSecond
 */
public final class TimeHashUtils {

    private static final char[] CHARS =
            "456789BCDFGHJKLMNPQRSTVWXYZbcdfghjklmnpqrstvwxyz".toCharArray();
    private static final int RADIX = CHARS.length;
    private static final int MIN_CHARS = 6;
    private static final Pattern PATTERN = asPattern(MIN_CHARS);
    private static final Clock UTC = Clock.systemUTC();

    public static final int YEAR_EPOCH = 2014;
    public static final int YEAR_MAX = YEAR_EPOCH + RADIX - 1;


    private TimeHashUtils() {
        // empty
    }

    /**
     * Handles sub-seconds hashing and unhashing.
     * <table summary="Value description">
     * <thead>
     * <tr>
     * <td>Value</td>
     * <td>Period</td>
     * <td>Frequency</td>
     * <td>Precision compared to previous value</td>
     * <td>Number of characters including {@code YMdHms}</td>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>{@link #TRIM}</td>
     * <td>1 s</td>
     * <td>1 Hz</td>
     * <td>&nbsp;</td>
     * <td>6</td>
     * </tr>
     * <tr>
     * <td>{@link #MILLIGROUP}</td>
     * <td>25 ms</td>
     * <td>40 Hz</td>
     * <td>40x</td>
     * <td>7</td>
     * </tr>
     * <tr>
     * <td>{@link #MILLIS}</td>
     * <td>1 ms</td>
     * <td>1 KHz</td>
     * <td>25x</td>
     * <td>8</td>
     * </tr>
     * <tr>
     * <td>{@link #MICROGROUP}</td>
     * <td>10 μs</td>
     * <td>100 KHz</td>
     * <td>100x</td>
     * <td>9</td>
     * </tr>
     * <tr>
     * <td>{@link #NANOGROUP}</td>
     * <td>200 ns</td>
     * <td>5 MHz</td>
     * <td>50x</td>
     * <td>10</td>
     * </tr>
     * <tr>
     * <td>{@link #QUADNANO}</td>
     * <td>4 ns</td>
     * <td>250 MHz</td>
     * <td>50 x</td>
     * <td>11</td>
     * </tr>
     * <tr>
     * <td>{@link #NANOS}</td>
     * <td>1 ns</td>
     * <td>1 GHz</td>
     * <td>4x</td>
     * <td>12</td>
     * </tr>
     * </tbody>
     * </table>
     */
    enum SubSecond {
        /**
         * Trims sub-seconds, for a 6-character string.
         */
        TRIM(v -> 0, v -> "", asPattern(0), (v, t) -> t),
        /**
         * Provides precision up to every 25 milliseconds, for a 7-character string.<br>
         * Effectively 40 Hz, or 40x more precise than {@link #TRIM}.
         */
        MILLIGROUP(1, ChronoField.MILLI_OF_SECOND, 25),
        /**
         * Provides milliseconds precision, for an 8-character string.<br>
         * 25x more precise than {@link #MILLIGROUP}.
         */
        MILLIS(2, ChronoField.MILLI_OF_SECOND),
        /**
         * Provides precision up to every 10 microseconds, for a 9-character string.<br>
         * Effectively 100 KHz, or 100x more precise than {@link #MILLIS}.
         */
        MICROGROUP(3, ChronoField.MICRO_OF_SECOND, 10),
        /**
         * Provides precision up to every 200 nanoseconds, for a 10-character string.<br>
         * 50x more precise than {@link #MICROGROUP}.
         */
        NANOGROUP(4, ChronoField.NANO_OF_SECOND, 200),
        /**
         * Provides precision up to every 4 nanoseconds, for an 11-character string.<br>
         * 50x more precise than {@link #NANOGROUP}.
         */
        QUADNANO(5, ChronoField.NANO_OF_SECOND, 4),
        /**
         * Provides nanoseconds precision, for a 12-character string.<br>
         * 4x more precise than {@link #QUADNANO}.
         */
        NANOS(6, ChronoField.NANO_OF_SECOND);

        private static final SubSecond[] VALUES = SubSecond.values();

        private final Pattern pattern;
        private final ToIntFunction<LocalDateTime> extractor;
        private final IntFunction<String> hasher;
        private final BiFunction<String, Temporal, Temporal> unhasher;

        /**
         * @param length the desired length for representing sub-seconds
         * @param field  the field to extract from
         */
        SubSecond(int length, TemporalField field) {
            this(length, field, 1);
        }

        /**
         * @param length     the desired length for representing sub-seconds
         * @param field      the field to extract from
         * @param multiplier the multiplier to use
         */
        SubSecond(int length, TemporalField field, int multiplier) {
            this(v -> v.get(field), v -> toHash(v / multiplier, length), asPattern(length),
                    (v, t) -> t.with(field, multiplier * parse(v)));
        }

        /**
         * @param extractor for retrieving the sub-seconds value
         * @param hasher    converts the sub-seconds value to resulting hashed string
         * @param pattern   the pattern to validate a hashed sub-seconds string
         * @param unhasher  converts the hashed sub-seconds string to adjust the temporal
         */
        SubSecond(ToIntFunction<LocalDateTime> extractor,
                  IntFunction<String> hasher,
                  Pattern pattern,
                  BiFunction<String, Temporal, Temporal> unhasher) {
            this.extractor = extractor;
            this.hasher = hasher;
            this.pattern = pattern;
            this.unhasher = unhasher;
        }

        /**
         * @param value the value to extract sub-seconds value from
         * @return the sub-seconds value
         */
        private int extract(LocalDateTime value) {
            return extractor.applyAsInt(value);
        }

        /**
         * @param value the value to hash
         * @return the hashed value
         */
        private String hash(int value) {
            return hasher.apply(value);
        }

        /**
         * @return the {@link Pattern} for unhashing validation
         */
        private Pattern pattern() {
            return pattern;
        }

        /**
         * @param value the value to check for validity
         * @return {@code true} if the value is valid for unhashing
         */
        private boolean matches(String value) {
            return pattern.matcher(value).matches();
        }

        /**
         * @param value  the value to unhash
         * @param output the current {@link Temporal} value
         * @return the resulting {@link Temporal} instance
         */
        private Temporal unhash(String value, Temporal output) {
            return unhasher.apply(value, output);
        }
    }

    /**
     * @return hashed value of current system clock's UTC time with millisecond precision
     * @throws IllegalArgumentException if year is less than {@link #YEAR_EPOCH epoch year}
     *                                  or larger than {@link #YEAR_MAX max year}
     */
    public static String utcNowMillis() {
        return hashMillis(UTC);
    }

    /**
     * @param clock the clock to get the current time from
     * @return hashed value of the clock's current time with millisecond precision
     * @throws IllegalArgumentException if year is less than {@link #YEAR_EPOCH epoch year}
     *                                  or larger than {@link #YEAR_MAX max year}
     */
    public static String hashMillis(Clock clock) {
        return hash(LocalDateTime.now(clock), SubSecond.MILLIS);
    }

    /**
     * @param clock   the clock to get the current time from
     * @param handler the {@link SubSecond} value to handle sub-seconds
     * @return hashed value of the clock's current time with the desired precision
     * @throws IllegalArgumentException if year is less than {@link #YEAR_EPOCH epoch year}
     *                                  or larger than {@link #YEAR_MAX max year}
     */
    public static String hash(Clock clock, SubSecond handler) {
        return hash(LocalDateTime.now(clock), handler);
    }

    /**
     * Hashes the date-time with an appropriate precision given the following rule:
     * <table summary="Precision derivation">
     * <thead>
     * <tr>
     * <td>Nanoseconds</td>
     * <td>{@link SubSecond SubSecond} value</td>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>0</td>
     * <td>{@link SubSecond#TRIM TRIM}</td>
     * </tr>
     * <tr>
     * <td>Multiple of 1,000,000</td>
     * <td>{@link SubSecond#MILLIS MILLIS}</td>
     * </tr>
     * <tr>
     * <td>All other cases</td>
     * <td>{@link SubSecond#NANOS NANOS}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param value the date-time to hash
     * @return hashed value of the date-time with an appropriate precision
     * @throws IllegalArgumentException if year is less than {@link #YEAR_EPOCH epoch year}
     *                                  or larger than {@link #YEAR_MAX max year}
     */
    public static String hash(LocalDateTime value) {
        int subSecond = value.getNano();
        if (subSecond == 0) {
            return hash(value, SubSecond.TRIM);
        } else if (subSecond % 1e6 == 0) {
            return hash(value, SubSecond.MILLIS);
        }
        return hash(value, SubSecond.NANOS);
    }

    /**
     * @param value   the date-time to hash
     * @param handler the {@link SubSecond} value to handle sub-seconds
     * @return hashed value of the date-time with the desired precision
     * @throws IllegalArgumentException if year is less than {@link #YEAR_EPOCH epoch year}
     *                                  or larger than {@link #YEAR_MAX max year}
     */
    public static String hash(LocalDateTime value, SubSecond handler) {
        return hash(value.getYear(),
                value.getMonthValue(),
                value.getDayOfMonth(),
                value.get(ChronoField.SECOND_OF_DAY),
                handler.extract(value),
                handler);
    }

    /**
     * @param year        the year
     * @param month       the month of year
     * @param day         the day of month
     * @param secondOfDay the second of day
     * @param subSecond   the sub-seconds value
     * @param handler     the handler to hash the sub-seconds value
     * @return the hashed value with the desired precision
     */
    private static String hash(int year, int month, int day, int secondOfDay, int subSecond,
                               SubSecond handler) {
        if (year < YEAR_EPOCH) {
            throw new IllegalArgumentException("Year before " + YEAR_EPOCH);
        }
        if (year > YEAR_MAX) {
            throw new IllegalArgumentException("Year after " + YEAR_MAX);
        }
        StringBuilder result = new StringBuilder();
        IntStream.of(year - YEAR_EPOCH, month, day).forEach(v -> result.append(CHARS[v]));
        return result.append(toHash(secondOfDay, 3)).append(handler.hash(subSecond)).toString();
    }

    /**
     * @param value     the value to hash
     * @param padLength the expected length to pad to
     * @return hashed value
     */
    private static String toHash(int value, int padLength) {
        StringBuilder result = createWith(padLength);
        int remaining = value;
        for (int i = padLength - 1; i >= 0; i--) {
            result.setCharAt(i, CHARS[remaining % RADIX]);
            remaining /= RADIX;
        }
        return result.toString();
    }

    /**
     * @param padLength the expected length to pad to
     * @return the initial {@link StringBuilder}
     */
    private static StringBuilder createWith(int padLength) {
        StringBuilder builder = new StringBuilder(padLength);
        IntStream.range(0, padLength).forEach(i -> builder.append(CHARS[0]));
        return builder;
    }

    /**
     * Unhashes a string with the appropriate precision, based on its length
     *
     * @param hash the value to unhash
     * @return the resulting {@link LocalDateTime} representation
     * @throws IllegalArgumentException if the string does not pass validation
     */
    public static LocalDateTime unhash(String hash) {
        validate(hash, v -> v.length() <= MIN_CHARS + SubSecond.VALUES.length);
        return unhash(hash, SubSecond.VALUES[hash.length() - MIN_CHARS]);
    }

    /**
     * Unhashes a string with the given precision
     *
     * @param hash    the value to unhash
     * @param handler the {@link SubSecond} value to handle sub-seconds
     * @return the resulting {@link LocalDateTime} representation
     * @throws IllegalArgumentException if the string does not pass validation
     */
    public static LocalDateTime unhash(String hash, SubSecond handler) {
        validate(hash, v -> PATTERN.matcher(v.substring(0, MIN_CHARS)).matches());
        String subSecond = hash.substring(MIN_CHARS);
        validate(subSecond, handler::matches,
                "Subsecond does not match pattern: " + handler.pattern());
        LocalDateTime result = LocalDateTime.of(YEAR_EPOCH + parse(hash.charAt(0)),
                parse(hash.charAt(1)), parse(hash.charAt(2)), 0, 0)
                .with(ChronoField.SECOND_OF_DAY, parse(hash.substring(3, MIN_CHARS)));
        return handler.unhash(subSecond, result).query(LocalDateTime::from);
    }

    /**
     * @param value     the string to validate
     * @param predicate the {@link Predicate} to test the string
     * @throws IllegalArgumentException if the string does not pass validation
     */
    private static void validate(String value, Predicate<String> predicate) {
        validate(value, hash -> hash.length() >= MIN_CHARS && predicate.test(hash),
                "Does not match pattern: " + PATTERN.pattern());
    }

    /**
     * @param value     the string to validate
     * @param predicate the {@link Predicate} to test the string
     * @param message   the message to throw the {@link IllegalArgumentException} with
     * @throws IllegalArgumentException if the string does not pass validation
     */
    private static void validate(String value, Predicate<String> predicate, String message) {
        if (value == null || !predicate.test(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @param characters the hashed string to parse
     * @return the numeric representation from the hashed string
     */
    private static int parse(String characters) {
        int upperBound = characters.length() - 1;
        return IntStream.rangeClosed(0, upperBound)
                .map(i -> TimeHashUtils.parse(characters.charAt(i), upperBound - i)).sum();
    }

    /**
     * @param digit the digit to parse
     * @return the numeric representation of the hashed digit
     */
    private static int parse(char digit) {
        return parse(digit, 0);
    }

    /**
     * @param digit the digit to parse
     * @param power the power to use
     * @return the numeric representation of the hashed digit with the given power of the base
     */
    private static int parse(char digit, int power) {
        return (int) Math.pow(RADIX, power) * Arrays.binarySearch(CHARS, digit);
    }

    /**
     * @param length the required length
     * @return a {@link Pattern} that matches for valid characters of the required length
     */
    private static Pattern asPattern(int length) {
        return Pattern.compile(length == 0 ? "^$"
                : String.format("[%s]{%d}+", String.valueOf(CHARS), length));
    }

}
