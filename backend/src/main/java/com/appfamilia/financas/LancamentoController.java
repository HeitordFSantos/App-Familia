package com.appfamilia.financas;

import com.appfamilia.financas.Lancamento.Tipo;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Finanças da Casa por lançamento manual. A ligação com banco (Open Finance) vai entrar
 * como outra origem de lançamentos, sem mudar este contrato.
 */
@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

    public record NovoLancamento(
            @NotNull Tipo tipo,
            @Positive long valorCentavos,
            @NotBlank @Size(max = 60) String categoria,
            @Size(max = 255) String descricao,
            @NotNull LocalDate data) {
    }

    public record LancamentoResposta(UUID id, String tipo, long valorCentavos, String categoria,
                                     String descricao, LocalDate data) {
        static LancamentoResposta de(Lancamento l) {
            return new LancamentoResposta(l.getId(), l.getTipo().name(), l.getValorCentavos(), l.getCategoria(),
                    l.getDescricao(), l.getData());
        }
    }

    public record ResumoMes(String mes, long receitasCentavos, long despesasCentavos, long saldoCentavos,
                            List<LancamentoResposta> itens) {
    }

    private final LancamentoRepository lancamentos;

    public LancamentoController(LancamentoRepository lancamentos) {
        this.lancamentos = lancamentos;
    }

    /** Lançamentos e totais de um mês. mes no formato AAAA-MM; sem ele, o mês atual. */
    @GetMapping
    public ResumoMes resumo(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String mes) {
        YearMonth periodo;
        try {
            periodo = mes == null || mes.isBlank() ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new ErroNegocio(HttpStatus.BAD_REQUEST, "Mês inválido; use o formato AAAA-MM");
        }
        List<Lancamento> doMes = lancamentos.findByCasaIdAndDataBetweenOrderByDataDescCriadoEmDesc(
                UsuarioAtual.de(jwt).casaId(), periodo.atDay(1), periodo.atEndOfMonth());
        long receitas = soma(doMes, Tipo.RECEITA);
        long despesas = soma(doMes, Tipo.DESPESA);
        return new ResumoMes(periodo.toString(), receitas, despesas, receitas - despesas,
                doMes.stream().map(LancamentoResposta::de).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LancamentoResposta criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoLancamento dados) {
        String descricao = dados.descricao() == null || dados.descricao().isBlank() ? null : dados.descricao().trim();
        var salvo = lancamentos.save(new Lancamento(UsuarioAtual.de(jwt).casaId(), dados.tipo(),
                dados.valorCentavos(), dados.categoria().trim(), descricao, dados.data()));
        return LancamentoResposta.de(salvo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var lancamento = lancamentos.findByIdAndCasaId(id, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Lançamento não encontrado"));
        lancamentos.delete(lancamento);
    }

    private static long soma(List<Lancamento> lista, Tipo tipo) {
        return lista.stream().filter(l -> l.getTipo() == tipo).mapToLong(Lancamento::getValorCentavos).sum();
    }
}
