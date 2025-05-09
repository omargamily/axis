package com.axis.pay.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "accounts")
data class Account(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var balance: Double = 0.0,

    @OneToMany(mappedBy = "account", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val transactions: List<Transaction> = emptyList(),

    @Column(nullable = false)
    val createdAt: Long = System.currentTimeMillis()
)