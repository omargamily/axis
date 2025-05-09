package com.axis.pay.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val timestamp: Long = System.currentTimeMillis()
)