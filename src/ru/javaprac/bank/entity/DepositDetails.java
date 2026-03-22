package ru.javaprac.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "deposit")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("deposit")
public class DepositDetails extends Account {

    @Column(name = "initial_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialAmount;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "automatic_renewal", nullable = false)
    private boolean automaticRenewal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    public BigDecimal getInitialAmount() { return initialAmount; }
    public void setInitialAmount(BigDecimal initialAmount) { this.initialAmount = initialAmount; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isAutomaticRenewal() { return automaticRenewal; }
    public void setAutomaticRenewal(boolean automaticRenewal) { this.automaticRenewal = automaticRenewal; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
