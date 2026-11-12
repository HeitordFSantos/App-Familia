package com.appfamilia.vacinas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroVacinaRepository extends JpaRepository<RegistroVacina, UUID> {

    List<RegistroVacina> findByDependenteId(UUID dependenteId);

    Optional<RegistroVacina> findByDependenteIdAndCodigoDose(UUID dependenteId, String codigoDose);
}
