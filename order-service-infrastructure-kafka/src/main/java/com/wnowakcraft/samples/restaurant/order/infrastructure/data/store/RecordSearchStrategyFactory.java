package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

public interface RecordSearchStrategyFactory {
    SearchStrategy getSearchStrategyFor(long offsetRangeStart, long offsetRangeEnd);

    interface SearchStrategy {
        long NO_FURTHER_OFFSET_AVAILABLE = -1;

        SearchStrategy searchLower();
        SearchStrategy searchUpper();
        long getNextOffsetToTry();
    }
}
