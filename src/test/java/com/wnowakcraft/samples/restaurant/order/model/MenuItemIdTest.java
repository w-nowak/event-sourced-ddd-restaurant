package com.wnowakcraft.samples.restaurant.order.model;

import com.google.common.testing.NullPointerTester;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.catchThrowable;

class MenuItemIdTest {
    @Test
    void recreatesCorrectInstanceOfRestaurantId() {
        var idString = "RESTAURANT-MENU_ITEM-A-3df56c04-0bf9-4caa";

        var menuItemId = MenuItemId.of(idString);

        assertThat(menuItemId.getId()).isEqualTo(idString);
    }

    @Test
    void throwsException_whenDomainNameIsInvalid_forMenuItemId() {
        var idWithInvalidDomainName = "ORDER-MENU_ITEM-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> MenuItemId.of(idWithInvalidDomainName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenAggregateNameNameIsInvalid_forMenuItemId() {
        var idWithInvalidAggregateName = "RESTAURANT-CUSTOMER-A-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> MenuItemId.of(idWithInvalidAggregateName));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenDomainObjectTypeIsInvalid_forMenuItemId() {
        var idWithInvalidDomainObjectType = "RESTAURANT-MENU_ITEM-X-3df56c04-0bf9-4caa";

        var exception = catchThrowable(() -> MenuItemId.of(idWithInvalidDomainObjectType));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsException_whenUuidPartIsInvalid_forMenuItemId() {
        var idWithInvalidUuidPart = "RESTAURANT-MENU_ITEM-A-3df56c040bf9-4caa";

        var exception = catchThrowable(() -> MenuItemId.of(idWithInvalidUuidPart));

        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiesNullPointerContractOfPublicStaticMethods() {
        new NullPointerTester().testAllPublicStaticMethods(MenuItemId.class);
    }

    @Test
    void verifiesEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(MenuItemId.class).verify();
    }
}