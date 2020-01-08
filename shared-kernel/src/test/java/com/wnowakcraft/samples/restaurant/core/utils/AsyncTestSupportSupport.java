package com.wnowakcraft.samples.restaurant.core.utils;

import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static java.lang.String.format;

public class AsyncTestSupportSupport implements AsyncTestWaitSupport {
    private final AsyncTestMonitorWait asyncFlowFinishedMonitor = new AsyncTestMonitorWait();
    private boolean asyncFlowStarted;
    private boolean asyncFlowFinished;

    public void startAsyncFlow() {
        requireStateThat(!asyncFlowFinished, "This async flow has already finished! You cannot reuse it again");
        requireStateThat(!asyncFlowStarted, "This async flow has already started and cannot be started again");

        asyncFlowStarted = true;
    }

    public void finishAsyncFlow() {
        requireStateThat(asyncFlowStarted, "This async flow has already finished and can not be finished again");

        asyncFlowStarted = false;
        asyncFlowFinished = true;
        asyncFlowFinishedMonitor.notifyMonitorConditionIsMet();
    }

    public boolean isAsyncFlowStarted() {
        return asyncFlowStarted;
    }

    @Override
    public void waitUntilAsyncFlowFinished() {
        asyncFlowFinishedMonitor.waitUntilMonitorConditionIsMet();
    }

    private static class AsyncTestMonitorWait {
        private static final int MONITOR_TIMEOUT_MILLIS = 6000;
        private final Object monitorObject = new Object();
        private boolean monitorConditionMet;

        private void notifyMonitorConditionIsMet() {
            synchronized (monitorObject) {
                monitorConditionMet = true;
                monitorObject.notify();
            }
        }

        void waitUntilMonitorConditionIsMet() {
            synchronized (monitorObject) {
                waitUntilMonitorConditionIsMarkedAsMet();
            }
        }

        private void waitUntilMonitorConditionIsMarkedAsMet() {
            while (!monitorConditionMet) {
                try {
                    monitorObject.wait(MONITOR_TIMEOUT_MILLIS);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Main test thread has been interrupted by another thread", ex);
                }

            }
        }
    }
}
