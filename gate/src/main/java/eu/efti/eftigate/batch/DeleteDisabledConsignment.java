package eu.efti.eftigate.batch;

import eu.efti.identifiersregistry.service.IdentifiersService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class DeleteDisabledConsignment {

    public static final String VACUUM_PARALLEL_2_ANALYSE = "VACUUM (PARALLEL 2, ANALYSE);";

    @Autowired
    private IdentifiersService identifiersService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${batch.identifier.activated:false}")
    private boolean deleteUilActivated;

    @Scheduled(cron = "${batch.identifier.cron}")
    @SchedulerLock(name = "TaskScheduler_deleteOldConsignment",
            lockAtLeastFor = "PT19S", lockAtMostFor = "PT19S")
    public void deleteOldConsignment() {
        if (deleteUilActivated) {
            log.info("Batch of deleting old Consignment just started");
            final int result = identifiersService.deleteOldConsignment();
            log.info("End of batch, delete {} consignment", result);
            log.info("Started vacuum BDD");
            try {
                jdbcTemplate.execute(VACUUM_PARALLEL_2_ANALYSE);
                log.info("Finished vacuum BDD");
            } catch (Exception e) {
                log.error("Error when try to VACUUM database (try to clean database): ", e);
            }
        }
    }
}
