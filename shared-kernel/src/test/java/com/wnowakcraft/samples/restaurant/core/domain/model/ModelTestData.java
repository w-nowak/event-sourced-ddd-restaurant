package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import static java.util.Collections.emptyList;

public class ModelTestData {
    public static final Aggregate AGGREGATE = Aggregate.ofVersion(Aggregate.VERSION_1);

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
        public static final Snapshot DEFAULT = new Snapshot(Snapshot.Id.DEFAULT, AggregateId.DEFAULT_ONE, INSTANT_NOW, Aggregate.VERSION_1);

        private final Id snapshotId;
        private final AggregateId aggregateId;
        private final Instant creationDate;
        private final Version aggregateVersion;

        public static Snapshot ofVersion(Version version) {
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
        public Version getAggregateVersion() {
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
        public static final InitEvent INIT_EVENT = new InitEvent();
        public static final SampleEvent SAMPLE_EVENT = new SampleEvent();
        public static final Version VERSION_1 = Version.of(1);
        public static final Version VERSION_2 = Version.of(2);

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

        public static Aggregate ofVersion(Aggregate.Version version) {
            return new Aggregate(
                    List.of(INIT_EVENT, SAMPLE_EVENT),
                    INIT_EVENT.getClass(),
                    version
            );
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

    @Getter
    @RequiredArgsConstructor
    public static class EventStream implements EventStore.EventStream<Event> {
        private static final EventStream EMPTY = new EventStream(emptyList(), Version.NONE);
        private final Collection<Event> events;
        private final Version version;

        private static EventStream empty() {
            return EMPTY;
        }
    }
}
