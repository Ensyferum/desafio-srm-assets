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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Recebível adquirido pela mesa (RF02/RF03). O campo {@code version} garante optimistic locking:
 * liquidações concorrentes geram conflito 409.
 */
@Entity
@Table(name = "receivable", schema = "credit")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cedente_id", nullable = false)
    private UUID cedenteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private ReceivableType type;

    @Column(name = "face_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal faceValue;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "currency_id", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceivableStatus status = ReceivableStatus.PENDING;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Receivable() {}

    public Receivable(
            UUID cedenteId,
            ReceivableType type,
            BigDecimal faceValue,
            LocalDate dueDate,
            String currency) {
        this.cedenteId = cedenteId;
        this.type = type;
        this.faceValue = faceValue;
        this.dueDate = dueDate;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCedenteId() {
        return cedenteId;
    }

    public ReceivableType getType() {
        return type;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getCurrency() {
        return currency;
    }

    public ReceivableStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markSettled() {
        this.status = ReceivableStatus.SETTLED;
        this.updatedAt = Instant.now();
    }
}
