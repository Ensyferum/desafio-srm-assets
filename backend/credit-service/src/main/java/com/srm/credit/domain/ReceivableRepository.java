package com.srm.credit.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

    Optional<Receivable> findByIdAndStatus(UUID id, ReceivableStatus status);
}
