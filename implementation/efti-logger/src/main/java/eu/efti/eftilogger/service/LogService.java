package eu.efti.eftilogger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface LogService<T> {

    Logger logger = LoggerFactory.getLogger(LogService.class);

    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    void log(final T data);
}
