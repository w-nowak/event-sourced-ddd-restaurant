package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.catchThrowable;

class RestaurantIdTest {

    @Test
    void recreatesCorrectInstanceOfRestaurantId() {
        var idString = "RESTAURANT-RESTAURANT-A-3df56c04-0bf9-4caa";

        var restaurantId = RestaurantId.of(idString);

        assertThat(restaurantId.getId()).isEqualTo(idString);
    }

    @Test
    void throwsException_whenDomainNameIsInvalid_forRestaurantId() {
        var idWithInvalidDomainName = "PAYMENTS-RESTAURANT-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> RestaurantId.of(idWithInvalidDomainName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenAggrateNameNameIsInvalid_forRestaurantId() {
        var idWithInvalidAggregateName = "RESTAURANT-ORDER-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> RestaurantId.of(idWithInvalidAggregateName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenDomainObjectTypeIsInvalid_forRestaurantId() {
        var idWithInvalidDomainObjectType = "RESTAURANT-RESTAURANT-X-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> RestaurantId.of(idWithInvalidDomainObjectType));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenUuidPartIsInvalid_forRestaurantId() {
        var idWithInvalidUuidPart = "RESTAURANT-RESTAURANT-A-3df56c040bf9-4caa";

        var exception = catchThrowable(() -> RestaurantId.of(idWithInvalidUuidPart));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester().testAllPublicStaticMethods(RestaurantId.class);
    }
}