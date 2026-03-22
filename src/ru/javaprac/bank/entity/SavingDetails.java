package ru.javaprac.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "saving_account")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("saving")
public class SavingDetails extends Account {

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "max_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal maxLimit;

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public void setMaxLimit(BigDecimal maxLimit) { this.maxLimit = maxLimit; }
}
