package org.finalcola.delay.mq.broker;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author: finalcola
 * @date: 2023/3/18 13:14
 */
public class MetaHolder {
    private static final AtomicReferenceFieldUpdater<MetaHolder, String> LAST_HANDLE_TIME_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(MetaHolder.class, String.class, "lastHandledKey");
    private volatile String lastHandledKey = StringUtils.repeat("0", 13);

    public String getLastHandledKey() {
        return lastHandledKey;
    }

    public void setLastHandledKey(String lastHandledKey) {
        LAST_HANDLE_TIME_UPDATER.set(this, lastHandledKey);
    }

    public boolean setLastHandledKey(String oldVal, String newVal) {
        return LAST_HANDLE_TIME_UPDATER.compareAndSet(this, oldVal, newVal);
    }
}
