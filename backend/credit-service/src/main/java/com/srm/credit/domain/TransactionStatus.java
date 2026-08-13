package com.srm.credit.domain;

/** Status da transação de liquidação. */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
