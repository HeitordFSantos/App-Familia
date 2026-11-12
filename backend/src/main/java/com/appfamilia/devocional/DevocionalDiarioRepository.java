package com.appfamilia.devocional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevocionalDiarioRepository extends JpaRepository<DevocionalDiario, UUID> {

    Optional<DevocionalDiario> findByCasaIdAndData(UUID casaId, LocalDate data);

    /** Os mais recentes, para o modelo não repetir a mesma passagem. */
    List<DevocionalDiario> findTop15ByCasaIdOrderByDataDesc(UUID casaId);
}
