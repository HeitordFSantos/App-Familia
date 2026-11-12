package com.appfamilia.core.convite;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteRepository extends JpaRepository<Convite, UUID> {

    Optional<Convite> findByToken(String token);
}
