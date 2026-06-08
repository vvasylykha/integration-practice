package com.example.exchange.repository;

import com.example.exchange.model.UserBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Atomic "add to balance, or create it" in a single statement, using PostgreSQL's
     * INSERT ... ON CONFLICT ... DO UPDATE (UPSERT). This avoids the read-modify-write race of
     * fetching the row, adding in Java and saving back.
     *
     * NOTE: ON CONFLICT / EXCLUDED is PostgreSQL-specific SQL — H2 has no such syntax (it uses
     * MERGE), so this method simply does not run on H2. It is the litmus test for BP2: a test
     * that passes on H2 gives false confidence, because a core query only works on the real
     * database you ship.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_balances (user_id, currency, balance, locked_balance, updated_at, version)
            VALUES (:userId, :currency, :amount, 0, NOW(), 0)
            ON CONFLICT (user_id, currency)
            DO UPDATE SET balance = user_balances.balance + EXCLUDED.balance,
                          updated_at = NOW()
            """, nativeQuery = true)
    void upsertBalance(@Param("userId") String userId,
                       @Param("currency") String currency,
                       @Param("amount") BigDecimal amount);
}
