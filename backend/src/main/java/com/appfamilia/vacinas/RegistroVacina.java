package com.appfamilia.vacinas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Marca que uma dose do calendário foi aplicada em um filho. */
@Entity
@Table(name = "registro_vacina")
public class RegistroVacina {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(name = "dependente_id", nullable = false)
    private UUID dependenteId;

    @Column(name = "codigo_dose", nullable = false)
    private String codigoDose;

    @Column(name = "aplicada_em", nullable = false)
    private LocalDate aplicadaEm;

    protected RegistroVacina() {
    }

    public RegistroVacina(UUID casaId, UUID dependenteId, String codigoDose, LocalDate aplicadaEm) {
        this.casaId = casaId;
        this.dependenteId = dependenteId;
        this.codigoDose = codigoDose;
        this.aplicadaEm = aplicadaEm;
    }

    public String getCodigoDose() {
        return codigoDose;
    }

    public LocalDate getAplicadaEm() {
        return aplicadaEm;
    }

    public void setAplicadaEm(LocalDate aplicadaEm) {
        this.aplicadaEm = aplicadaEm;
    }
}
