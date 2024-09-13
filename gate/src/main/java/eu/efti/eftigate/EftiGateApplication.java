package eu.efti.eftigate;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRabbit
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class EftiGateApplication {
    public static void main(final String[] args) {
        SpringApplication.run(EftiGateApplication.class, args);
    }
}
