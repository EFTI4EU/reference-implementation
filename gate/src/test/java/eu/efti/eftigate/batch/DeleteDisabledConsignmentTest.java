package eu.efti.eftigate.batch;

import eu.efti.identifiersregistry.service.IdentifiersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteDisabledConsignmentTest {

    @Mock
    private IdentifiersService identifiersService;

    private DeleteDisabledConsignment deleteDisabledConsignment;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        deleteDisabledConsignment = new DeleteDisabledConsignment(identifiersService, jdbcTemplate, true);
        ReflectionTestUtils.setField(deleteDisabledConsignment, "deleteUilActivated", true);
    }

    @Test
    void deleteOldConsignmentTest() {
        when(identifiersService.deleteOldConsignment()).thenReturn(1000);
        doNothing().when(jdbcTemplate).execute(anyString());
        deleteDisabledConsignment.deleteOldConsignment();
        verify(identifiersService, times(1)).deleteOldConsignment();
    }

    @Test
    void deleteOldConsignmentFalseTest() {
        ReflectionTestUtils.setField(deleteDisabledConsignment, "deleteUilActivated", false);

        deleteDisabledConsignment.deleteOldConsignment();

        verify(identifiersService, times(0)).deleteOldConsignment();
    }
}
