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
        TestAggregate aggregate = new TestAggregate(ModelTestData.Snapshot.DEFAULT, List.of(AGGREGATE_SAMPLE_EVENT), AGGREGATE_VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(ModelTestData.Snapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(AGGREGATE_SAMPLE_EVENT);
    }

    @Test
    void recreatesAggregateFromSnapshotAndEmptyCollectionOfEvents() {
        TestAggregate aggregate = new TestAggregate(ModelTestData.Snapshot.DEFAULT, Collections.emptyList(), AGGREGATE_VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(AGGREGATE_INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(ModelTestData.Snapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).isEmpty();
    }

    @Test
    void recreatingAggregateFromSnapshotAndEvents_throwsIllegalArgumentException_whenAggregateVersionIsNone() {
        var caughtException = catchThrowable(
                () -> new TestAggregate(ModelTestData.Snapshot.DEFAULT, List.of(AGGREGATE_SAMPLE_EVENT), Version.NONE));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesAggregateVersion() {
        var aggregate = new TestAggregate(AGGREGATE_INIT_EVENT);
        assertThat(aggregate.getVersion()).isEqualTo(Version.NONE);

        aggregate.updateVersionTo(AGGREGATE_VERSION_2);

        assertThat(aggregate.getVersion()).isEqualTo(AGGREGATE_VERSION_2);
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(Aggregate.Version.class, AGGREGATE_VERSION_1)
                .setDefault(Collection.class, List.of(AGGREGATE_INIT_EVENT))
                .testAllPublicConstructors(TestAggregate.class);
    }

    static class TestAggregate extends AbstractAggregate<AggregateId, ModelTestData.Event, ModelTestData.Snapshot> {
        private ModelTestData.Snapshot snapshotUsedToRestoreAggregate;
        private Collection<? extends ModelTestData.Event> eventsAppliedToAggregate;

        public TestAggregate(ModelTestData.Event creatingEvent) {
            super(creatingEvent);
        }

        public TestAggregate(Collection<? extends ModelTestData.Event> events, Class<? extends ModelTestData.Event> creatingEventClass, Version version) {
            super(events, creatingEventClass, version);
        }

        public TestAggregate(ModelTestData.Snapshot snapshot, Collection<? extends ModelTestData.Event> events, Version version) {
            super(snapshot, events, version);
        }

        @Override
        protected void applyAll(Collection<ModelTestData.Event> events) {
            eventsAppliedToAggregate = events;
        }

        @Override
        protected void restoreFrom(ModelTestData.Snapshot snapshot) {
            snapshotUsedToRestoreAggregate = snapshot;
        }

        public ModelTestData.Snapshot getSnapshotUsedToRestoreAggregate() {
            return snapshotUsedToRestoreAggregate;
        }

        @SuppressWarnings("unchecked")
        public Collection<ModelTestData.Event> getEventsAppliedToAggregate() {
            return (Collection<ModelTestData.Event>) eventsAppliedToAggregate;
        }
    }
}