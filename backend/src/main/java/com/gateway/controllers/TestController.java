package com.gateway.controllers;

import com.gateway.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin // Fixes the "Offline" status on the Dashboard
public class TestController {

    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private PaymentRepository paymentRepository; // Inject the repository

    @GetMapping("/jobs/status")
    public ResponseEntity<?> getJobStatus() {
        // 1. Get Real Pending Count (Jobs waiting in Redis)
        Long paymentsQueue = redisTemplate.opsForList().size("queue:payments");
        Long refundsQueue = redisTemplate.opsForList().size("queue:refunds");
        Long webhooksQueue = redisTemplate.opsForList().size("queue:webhooks");
        
        // Sum of all queues
        long pendingInRedis = (paymentsQueue != null ? paymentsQueue : 0) + 
                              (refundsQueue != null ? refundsQueue : 0) + 
                              (webhooksQueue != null ? webhooksQueue : 0);

        // 2. Get Completed Count (Success + Failed in DB)
        long successCount = paymentRepository.countByStatus("success");
        long failedCount = paymentRepository.countByStatus("failed");
        long completedTotal = successCount + failedCount;

        // 3. Calculate Processing Count
        // Logic: Items in DB that are 'pending' BUT are no longer in Redis means a Worker is holding them.
        long totalPendingInDb = paymentRepository.countByStatus("pending");
        long processingCount = Math.max(0, totalPendingInDb - (paymentsQueue != null ? paymentsQueue : 0));

        return ResponseEntity.ok(Map.of(
            "pending", pendingInRedis,
            "processing", processingCount,
            "completed", completedTotal,
            "worker_status", "running"
        ));
    }
}