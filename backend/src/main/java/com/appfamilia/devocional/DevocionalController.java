package com.appfamilia.devocional;

import com.appfamilia.core.membro.Papel;
import com.appfamilia.devocional.PlanoDevocional.Tipo;
import com.appfamilia.devocional.biblia.Livros;
import com.appfamilia.devocional.biblia.Livros.Livro;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devocional")
public class DevocionalController {

    public record Fonte(String titulo, String url) {
    }

    public record DevocionalResposta(LocalDate data, String referencia, String textoBiblico, String direitos,
                                     String titulo, String reflexao, String aplicacao, String oracao,
                                     List<Fonte> fontes) {
    }

    public record PlanoResposta(String tipo, String valor, String descricao, int proximoCapitulo) {
    }

    public record NovoPlano(@NotNull Tipo tipo, @NotBlank String valor) {
    }

    private final DevocionalService servico;
    private final ObjectMapper json;

    public DevocionalController(DevocionalService servico, ObjectMapper json) {
        this.servico = servico;
        this.json = json;
    }

    /** Devocional de hoje. Na primeira chamada do dia ele é gerado, o que pode levar um minuto. */
    @GetMapping("/hoje")
    public DevocionalResposta hoje(@AuthenticationPrincipal Jwt jwt) {
        DevocionalDiario d = servico.deHoje(UsuarioAtual.de(jwt).casaId());
        return new DevocionalResposta(d.getData(), d.getReferencia(), d.getTextoBiblico(), d.getDireitos(),
                d.getTitulo(), d.getReflexao(), d.getAplicacao(), d.getOracao(), fontes(d.getFontesJson()));
    }

    @GetMapping("/plano")
    public PlanoResposta plano(@AuthenticationPrincipal Jwt jwt) {
        return resposta(servico.plano(UsuarioAtual.de(jwt).casaId()));
    }

    /** Troca o plano. Vale a partir do próximo devocional; o de hoje, se já existe, fica como está. */
    @PutMapping("/plano")
    public PlanoResposta trocarPlano(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoPlano dados) {
        UsuarioAtual usuario = UsuarioAtual.de(jwt);
        if (usuario.papel() != Papel.ADMIN) {
            throw new ErroNegocio(HttpStatus.FORBIDDEN, "Só o administrador da Casa pode trocar o plano");
        }
        return resposta(servico.trocarPlano(usuario.casaId(), dados.tipo(), dados.valor()));
    }

    /** Livros que podem ser escolhidos no plano. */
    @GetMapping("/livros")
    public List<Livro> livros() {
        return Livros.TODOS;
    }

    private static PlanoResposta resposta(PlanoDevocional plano) {
        String descricao = plano.getTipo() == Tipo.LIVRO
                ? Livros.porCodigo(plano.getValor()).map(Livro::nome).orElse(plano.getValor())
                        + ", um capítulo por dia (próximo: " + plano.getProximoCapitulo() + ")"
                : "Tema: " + plano.getValor();
        return new PlanoResposta(plano.getTipo().name(), plano.getValor(), descricao, plano.getProximoCapitulo());
    }

    private List<Fonte> fontes(String fontesJson) {
        if (fontesJson == null || fontesJson.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(fontesJson, new TypeReference<List<Fonte>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }
}
