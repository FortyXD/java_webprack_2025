package ru.javaprac.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "credit")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("credit")
public class CreditDetails extends Account {

    @Column(name = "max_credit", nullable = false, precision = 14, scale = 2)
    private BigDecimal maxCredit;

    @Column(name = "current_dept", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentDebt;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    public BigDecimal getMaxCredit() { return maxCredit; }
    public void setMaxCredit(BigDecimal maxCredit) { this.maxCredit = maxCredit; }
    public BigDecimal getCurrentDebt() { return currentDebt; }
    public void setCurrentDebt(BigDecimal currentDebt) { this.currentDebt = currentDebt; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
