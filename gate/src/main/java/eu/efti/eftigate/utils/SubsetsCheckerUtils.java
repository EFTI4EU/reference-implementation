package eu.efti.eftigate.utils;

import eu.efti.v1.edelivery.SubsetIds;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class SubsetsCheckerUtils {

    public static final String FULL = "full";

    public static boolean isSubsetsValid(final List<String> subsets) {
        if (subsets.size() == 1 && FULL.equals(subsets.get(0))) {
            return true;
        }
        try {
            return subsets.stream().allMatch(subset -> subset.equals(SubsetIds.fromValue(subset).value()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
