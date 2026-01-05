package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.domain.users.UserStatus;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findByStatus(UserStatus status);
    
    // Bank-related queries
    @Query("SELECT u FROM UserEntity u WHERE u.bankId = :bankId")
    List<UserEntity> findByBankId(@Param("bankId") Long bankId);
    
    @Query("SELECT u FROM UserEntity u WHERE u.bankId = :bankId AND u.status = :status")
    List<UserEntity> findByBankIdAndStatus(@Param("bankId") Long bankId, @Param("status") UserStatus status);
    
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email AND u.bankId = :bankId")
    boolean existsByEmailAndBankId(@Param("email") String email, @Param("bankId") Long bankId);
    
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.bankId = :bankId")
    Optional<UserEntity> findByEmailAndBankId(@Param("email") String email, @Param("bankId") Long bankId);
}