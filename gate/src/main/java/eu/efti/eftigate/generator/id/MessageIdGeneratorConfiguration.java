package eu.efti.eftigate.generator.id;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageIdGeneratorConfiguration {

    @Bean("eftiUUIDGenerator")
    public NoArgGenerator createUUIDGenerator() {
        final EthernetAddress ethernetAddress = EthernetAddress.fromInterface();
        return Generators.timeBasedGenerator(ethernetAddress);
    }
}
