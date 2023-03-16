package org.finalcola.delay.mq.broker.consumer;

/**
 * @author: finalcola
 * @date: 2023/3/15 23:36
 */
public interface Consumer {

    void start();

    void stop();

    void poll();

    void commitOffset(long offset);
}
