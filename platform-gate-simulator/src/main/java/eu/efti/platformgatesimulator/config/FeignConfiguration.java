package eu.efti.platformgatesimulator.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"eu.efti.platformgatesimulator"})
public class FeignConfiguration {
}
