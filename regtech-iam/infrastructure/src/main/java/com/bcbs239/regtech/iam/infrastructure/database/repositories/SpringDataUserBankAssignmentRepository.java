package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.UserBankAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataUserBankAssignmentRepository extends JpaRepository<UserBankAssignmentEntity, String> {
    List<UserBankAssignmentEntity> findByUserId(String userId);
}