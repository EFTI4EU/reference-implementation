package eu.efti.eftigate.generator.id;

import com.fasterxml.uuid.NoArgGenerator;
import eu.efti.eftigate.generator.id.MessageIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageIdGeneratorTest {
    @Mock
    private NoArgGenerator noArgGenerator;
    @InjectMocks
    private MessageIdGenerator messageIdGenerator;

    @Test
    void shouldGenerateMessageId() {
        String generatedMessageId = messageIdGenerator.generateMessageId();
        Assertions.assertNotNull(generatedMessageId);
        Assertions.assertTrue(generatedMessageId.endsWith("@domibus.eu"));
    }
}
