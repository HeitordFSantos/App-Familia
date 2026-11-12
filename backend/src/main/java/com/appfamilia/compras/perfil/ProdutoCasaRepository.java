package com.appfamilia.compras.perfil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoCasaRepository extends JpaRepository<ProdutoCasa, UUID> {

    List<ProdutoCasa> findByCasaId(UUID casaId);

    Optional<ProdutoCasa> findByCasaIdAndChave(UUID casaId, String chave);
}
