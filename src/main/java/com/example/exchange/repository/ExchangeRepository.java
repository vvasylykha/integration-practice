package com.example.exchange.repository;

import com.example.exchange.model.Exchange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    
    List<Exchange> findByUserId(String userId);
    
    Page<Exchange> findByUserId(String userId, Pageable pageable);
    
    List<Exchange> findByStatus(Exchange.ExchangeStatus status);
}
