package com.wnowakcraft.samples.restaurant.core.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;
import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static java.lang.String.format;

@Getter
@ToString
@EqualsAndHashCode
public abstract class DomainBoundBusinessId implements Id<String> {
    private static final String SEPARATOR = "-";
    private final String id;

    protected DomainBoundBusinessId(String domainObjectId, String domainObjectName, char domainObjectType) {
        requireNonEmpty(domainObjectId, "domainObjectId");
        requireNonEmpty(domainObjectName, "domainObjectName");

        id = idPrefixOf(domainObjectId, domainObjectName, domainObjectType)
                + SEPARATOR
                + threeMostSignificantComponentsOf(UUID.randomUUID());
    }

    protected DomainBoundBusinessId(String domainObjectId, String domainName, String domainObjectName, char domainObjectType) {
        requireNonEmpty(domainObjectId, "domainObjectId");
        requireNonEmpty(domainName, "domainName");
        requireNonEmpty(domainObjectName, "domainObjectName");
        verifyAggregateIdCorrectness(domainObjectId, domainName, domainObjectName, domainObjectType);

        id = domainObjectId;
    }

    private static void verifyAggregateIdCorrectness(String domainObjectId, String domainName, String domainObjectName,
                                                     char domainObjectType) {
        requireThat(
                domainObjectId.startsWith(idPrefixOf(domainName, domainObjectName, domainObjectType)),
                format("%s is not valid identifier for %s domain and %s domain object of type %c",
                        domainObjectId, domainName, domainObjectName, domainObjectType)
        );
    }

    private static String threeMostSignificantComponentsOf(UUID uuid) {
        final var endOfThirdUuidGroup = 18;
        return uuid.toString().substring(0, endOfThirdUuidGroup);
    }

    private static String idPrefixOf(String domainName, String domainObjectName, char domainObjectType) {
        return domainName + SEPARATOR + domainObjectName + SEPARATOR + domainObjectType;
    }
}
