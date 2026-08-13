package com.srm.credit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Transação de liquidação de um recebível (RF03 — ACID). */
@Entity
@Table(name = "transaction", schema = "credit")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Column(name = "present_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal presentValue;

    @Column(name = "discount_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "settlement_currency", nullable = false, length = 3)
    private String settlementCurrency;

    @Column(name = "exchange_rate_applied", precision = 20, scale = 10)
    private BigDecimal exchangeRateApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected Transaction() {}

    public Transaction(
            Receivable receivable,
            BigDecimal presentValue,
            BigDecimal discountValue,
            String settlementCurrency,
            BigDecimal exchangeRateApplied,
            String createdBy) {
        this.receivable = receivable;
        this.presentValue = presentValue;
        this.discountValue = discountValue;
        this.settlementCurrency = settlementCurrency;
        this.exchangeRateApplied = exchangeRateApplied;
        this.createdBy = createdBy;
        this.status = TransactionStatus.COMPLETED;
        this.settledAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public BigDecimal getPresentValue() {
        return presentValue;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public BigDecimal getExchangeRateApplied() {
        return exchangeRateApplied;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
