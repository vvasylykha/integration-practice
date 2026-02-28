package com.example.exchange.service;

import com.example.exchange.model.AuditLog;
import com.example.exchange.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logExchangeEvent(Long exchangeId, String eventType, String userId,
                                  String details, String ipAddress, String userAgent) {
        log.debug("Logging audit event: {} for exchange {}", eventType, exchangeId);

        AuditLog auditLog = AuditLog.builder()
                .exchangeId(exchangeId)
                .eventType(eventType)
                .userId(userId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .processedAt(LocalDateTime.now())
                .build();

        try {
            auditLogRepository.save(auditLog);
            log.info("Audit log created for exchange {}: {}", exchangeId, eventType);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate audit event ignored for exchange {}: {}", exchangeId, eventType);
        }
    }

    public boolean isEventAlreadyProcessed(Long exchangeId, String eventType) {
        return auditLogRepository.findByExchangeIdAndEventType(exchangeId, eventType).isPresent();
    }
}
