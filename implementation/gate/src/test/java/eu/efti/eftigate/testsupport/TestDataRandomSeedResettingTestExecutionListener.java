package eu.efti.eftigate.testsupport;

import eu.efti.testsupport.TestData;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

@Slf4j
public class TestDataRandomSeedResettingTestExecutionListener implements TestExecutionListener {
    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (TestDescriptor.Type.TEST.equals(testIdentifier.getType())) {
            var seed = TestData.resetSeed();
            log.trace("Reset random seed to {}", seed);
        }
    }
}
