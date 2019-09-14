package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.google.common.testing.NullPointerTester;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderSnapshot;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderSnapshot_IdTest {
    @Test
    void createsInstanceOfOrderSnapshotId_whenPassedStringIdIsValid() {
        var id = OrderSnapshot.Id.of("ORDER-ORDER-S-29ef61dc-651c-4b87");

        assertThat(id).isNotNull();
    }

    @Test
    void throwsIllegalArgumentException_whenPassedStringIdDoenstContainValidDomainName() {
        var actualException = catchThrowable(() -> OrderSnapshot.Id.of("RESTAURANT-ORDER-S-29ef61dc-651c-4b87"));

        assertThat(actualException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsIllegalArgumentException_whenPassedStringIdDoenstContainValidDomainObjectName() {
        var actualException = catchThrowable(() -> OrderSnapshot.Id.of("ORDER-MENU_ITEM-S-29ef61dc-651c-4b87"));

        assertThat(actualException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsIllegalArgumentException_whenPassedStringIdDoenstContainValidDomainObjectNameType() {
        var actualException = catchThrowable(() -> OrderSnapshot.Id.of("ORDER-ORDER-A-29ef61dc-651c-4b87"));

        assertThat(actualException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsIllegalArgumentException_whenPassedStringIdDoenstContainValidUuidPart() {
        var actualException = catchThrowable(() -> OrderSnapshot.Id.of("ORDER-ORDER-S-29ef61dc651c-4b87"));

        assertThat(actualException).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester().testAllPublicStaticMethods(OrderSnapshot.Id.class);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(OrderSnapshot.Id.class).verify();
    }
}