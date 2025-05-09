package com.axis.pay.model.repository

import com.axis.pay.model.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID>