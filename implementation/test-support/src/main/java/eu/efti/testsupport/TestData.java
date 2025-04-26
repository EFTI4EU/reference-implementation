package eu.efti.testsupport;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Utility functions for creating reproducible pseudo-random test data. Before each test, reset the generator
 * with {@link #resetSeed()} to make test cases reproducible.
 */
public class TestData {
    /**
     * Random seed used for random generator. Replace with a constant value if you need to debug tests that break when
     * a specific seed value is used.
     */
    private static final long defaultSeed = LocalDate.now().getDayOfMonth() % 4;

    private static final Random random = new Random();

    private static final char[] ALPHANUMERICAL_CHARS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    public static long resetSeed() {
        random.setSeed(defaultSeed);
        return defaultSeed;
    }

    public static <T extends Enum<T>> T random(Class<T> enumClass) {
        var choices = enumClass.getEnumConstants();
        return randomChoice(choices);
    }

    @SafeVarargs
    public static <T> T random(T... choices) {
        return randomChoice(choices);
    }

    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    public static Instant randomFutureInstant() {
        return Instant.now().truncatedTo(ChronoUnit.DAYS).plus(randomLong(30), ChronoUnit.DAYS).plus(randomLong(23), ChronoUnit.HOURS);
    }

    public static String randomIdentifier() {
        return RandomStringUtils.random(4, 0, ALPHANUMERICAL_CHARS.length, true, true, ALPHANUMERICAL_CHARS, random);
    }

    public static long randomLong(long endExclusive) {
        return random.nextLong(0, endExclusive);
    }

    public static Instant randomPastInstant() {
        return Instant.now().truncatedTo(ChronoUnit.DAYS).plus(-randomLong(30), ChronoUnit.DAYS).plus(-randomLong(23), ChronoUnit.HOURS);
    }

    private static <T> T randomChoice(T[] choices) {
        return choices[random.nextInt(0, choices.length - 1)];
    }
}
