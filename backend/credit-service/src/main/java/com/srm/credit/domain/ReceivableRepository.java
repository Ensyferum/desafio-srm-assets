package com.srm.credit.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReceivableRepository
        extends JpaRepository<Receivable, UUID>, JpaSpecificationExecutor<Receivable> {

    Optional<Receivable> findByIdAndStatus(UUID id, ReceivableStatus status);
}
