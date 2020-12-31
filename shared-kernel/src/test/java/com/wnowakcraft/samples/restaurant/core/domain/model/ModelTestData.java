package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Collection;

public class ModelTestData {
    public static final InitEvent AGGREGATE_INIT_EVENT = new InitEvent();
    public static final SampleEvent AGGREGATE_SAMPLE_EVENT = new SampleEvent();

    public static final com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version AGGREGATE_VERSION_1 = com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version.of(1);
    public static final com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version AGGREGATE_VERSION_2 = com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version.of(2);

    public static class AggregateId extends com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Id {
        public static final String DOMAIN_NAME = "shared-kernel";
        public static final String AGGREGATE_NAME = "testAggregate";
        public static final AggregateId DEFAULT_ONE = new AggregateId(DOMAIN_NAME, AGGREGATE_NAME);

        private AggregateId(String domainName, String domainObjectName) {
            super(domainName, domainObjectName);
        }
    }

    public interface Event extends com.wnowakcraft.samples.restaurant.core.domain.model.Event<AggregateId> { }

    @RequiredArgsConstructor
    public abstract static class BaseEvent implements Event {
        public static final SequenceNumber SEQUENCE_NUMBER = SequenceNumber.of(5);
        public static final Instant GENERATED_ON = Instant.now();

        private final AggregateId aggregateId;
        private final SequenceNumber sequenceNumber;
        private final Instant generatedOn;

        @Override
        public AggregateId getConcernedAggregateId() {
            return aggregateId;
        }

        @Override
        public SequenceNumber getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public Instant getGeneratedOn() {
            return generatedOn;
        }
    }

    public static class InitEvent extends BaseEvent {
        InitEvent() {
            super(AggregateId.DEFAULT_ONE, BaseEvent.SEQUENCE_NUMBER, BaseEvent.GENERATED_ON);
        }
    }

    public static class SampleEvent extends BaseEvent {
        SampleEvent() {
            super(AggregateId.DEFAULT_ONE, BaseEvent.SEQUENCE_NUMBER.next(), BaseEvent.GENERATED_ON.plusSeconds(60*60));
        }
    }

    @RequiredArgsConstructor
    public static class Snapshot implements com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot<Snapshot.Id, AggregateId> {
        private static final Instant INSTANT_NOW = Instant.now();
        public static final Snapshot DEFAULT = new Snapshot(Snapshot.Id.DEFAULT, AggregateId.DEFAULT_ONE, INSTANT_NOW, AGGREGATE_VERSION_1);

        private final Id snapshotId;
        private final AggregateId aggregateId;
        private final Instant creationDate;
        private final com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version aggregateVersion;

        public static Snapshot ofVersion(com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version version) {
            return new Snapshot(Snapshot.Id.any(), AggregateId.DEFAULT_ONE, INSTANT_NOW, version);
        }

        @Override
        public Id getSnapshotId() {
            return snapshotId;
        }

        @Override
        public AggregateId getAggregateId() {
            return aggregateId;
        }

        @Override
        public Instant getCreationDate() {
            return creationDate;
        }

        @Override
        public com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version getAggregateVersion() {
            return aggregateVersion;
        }

        public static class Id extends com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot.Id {
            private static final String TEST_DOMAIN_NAME = "TEST";
            private static final String TEST_DOMAIN_OBJECT_NAME = "TEST";
            public static final String TEST_SNAPSHOT_ID = TEST_DOMAIN_NAME + "-" + TEST_DOMAIN_OBJECT_NAME + "-S-75a03383-694a-4b78";
            public static final Id DEFAULT = new Id(TEST_SNAPSHOT_ID, TEST_DOMAIN_NAME, TEST_DOMAIN_OBJECT_NAME);

            public static Id any() {
                return new Id(TEST_DOMAIN_NAME, TEST_DOMAIN_OBJECT_NAME);
            }

            private Id(String domainName, String domainObjectName) {
                super(domainName, domainObjectName);
            }

            private Id(String snapshotId, String domainName, String domainObjectName) {
                super(snapshotId, domainName, domainObjectName);
            }
        }
    }

    public static class Aggregate extends AbstractAggregate<AggregateId, Event, Snapshot> {
        private Snapshot snapshotUsedToRestoreAggregate;
        private Collection<? extends Event> eventsAppliedToAggregate;

        public Aggregate(Event creatingEvent) {
            super(creatingEvent);
        }

        public Aggregate(Collection<? extends Event> events, Class<? extends Event> creatingEventClass, Version version) {
            super(events, creatingEventClass, version);
        }

        public Aggregate(Snapshot snapshot, Collection<? extends Event> events, Version version) {
            super(snapshot, events, version);
        }

        @Override
        protected void applyAll(Collection<Event> events) {
            eventsAppliedToAggregate = events;
        }

        @Override
        protected void restoreFrom(Snapshot snapshot) {
            snapshotUsedToRestoreAggregate = snapshot;
        }

        public Snapshot getSnapshotUsedToRestoreAggregate() {
            return snapshotUsedToRestoreAggregate;
        }

        @SuppressWarnings("unchecked")
        public Collection<Event> getEventsAppliedToAggregate() {
            return (Collection<Event>) eventsAppliedToAggregate;
        }
    }
}
