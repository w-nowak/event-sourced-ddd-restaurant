package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import java.util.function.DoubleUnaryOperator;

public class RecordBinarySearchStrategyFactory implements RecordSearchStrategyFactory {
    @Override
    public SearchStrategy getSearchStrategyFor(long rangeStart, long rangeEnd) {
        return new BinarySearchStrategy(rangeStart, rangeEnd, Math::floor);
    }

    private static class BinarySearchStrategy implements SearchStrategy {
        private final long rangeStart;
        private final long rangeEnd;
        private final long rangeMiddle;

        private BinarySearchStrategy(long rangeStart, long rangeEnd, DoubleUnaryOperator rounding) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.rangeMiddle = middleOf(rangeStart, rangeEnd, rounding);
        }

        @Override
        public SearchStrategy searchLower() {
            return new BinarySearchStrategy(rangeStart, rangeMiddle, Math::floor);
        }

        @Override
        public SearchStrategy searchUpper() {
            return new BinarySearchStrategy(rangeMiddle, rangeEnd, Math::ceil);
        }

        @Override
        public long getNextOffsetToTry() {
            return rangeMiddle == rangeEnd ? NO_FURTHER_OFFSET_AVAILABLE : rangeMiddle;
        }

        private static long middleOf(long rangeStart, long rangeEnd, DoubleUnaryOperator rounding) {
            return (long) rounding.applyAsDouble(rangeStart + (rangeEnd - rangeStart) / 2.0);
        }
    }
}
