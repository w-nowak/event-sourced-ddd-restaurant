package com.wnowakcraft.samples.restaurant.core.domain.model.snapshot;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.AggregateId;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.ModelTestData.Snapshot;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class TakeSnapshotEveryVersionThresholdStrategyTest {
    private final int VERSION_THRESHOLD = 10;
    private final TakeSnapshotEveryVersionThresholdStrategy<Event, Aggregate, Snapshot, AggregateId> takeSnapshotStrategy =
            new TakeSnapshotEveryVersionThresholdStrategy<>(VERSION_THRESHOLD);

    @Test
    void shouldReturnTrue_whenAggregateVersionIsHigherByThresholdThanLastVersionFromSnapshot() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(25));
        var snapshot = Snapshot.ofVersion(Aggregate.Version.of(15));

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, providerOf(snapshot));

        assertThat(shouldTakeSnapshot).isTrue();
    }

    private Function<Aggregate, Optional<Snapshot>> providerOf(Snapshot snapshot) {
        return a -> Optional.of(snapshot);
    }

    @Test
    void shouldReturnTrue_whenAggregateVersionIsHigherByMoreThanThresholdThanLastVersionFromSnapshot() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(26));
        var snapshot = Snapshot.ofVersion(Aggregate.Version.of(15));

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, providerOf(snapshot));

        assertThat(shouldTakeSnapshot).isTrue();
    }

    @Test
    void shouldReturnFalse_whenAggregateVersionIsNotHigherByThresholdThanLastVersionFromSnapshot() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(24));
        var snapshot = Snapshot.ofVersion(Aggregate.Version.of(15));

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, providerOf(snapshot));

        assertThat(shouldTakeSnapshot).isFalse();
    }

    @Test
    void shouldReturnTrue_whenAggregateVersionIsHigherThanThreshold_andNoSnapshotWasYetTaken() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(11));
        Snapshot noSnapshot = null;

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, noSnapshotProvider());

        assertThat(shouldTakeSnapshot).isTrue();
    }

    private Function<Aggregate, Optional<Snapshot>> noSnapshotProvider() {
        return a -> Optional.empty();
    }

    @Test
    void shouldReturnTrue_whenAggregateVersionIsEqualThreshold_andNoSnapshotWasYetTaken() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(10));

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, noSnapshotProvider());

        assertThat(shouldTakeSnapshot).isTrue();
    }

    @Test
    void shouldReturnFalse_whenAggregateVersionIsLessThanThreshold_andNoSnapshotWasYetTaken() {
        var aggregate = Aggregate.ofVersion(Aggregate.Version.of(9));

        var shouldTakeSnapshot = takeSnapshotStrategy.shouldTakeNewSnapshot(aggregate, noSnapshotProvider());

        assertThat(shouldTakeSnapshot).isFalse();
    }

    @Test
    void verifiesNullPointerContractOfPublicInstanceMethods() {
        new NullPointerTester().testAllPublicInstanceMethods(takeSnapshotStrategy);
    }
}