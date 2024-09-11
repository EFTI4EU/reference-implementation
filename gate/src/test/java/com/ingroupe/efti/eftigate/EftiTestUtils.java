package com.ingroupe.efti.eftigate;

import java.io.InputStream;
import java.util.List;

import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.commons.lang3.StringUtils.join;

public class EftiTestUtils {
    public static String testFile(final String fileName) {
        return testFile(fileName, "");
    }

    public static String testFile(final String fileName, final String separator) {
        return join(testFileLines(fileName), separator);
    }

    public static List<String> testFileLines(final String fileName) {
        final InputStream resource = testFileResource(fileName);
        return readLines(resource, "UTF-8");
    }

    private static InputStream testFileResource(final String filePath) {
        final InputStream resource = EftiTestUtils.class.getResourceAsStream(filePath);
        if (resource == null) {
            throw new RuntimeException(filePath + " not found");
        }
        return resource;
    }
}
