package eu.efti.eftigate.batch;

import eu.efti.eftigate.service.AbstractServiceTest;
import eu.efti.eftigate.service.ControlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ControlBatchTest extends AbstractServiceTest {
    @Mock
    private ControlService controlService;

    @Test
    void work_success() {
        final ControlBatch controlBatch = new ControlBatch(controlService);
        controlBatch.updatePendingControls();
        verify(controlService, times(1)).updatePendingControls();
    }
}
