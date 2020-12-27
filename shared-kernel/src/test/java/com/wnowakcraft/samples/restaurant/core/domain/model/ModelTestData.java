package com.wnowakcraft.samples.restaurant.core.domain.model;

public class ModelTestData {
    public static final TestInitEvent AGGREGATE_INIT_EVENT = new TestInitEvent();
    public static final TestSampleEvent AGGREGATE_SAMPLE_EVENT = new TestSampleEvent();
    public static final Aggregate.Version AGGREGATE_VERSION_1 = Aggregate.Version.of(1);
    public static final Aggregate.Version AGGREGATE_VERSION_2 = Aggregate.Version.of(2);

    public static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    public static class TestSampleEvent extends BaseTestEvent {
        TestSampleEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER.next(), BaseTestEvent.GENERATED_ON.plusSeconds(60*60));
        }
    }
}
