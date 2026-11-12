package com.appfamilia.shared.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Foto enviada pelo app para leitura pelo modelo de linguagem. A imagem é repassada ao
 * modelo e descartada: o backend não guarda a foto.
 */
public record Foto(
        @NotBlank @Size(max = 8_000_000, message = "foto grande demais") String imagemBase64,
        String tipo) {
}
