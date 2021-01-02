package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Aggregate;
import static com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AbstractAggregateTest {

    @Test
    void createsAggregateWithInitialEvent() {
        Aggregate aggregate = new Aggregate(Aggregate.INIT_EVENT);

        assertThat(aggregate.getId()).isEqualTo(Aggregate.INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(Version.NONE);
        assertThat(aggregate.getChanges()).containsExactly(Aggregate.INIT_EVENT);
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isNull();
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(Aggregate.INIT_EVENT);
    }

    @Test
    void recreatesAggregateFromEvents() {
        Aggregate aggregate = new Aggregate(
                List.of(Aggregate.INIT_EVENT, Aggregate.SAMPLE_EVENT), Aggregate.INIT_EVENT.getClass(), Aggregate.VERSION_2
        );

        assertThat(aggregate.getId()).isEqualTo(Aggregate.INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(Aggregate.VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isNull();
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(Aggregate.INIT_EVENT, Aggregate.SAMPLE_EVENT);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenAggregateVersionIsNone() {
        var caughtException = catchThrowable(
                () -> new Aggregate(List.of(Aggregate.INIT_EVENT), Aggregate.INIT_EVENT.getClass(), Version.NONE));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenEmptyCollectionOfEventsIsPassed() {
        var caughtException = catchThrowable(
                () -> new Aggregate(Collections.emptyList(), Aggregate.INIT_EVENT.getClass(), Aggregate.VERSION_1));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatingAggregateFromEvents_throwsIllegalArgumentException_whenFirstOfPassedEventsIsDifferentThanInitEventClass() {
        var caughtException = catchThrowable(
                () -> new Aggregate(List.of(Aggregate.SAMPLE_EVENT), Aggregate.INIT_EVENT.getClass(), Aggregate.VERSION_1));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recreatesAggregateFromSnapshotAndEvents() {
        Aggregate aggregate = new Aggregate(Snapshot.DEFAULT, List.of(Aggregate.SAMPLE_EVENT), Aggregate.VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(Aggregate.INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(Aggregate.VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(Snapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).containsExactly(Aggregate.SAMPLE_EVENT);
    }

    @Test
    void recreatesAggregateFromSnapshotAndEmptyCollectionOfEvents() {
        Aggregate aggregate = new Aggregate(Snapshot.DEFAULT, Collections.emptyList(), Aggregate.VERSION_2);

        assertThat(aggregate.getId()).isEqualTo(Aggregate.INIT_EVENT.getConcernedAggregateId());
        assertThat(aggregate.getVersion()).isEqualTo(Aggregate.VERSION_2);
        assertThat(aggregate.getChanges()).isEmpty();
        assertThat(aggregate.getSnapshotUsedToRestoreAggregate()).isEqualTo(Snapshot.DEFAULT);
        assertThat(aggregate.getEventsAppliedToAggregate()).isEmpty();
    }

    @Test
    void recreatingAggregateFromSnapshotAndEvents_throwsIllegalArgumentException_whenAggregateVersionIsNone() {
        var caughtException = catchThrowable(
                () -> new Aggregate(Snapshot.DEFAULT, List.of(Aggregate.SAMPLE_EVENT), Version.NONE));

        assertThat(caughtException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesAggregateVersion() {
        var aggregate = new Aggregate(Aggregate.INIT_EVENT);
        assertThat(aggregate.getVersion()).isEqualTo(Version.NONE);

        aggregate.updateVersionTo(Aggregate.VERSION_2);

        assertThat(aggregate.getVersion()).isEqualTo(Aggregate.VERSION_2);
    }

    @Test
    void verifiesNullPointerContractOfPublicConstructors() {
        new NullPointerTester()
                .setDefault(Aggregate.Version.class, Aggregate.VERSION_1)
                .setDefault(Collection.class, List.of(Aggregate.INIT_EVENT))
                .testAllPublicConstructors(Aggregate.class);
    }

}