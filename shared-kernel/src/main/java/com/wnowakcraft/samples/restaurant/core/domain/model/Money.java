package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.Value;

import java.math.BigDecimal;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireThat;

@Value
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    public Money(double amount) {
        requireThatAmountIsNonNegative(amount);

        this.amount = BigDecimal.valueOf(amount);
    }

    public Money(BigDecimal amount) {
        requireNonNull(amount, "amount");
        requireThatAmountIsNonNegative(amount.floatValue());

        this.amount = amount;
    }

    public Money add(Money money) {
        return new Money(this.amount.add(money.amount));
    }

    public Money subtract(Money money) {
        return new Money(this.amount.subtract(money.amount));
    }

    public Money multiplyBy(double value) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(value)));
    }

    public Money divideBy(double value) {
        return new Money(this.amount.divide(BigDecimal.valueOf(value)));
    }

    public void requireThatAmountIsNonNegative(double amount) {
        requireThat(amount >= 0, "amount cannot be negative value");
    }
}
