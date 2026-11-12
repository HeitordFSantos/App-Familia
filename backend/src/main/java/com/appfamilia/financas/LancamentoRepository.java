package com.appfamilia.financas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {

    List<Lancamento> findByCasaIdAndDataBetweenOrderByDataDescCriadoEmDesc(UUID casaId, LocalDate de, LocalDate ate);

    Optional<Lancamento> findByIdAndCasaId(UUID id, UUID casaId);
}
