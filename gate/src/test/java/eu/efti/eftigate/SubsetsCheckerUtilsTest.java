package eu.efti.eftigate;

import eu.efti.eftigate.utils.SubsetsCheckerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SubsetsCheckerUtilsTest {

    @Test
    void isSubsetValidTrueTest() {
        List<String> subsets = List.of("EL05");

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertTrue(result);
    }

    @Test
    void isSubsetValidFalseTest() {
        List<String> subsets = List.of("Lens is a good football club");

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertFalse(result);
    }

    @Test
    void isSubsetValidEmptyListTest() {
        List<String> subsets = List.of();

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertTrue(result);
    }

    @Test
    void isSubsetValidMultipleTrueTest() {
        List<String> subsets = List.of("EL05", "EU02");

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertTrue(result);
    }

    @Test
    void isSubsetValidMultipleFalseTest() {
        List<String> subsets = List.of("EL0", "EU===02");

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertFalse(result);
    }

    @Test
    void isSubsetValidFullTest() {
        List<String> subsets = List.of("full");

        boolean result = SubsetsCheckerUtils.isSubsetsValid(subsets);

        Assertions.assertTrue(result);
    }
}
