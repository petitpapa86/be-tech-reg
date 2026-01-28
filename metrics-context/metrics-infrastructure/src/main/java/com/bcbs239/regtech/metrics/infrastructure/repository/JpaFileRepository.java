package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.application.dashboard.port.FileRepository;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.infrastructure.entity.FileEntity;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
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
    public List<ComplianceFile> findByBankIdAndDateBetween(BankId bankId, String startDate, String endDate, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return repo.findByBankIdAndDateBetweenOrderByDateDesc(bankId.getValue(), startDate, endDate, pageable)
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
        QualityReportId reportId = e.getReportId() == null ? null : QualityReportId.of(e.getReportId());
        return new ComplianceFile(e.getId(), e.getFilename(), e.getDate(), e.getScore(), e.getCompletenessScore(), e.getStatus(), e.getBatchId(), bid, reportId);
    }

    private FileEntity toEntity(ComplianceFile f) {
        String bank = f.getBankId() == null ? null : f.getBankId().getValue();
        String reportId = f.getReportId() == null ? null : f.getReportId().value();
        return new FileEntity(f.getFilename(), f.getDate(), f.getScore(), f.getCompletenessScore(), f.getStatus(), f.getBatchId(), bank, reportId);
    }
}
