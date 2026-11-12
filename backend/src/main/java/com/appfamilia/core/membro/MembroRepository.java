package com.appfamilia.core.membro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroRepository extends JpaRepository<Membro, UUID> {

    Optional<Membro> findByEmail(String email);

    Optional<Membro> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    List<Membro> findByCasaIdOrderByNome(UUID casaId);
}
