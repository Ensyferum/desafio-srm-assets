package com.srm.credit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableTypeRepository extends JpaRepository<ReceivableType, UUID> {

    Optional<ReceivableType> findByName(String name);

    List<ReceivableType> findAllByOrderByName();
}
