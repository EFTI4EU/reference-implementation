package eu.efti.eftigate.generator.id;

import com.fasterxml.uuid.NoArgGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service("messageIdGenerator")
@RequiredArgsConstructor
public class MessageIdGenerator {
    private static final String MESSAGE_ID_SUFFIX = "domibus.eu";

    private final NoArgGenerator eftiUUIDGenerator;

    public String generateMessageId() {
        return eftiUUIDGenerator.generate() + "@" + MESSAGE_ID_SUFFIX;
    }
}
