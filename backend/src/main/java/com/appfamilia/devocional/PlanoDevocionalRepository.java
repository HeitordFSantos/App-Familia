package com.appfamilia.devocional;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoDevocionalRepository extends JpaRepository<PlanoDevocional, UUID> {

    Optional<PlanoDevocional> findByCasaId(UUID casaId);
}
