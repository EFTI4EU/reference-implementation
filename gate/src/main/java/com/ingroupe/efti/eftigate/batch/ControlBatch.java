package com.ingroupe.efti.eftigate.batch;

import com.ingroupe.efti.eftigate.service.ControlService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class ControlBatch {
    ControlService controlService;
    @Scheduled(cron = "${batch.update.cron}")
    @SchedulerLock(name = "TaskScheduler_scheduledTask",
            lockAtLeastFor = "PT19S", lockAtMostFor = "PT19S")
    public void updatePendingControls() {
        log.info("Batch of updating control started");
        final int updatePendingControls = controlService.updatePendingControls();
        log.info("Batch of updating control finished with {} control updated", updatePendingControls);
    }
}
