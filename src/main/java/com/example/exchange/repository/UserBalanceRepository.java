package com.example.exchange.repository;

import com.example.exchange.model.UserBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {
    
    Optional<UserBalance> findByUserIdAndCurrency(String userId, String currency);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalance ub WHERE ub.userId = :userId AND ub.currency = :currency")
    Optional<UserBalance> findByUserIdAndCurrencyForUpdate(@Param("userId") String userId,
                                                           @Param("currency") String currency);

    List<UserBalance> findByUserId(String userId);
    
    boolean existsByUserIdAndCurrency(String userId, String currency);
}
