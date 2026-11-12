package com.appfamilia.core.casa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasaRepository extends JpaRepository<Casa, UUID> {
}
