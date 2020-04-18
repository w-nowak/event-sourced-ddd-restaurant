package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;
import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static java.lang.String.format;

@ToString
@EqualsAndHashCode
public abstract class DomainBoundBusinessId implements Id<String> {
    private static final String SEPARATOR = "-";
    private static final String FIRST_THREE_UUUID_GROUPS_REGEX = "\\w{8}" + SEPARATOR + "\\w{4}" + SEPARATOR + "\\w{4}";
    public static final String STRING_ID_REGEX = "\\w+" + SEPARATOR + "\\w+" + SEPARATOR + "[A-Z]" + SEPARATOR + FIRST_THREE_UUUID_GROUPS_REGEX;

    private final String idValue;
    public final String domainName;
    public final String domainObjectName;

    protected DomainBoundBusinessId(String domainName, String domainObjectName, char domainObjectType) {
        this.domainName = requireNonEmpty(domainName, "domainName");
        this.domainObjectName = requireNonEmpty(domainObjectName, "domainObjectName");

        idValue = idPrefixOf(domainName, domainObjectName, domainObjectType)
                + SEPARATOR
                + threeMostSignificantComponentsOf(UUID.randomUUID());
    }

    protected DomainBoundBusinessId(String domainObjectId, String domainName, String domainObjectName, char domainObjectType) {
        requireNonEmpty(domainObjectId, "domainObjectId");
        this.domainName = requireNonEmpty(domainName, "domainName");
        this.domainObjectName = requireNonEmpty(domainObjectName, "domainObjectName");
        verifyAggregateIdCorrectness(domainObjectId, domainName, domainObjectName, domainObjectType);

        idValue = domainObjectId;
    }

    @Override
    public String getValue() {
        return idValue;
    }

    private static void verifyAggregateIdCorrectness(String domainObjectId, String domainName, String domainObjectName,
                                                     char domainObjectType) {
        requireThat(
                domainObjectId.matches(
                        idPrefixOf(domainName, domainObjectName, domainObjectType) + SEPARATOR + FIRST_THREE_UUUID_GROUPS_REGEX),
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
