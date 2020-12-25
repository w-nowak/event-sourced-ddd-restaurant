package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

public interface Converter<S, T> {
    T convert(S source) ;
    boolean canConvert(Object sourceCandidate);
}
