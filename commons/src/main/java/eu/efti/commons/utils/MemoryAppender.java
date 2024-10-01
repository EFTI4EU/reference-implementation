package eu.efti.commons.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MemoryAppender extends ListAppender<ILoggingEvent> {
    public MemoryAppender() {
        //empty to allow instantiation of empty object
    }

    public static MemoryAppender createInitializedMemoryAppender(final Level logLevel, final Logger... loggers) {
        if (!Objects.isNull(loggers) && loggers.length != 0) {
            MemoryAppender memoryAppender = new MemoryAppender();
            memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());

            for (Logger logger : loggers) {
                logger.setLevel(logLevel);
                logger.addAppender(memoryAppender);
            }
            memoryAppender.start();
            return memoryAppender;
        } else {
            throw new IllegalArgumentException("At least one logger must be provided");
        }
    }

    public int countEventsForLogger(final String loggerName, final Level level) {
        return (int) this.list.stream()
                .filter(event -> event.getLoggerName().contains(loggerName) && event.getLevel().equals(level))
                .count();
    }

    public boolean containsFormattedLogMessage(final String message) {
        return this.list.stream().anyMatch(event -> event.getFormattedMessage().contains(message));
    }

    public void reset() {
        this.list.clear();
    }

    public static void shutdownMemoryAppender(final MemoryAppender memoryAppender, final Logger... loggers) {
        memoryAppender.reset();
        memoryAppender.stop();
        if (!Objects.isNull(loggers)) {
            for (Logger logger : loggers) {
                logger.detachAppender(memoryAppender);
            }
        }
    }
}
