package eu.efti.eftigate.utils;

import eu.efti.v1.edelivery.SubsetIds;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@UtilityClass
@Log4j2
public class SubsetsCheckerUtils {

    public static final String FULL = "full";

    public static boolean isSubsetsValid(final List<String> subsets) {
        if (subsets.size() == 1 && FULL.equals(subsets.get(0))) {
            return true;
        }
        try {
            return subsets.stream().allMatch(subset -> subset.equals(SubsetIds.fromValue(subset).value()));
        } catch (IllegalArgumentException e) {
            log.error("Error bad subsets", e);
            return false;
        }
    }
}
