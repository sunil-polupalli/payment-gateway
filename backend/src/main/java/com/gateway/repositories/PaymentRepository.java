package com.gateway.repositories;

import com.gateway.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    // New method to count payments by status (e.g., "success", "failed", "pending")
    long countByStatus(String status);
}