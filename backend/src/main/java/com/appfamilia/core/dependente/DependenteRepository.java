package com.appfamilia.core.dependente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenteRepository extends JpaRepository<Dependente, UUID> {

    List<Dependente> findByCasaIdOrderByDataNascimento(UUID casaId);

    /** Busca sempre com o casa_id junto, para uma Casa nunca enxergar dados de outra. */
    Optional<Dependente> findByIdAndCasaId(UUID id, UUID casaId);
}
