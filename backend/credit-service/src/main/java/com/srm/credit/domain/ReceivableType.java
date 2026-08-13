package com.srm.credit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Tipo de recebível com spread mensal configurável (RF02). */
@Entity
@Table(name = "receivable_type", schema = "credit")
public class ReceivableType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "spread_monthly", nullable = false, precision = 10, scale = 6)
    private BigDecimal spreadMonthly;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ReceivableType() {}

    public ReceivableType(String name, BigDecimal spreadMonthly, String description) {
        this.name = name;
        this.spreadMonthly = spreadMonthly;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSpreadMonthly() {
        return spreadMonthly;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
