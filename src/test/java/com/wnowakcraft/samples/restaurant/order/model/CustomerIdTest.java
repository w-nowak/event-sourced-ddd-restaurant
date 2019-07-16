package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.catchThrowable;

class CustomerIdTest {
    @Test
    void recreatesCorrectInstanceOfRestaurantId() {
        var idString = "CUSTOMER-CUSTOMER-A-3df56c04-0bf9-4caa";

        var customerId = CustomerId.of(idString);

        assertThat(customerId.getId()).isEqualTo(idString);
    }

    @Test
    void throwsException_whenDomainNameIsInvalid_forCustomerId() {
        var idWithInvalidDomainName = "RESTAURANT-CUSTOMER-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> CustomerId.of(idWithInvalidDomainName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenAggregateNameNameIsInvalid_forCustomerId() {
        var idWithInvalidAggregateName = "CUSTOMER-RESTAURANT-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> CustomerId.of(idWithInvalidAggregateName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenDomainObjectTypeIsInvalid_forCustomerId() {
        var idWithInvalidDomainObjectType = "CUSTOMER-CUSTOMER-X-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> CustomerId.of(idWithInvalidDomainObjectType));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenUuidPartIsInvalid_forCustomerId() {
        var idWithInvalidUuidPart = "CUSTOMER-CUSTOMER-A-3df56c040bf9-4caa";

        var exception = catchThrowable(() -> CustomerId.of(idWithInvalidUuidPart));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester().testAllPublicStaticMethods(CustomerId.class);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(CustomerId.class).verify();
    }
}