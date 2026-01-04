package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.application.dashboard.port.FileRepository;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.infrastructure.entity.FileEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JpaFileRepository implements FileRepository {
    private final SpringDataFileRepository repo;

    public JpaFileRepository(SpringDataFileRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ComplianceFile> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ComplianceFile> findByBankId(BankId bankId) {
        return repo.findByBankId(bankId.getValue()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ComplianceFile> findByBankIdAndDateBetween(BankId bankId, String startDate, String endDate) {
        return repo.findByBankIdAndDateBetween(bankId.getValue(), startDate, endDate)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public ComplianceFile save(ComplianceFile file) {
        FileEntity entity = toEntity(file);
        FileEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    private ComplianceFile toDomain(FileEntity e) {
        BankId bid = e.getBankId() == null ? BankId.unknown() : BankId.of(e.getBankId());
        return new ComplianceFile(e.getFilename(), e.getDate(), e.getScore(), e.getStatus(), e.getBatchId(), bid);
    }

    private FileEntity toEntity(ComplianceFile f) {
        String bank = f.getBankId() == null ? null : f.getBankId().getValue();
        return new FileEntity(f.getFilename(), f.getDate(), f.getScore(), f.getStatus(), f.getBatchId(), bank);
    }
}
