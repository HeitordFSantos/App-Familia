package com.appfamilia.agenda;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompromissoRepository extends JpaRepository<Compromisso, UUID> {

    List<Compromisso> findByCasaIdAndInicioGreaterThanEqualOrderByInicio(UUID casaId, LocalDateTime de);

    Optional<Compromisso> findByIdAndCasaId(UUID id, UUID casaId);
}
