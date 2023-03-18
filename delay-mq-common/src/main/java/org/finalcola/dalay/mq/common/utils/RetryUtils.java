package org.finalcola.dalay.mq.common.utils;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedRunnable;

/**
 * @author: finalcola
 * @date: 2023/3/19 00:20
 */
public class RetryUtils {

    public static void retry(int maxTime, CheckedRunnable runnable) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxTime)
                .failAfterMaxAttempts(true)
                .build();
        Retry retry = Retry.of("common", retryConfig);
        Retry.decorateCheckedRunnable(retry, runnable)
                .unchecked()
                .run();
    }
}
