package com.appfamilia.compras;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, UUID> {

    /** Itens a comprar primeiro, depois os já comprados; dentro de cada grupo, por ordem de criação. */
    List<ItemCompra> findByCasaIdOrderByCompradoAscCriadoEmAsc(UUID casaId);

    Optional<ItemCompra> findByIdAndCasaId(UUID id, UUID casaId);

    List<ItemCompra> findByCasaIdAndCompradoTrue(UUID casaId);
}
