package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import org.apache.kafka.clients.producer.Callback;

public class RecordAppendingOffsetTrackerHandler {
    private static final Callback NO_COMPOSED_HANDLER = (metadata, exception) -> { };
    private long offsetOfLatestAppendedRecord;

    public Callback getHandler() {
        return getHandlerComposedWith(NO_COMPOSED_HANDLER);
    }

    public Callback getHandlerComposedWith(Callback composedHandler) {
        return (recordMetadata, exception) -> {
            offsetOfLatestAppendedRecord = recordMetadata.offset();
            composedHandler.onCompletion(recordMetadata, exception);
        };
    }

    long getOffsetOfLatestAppendedRecord() {
        return offsetOfLatestAppendedRecord;
    }
}
