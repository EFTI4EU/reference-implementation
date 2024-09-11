package com.ingroupe.efti.eftigate.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class EftiAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(final Throwable ex, final Method method, final Object... params) {
        log.error("[ASYNC-ERROR] method: "+method.getName()+" ,exception: " + ex);
    }
}
