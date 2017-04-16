/*
 * Copyright 2017 h-j-k. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ikueb;

import com.ikueb.TimeHashUtils.SubSecond;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.LocalDateTime.of;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TimeHashUtilsTest {

    enum TestCase {
        MIN(of(TimeHashUtils.YEAR_EPOCH, 1, 1, 0, 0),
                with(0, "455444"),
                with(0, "4554444"),
                with(0, "45544444"),
                with(0, "455444444"),
                with(0, "4554444444"),
                with(0, "45544444444"),
                with(0, "455444444444")),
        MAX(of(TimeHashUtils.YEAR_MAX, 12, 31, 23, 59, 59, 999999999),
                with(0, "zJgnWz"),
                with(975_000_000, "zJgnWzq"),
                with(999_000_000, "zJgnWzSq"),
                with(999_990_000, "zJgnWzvRM"),
                with(999_999_800, "zJgnWzxGBg"),
                with(999_999_996, "zJgnWzz8ZxM"),
                with(999_999_999, "zJgnWz7wQHnM")),
        ASC(of(2017, 1, 2, 3, 45, 6, 789_012_345),
                with(0, "7569sQ"),
                with(775_000_000, "7569sQg"),
                with(789_000_000, "7569sQNT"),
                with(789_010_000, "7569sQkHn"),
                with(789_012_200, "7569sQlhJn"),
                with(789_012_344, "7569sQnCdML"),
                with(789_012_345, "7569sQ78fTKF")),
        DESC(of(2017, 9, 8, 7, 6, 54, 321_098_765),
                with(0, "7FDH9f"),
                with(300_000_000, "7FDH9fJ"),
                with(321_000_000, "7FDH9fBj"),
                with(321_090_000, "7FDH9fKwx"),
                with(321_098_600, "7FDH9fLXqn"),
                with(321_098_764, "7FDH9fM9sTR"),
                with(321_098_765, "7FDH9f5JWTnd")),
        NANOS(of(TimeHashUtils.YEAR_EPOCH, 1, 1, 0, 0, 0, 1),
                with(1, "455444444445")),
        MILLIS(of(2017, 1, 2, 3, 45, 6, 789_000_000),
                with(789_000_000, "7569sQNT")),
        SECONDS(of(2017, 9, 8, 7, 6, 54),
                with(0, "7FDH9f"));

        private static final Set<TestCase> NULL_HANDLER = EnumSet.of(NANOS, MILLIS, SECONDS);

        private final LocalDateTime temporal;
        private final Map<String, LocalDateTime> expected;

        TestCase(LocalDateTime temporal,
                 Function<LocalDateTime, Stream<Entry<String, LocalDateTime>>>... values) {
            this.temporal = temporal;
            this.expected = Arrays.stream(values)
                    .flatMap(v -> v.apply(temporal.withNano(0)))
                    .collect(toMap(Entry::getKey, Entry::getValue));
        }

        private static Function<LocalDateTime, Stream<Entry<String, LocalDateTime>>> with(
                int nanos, String expected) {
            return temporal -> singletonMap(expected, temporal.with(NANO_OF_SECOND, nanos))
                    .entrySet().stream();
        }

        public Stream<Object[]> getHashParameters() {
            return expected.entrySet().stream()
                    .map(entry -> new Object[]{temporal,
                            NULL_HANDLER.contains(this) ? null : getHandler(entry.getKey()),
                            entry.getKey()});
        }

        public Stream<Object[]> getUnhashParameters() {
            return expected.entrySet().stream()
                    .map(entry -> new Object[]{entry.getKey(),
                            NULL_HANDLER.contains(this) ? null : getHandler(entry.getKey()),
                            entry.getValue()});
        }

        public Stream<Object[]> getValidationExceptionParameters() {
            return expected.entrySet().stream()
                    .flatMap(entry ->
                            Stream.of(entry.getKey() + "4", "*" + entry.getKey().substring(1))
                                    .map(v -> new Object[]{v, getHandler(entry.getKey())}));
        }

        private static SubSecond getHandler(String expected) {
            return SubSecond.values()[expected.length() - 6];
        }
    }

    @DataProvider(name = "hash-tests")
    public Iterator<Object[]> getHashTestCases() {
        return EnumSet.allOf(TestCase.class).stream()
                .flatMap(TestCase::getHashParameters).iterator();
    }

    @Test(dataProvider = "hash-tests")
    public void testHash(LocalDateTime input, SubSecond handler, String expected) {
        if (handler == null) {
            assertThat(TimeHashUtils.hash(input), equalTo(expected));
        } else {
            assertThat(TimeHashUtils.hash(input, handler), equalTo(expected));
        }
    }

    @DataProvider(name = "unhash-tests")
    public Iterator<Object[]> getUnhashTestCases() {
        return EnumSet.allOf(TestCase.class).stream()
                .flatMap(TestCase::getUnhashParameters).iterator();
    }

    @Test(dataProvider = "unhash-tests")
    public void testUnhash(String input, SubSecond handler, LocalDateTime expected) {
        if (handler == null) {
            assertThat(TimeHashUtils.unhash(input), equalTo(expected));
        } else {
            assertThat(TimeHashUtils.unhash(input, handler), equalTo(expected));
        }
    }

    @DataProvider(name = "year-validation-exception-tests")
    public Iterator<Object[]> getYearExceptionCases() {
        return IntStream.of(TimeHashUtils.YEAR_EPOCH - 1, 2062)
                .mapToObj(i -> new Object[]{LocalDateTime.of(i, 1, 1, 0, 0)})
                .iterator();
    }

    @Test(dataProvider = "year-validation-exception-tests",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "^Year (before 2014|after 2061)$")
    public void invalidYearThrows(LocalDateTime input) {
        TimeHashUtils.hash(input);
    }

    @DataProvider(name = "validation-exception-tests")
    public Iterator<Object[]> getValidationExceptionCases() {
        return Stream.concat(
                Stream.of(null, "").map(v -> new Object[]{v, SubSecond.TRIM}),
                EnumSet.of(TestCase.MIN).stream()
                        .flatMap(TestCase::getValidationExceptionParameters))
                .iterator();
    }

    @Test(dataProvider = "validation-exception-tests",
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "^(Does|Subsecond does) not match pattern: .*")
    public void invalidUnhashLengthThrows(String input, SubSecond handler) {
        TimeHashUtils.unhash(input, handler);
    }

    @Test
    public void testClockHash() {
        LocalDateTime value = LocalDateTime.of(TimeHashUtils.YEAR_MAX, 12, 31, 23, 59, 59);
        Clock clock = Clock.fixed(value.withNano(999_999_999).toInstant(UTC), UTC);
        assertThat(TimeHashUtils.hash(clock, SubSecond.TRIM), equalTo("zJgnWz"));
    }

    @Test
    public void testUtcNowMillis() {
        Clock clock = Clock.systemUTC();
        String potentiallyBefore = TimeHashUtils.hashMillis(clock);
        String utcNowMillis = TimeHashUtils.utcNowMillis();
        String potentiallyAfter = TimeHashUtils.hashMillis(clock);
        assertThat(potentiallyBefore.compareTo(utcNowMillis), lessThan(1));
        assertThat(potentiallyAfter.compareTo(utcNowMillis), not(equalTo(-1)));
    }

}