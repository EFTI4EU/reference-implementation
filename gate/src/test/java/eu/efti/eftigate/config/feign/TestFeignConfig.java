package eu.efti.eftigate.config.feign;

import eu.efti.eftigate.feign.PlatformClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@TestConfiguration
@EnableFeignClients(clients = PlatformClient.class)
public class TestFeignConfig {
}
