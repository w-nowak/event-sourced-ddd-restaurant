package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AbstractAggregateTest {

    @Test
    void createsAggregateWithInitialEvent() {
        TestAggregate aggregate = new TestAggregate(AGGREGATE_INIT_EVENT);

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(Version.NONE);
        assertThat(aggregate.getChanges()).containsExactly(AGGREGATE_INIT_EVENT);
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isNull();
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(AGGREGATE_INIT_EVENT);
    }

    @Test
    void recreatesAggregateFromEvents() {
        TestAggregate aggregate = new TestAggregate(
                List.of(AGGREGATE_INIT_EVENT, AGGREGATE_SAMPLE_EVENT), AGGREGATE_INIT_EVENT.getClass(), AGGREGATE_VERSION_2
        );

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isNull();
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(AGGREGATE_INIT_EVENT, AGGREGATE_SAMPLE_EVENT);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenAggregateVersionIsNone() {
        var caughtException = catchThrowable(
                () -> new TestAggregate(List.of(AGGREGATE_INIT_EVENT), AGGREGATE_INIT_EVENT.getClass(), Version.NONE));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenEmptyCollectionOfEventsIsPassed() {
        var caughtException = catchThrowable(
                () -> new TestAggregate(Collections.emptyList(), AGGREGATE_INIT_EVENT.getClass(), AGGREGATE_VERSION_1));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenFirstOfPassedEventsIsDifferentThanInitEventClass() {
        var caughtException = catchThrowable(
                () -> new TestAggregate(List.of(AGGREGATE_SAMPLE_EVENT), AGGREGATE_INIT_EVENT.getClass(), AGGREGATE_VERSION_1));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatesAggregateFromSnapshotAndEvents() {
        TestAggregate aggregate = new TestAggregate(TestSnapshot.DEFAULT, List.of(AGGREGATE_SAMPLE_EVENT), AGGREGATE_VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(TestSnapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(AGGREGATE_SAMPLE_EVENT);
    }

    @Test
    void recreatesAggregateFromSnapshotAndEmptyCollectionOfEvents() {
        TestAggregate aggregate = new TestAggregate(TestSnapshot.DEFAULT, Collections.emptyList(), AGGREGATE_VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(TestSnapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).isEmpty();
    }

    @Test
    void recreatingAggregateFromSnapshotAndEvents_throwsIllegalArgumentException_whenAggregateVersionIsNone() {
        var caughtException = catchThrowable(
                () -> new TestAggregate(TestSnapshot.DEFAULT, List.of(AGGREGATE_SAMPLE_EVENT), Version.NONE));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(Aggregate.Version.class, AGGREGATE_VERSION_1)
                .setDefault(Collection.class, List.of(AGGREGATE_INIT_EVENT))
                .testAllPublicConstructors(TestAggregate.class);
    }

    static class TestAggregate extends AbstractAggregate<TestAggregateId, TestEvent, TestSnapshot> {
        private TestSnapshot snapshotUsedToRestoreAggregate;
        private Collection<? extends TestEvent> eventsAppliedToAggregate;

        public TestAggregate(TestEvent creatingEvent) {
            super(creatingEvent);
        }

        public TestAggregate(Collection<? extends TestEvent> events, Class<? extends TestEvent> creatingEventClass, Version version) {
            super(events, creatingEventClass, version);
        }

        public TestAggregate(TestSnapshot snapshot, Collection<? extends TestEvent> events, Version version) {
            super(snapshot, events, version);
        }

        @Override
        protected void applyAll(Collection<TestEvent> events) {
            eventsAppliedToAggregate = events;
        }

        @Override
        protected void restoreFrom(TestSnapshot snapshot) {
            snapshotUsedToRestoreAggregate = snapshot;
        }

        public TestSnapshot getSnapshotUsedToRestoreAggregate() {
            return snapshotUsedToRestoreAggregate;
        }

        @SuppressWarnings("unchecked")
        public Collection<TestEvent> getEventsAppliedToAggregate() {
            return (Collection<TestEvent>) eventsAppliedToAggregate;
        }
    }
}