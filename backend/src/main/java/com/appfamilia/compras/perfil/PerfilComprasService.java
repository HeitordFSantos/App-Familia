package com.appfamilia.compras.perfil;

import com.appfamilia.compras.perfil.ProdutoCasa.Evento;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprende o que cada Casa costuma ter e monta as sugestões de compra.
 *
 * Pontuação de um produto = 1 se for básico do catálogo + o histórico da Casa
 * (visto +1, adicionado +2, comprado +3, recusado -3). Um básico entra nas sugestões com
 * 1 ponto ou mais; um produto fora dos básicos precisa de 2, ou seja, de algum histórico.
 */
@Service
public class PerfilComprasService {

    public static final int MAXIMO_SUGESTOES = 25;
    private static final int PONTO_BASICO = 1;

    /** Produto que pode ser sugerido, já com a pontuação da Casa. */
    public record Candidato(String chave, String nome, Lugar lugar, int pontos, boolean basico,
                            boolean temHistorico, boolean ignorado) {

        public boolean sugerivel() {
            return !ignorado && lugar != null && pontos >= (basico ? PONTO_BASICO : 2);
        }
    }

    /** Produto que o modelo viu na foto. */
    public record Visto(String nome, boolean acabando) {
    }

    public record Sugestao(String nome, String motivo) {
    }

    private final ProdutoComumRepository catalogo;
    private final ProdutoCasaRepository perfil;

    public PerfilComprasService(ProdutoComumRepository catalogo, ProdutoCasaRepository perfil) {
        this.catalogo = catalogo;
        this.perfil = perfil;
    }

    @Transactional
    public void registrar(UUID casaId, String nome, Lugar lugar, Evento evento) {
        String chave = NomeProduto.chave(nome);
        if (chave.isEmpty()) {
            return;
        }
        ProdutoCasa produto = perfil.findByCasaIdAndChave(casaId, chave).orElseGet(() -> {
            ProdutoComum doCatalogo = catalogo.findById(chave).orElse(null);
            return doCatalogo != null
                    ? new ProdutoCasa(casaId, chave, doCatalogo.getNome(), doCatalogo.getLugar())
                    : new ProdutoCasa(casaId, chave, NomeProduto.exibicao(nome), null);
        });
        produto.definirLugarSeVazio(lugar);
        produto.registrar(evento);
        perfil.save(produto);
    }

    /** Catálogo base e histórico da Casa juntos, do mais para o menos usado. */
    @Transactional(readOnly = true)
    public List<Candidato> candidatos(UUID casaId) {
        Map<String, ProdutoComum> doCatalogo = new LinkedHashMap<>();
        catalogo.findAll().forEach(p -> doCatalogo.put(p.getChave(), p));
        Map<String, Candidato> todos = new LinkedHashMap<>();
        for (ProdutoComum p : doCatalogo.values()) {
            todos.put(p.getChave(), new Candidato(p.getChave(), p.getNome(), p.getLugar(),
                    p.isBasico() ? PONTO_BASICO : 0, p.isBasico(), false, false));
        }
        for (ProdutoCasa p : perfil.findByCasaId(casaId)) {
            ProdutoComum base = doCatalogo.get(p.getChave());
            boolean basico = base != null && base.isBasico();
            Lugar lugar = p.getLugar() != null ? p.getLugar() : base != null ? base.getLugar() : null;
            todos.put(p.getChave(), new Candidato(p.getChave(), p.getNome(), lugar,
                    (basico ? PONTO_BASICO : 0) + p.pontos(), basico, true, p.isIgnorado()));
        }
        List<Candidato> lista = new ArrayList<>(todos.values());
        lista.sort(Comparator.comparing(Candidato::ignorado)
                .thenComparing(Comparator.comparingInt(Candidato::pontos).reversed())
                .thenComparing(Candidato::nome));
        return lista;
    }

    /**
     * Sugestões para uma foto: primeiro o que o modelo viu acabando; depois o que a Casa
     * costuma ter naquele lugar e não apareceu. Nada que já esteja na lista a comprar.
     */
    public List<Sugestao> sugerir(List<Candidato> candidatos, Lugar lugar, List<Visto> vistos,
                                  Set<String> chavesNaLista) {
        Map<String, Candidato> porChave = new LinkedHashMap<>();
        candidatos.forEach(c -> porChave.put(c.chave(), c));
        Set<String> jaTratadas = new HashSet<>(chavesNaLista);
        List<Sugestao> sugestoes = new ArrayList<>();

        for (Visto visto : vistos) {
            String chave = NomeProduto.chave(visto.nome());
            boolean novo = !chave.isEmpty() && jaTratadas.add(chave);
            Candidato conhecido = porChave.get(chave);
            if (novo && visto.acabando() && (conhecido == null || !conhecido.ignorado())) {
                String nome = conhecido != null ? conhecido.nome() : NomeProduto.exibicao(visto.nome());
                sugestoes.add(new Sugestao(nome, "acabando"));
            }
        }
        if (lugar != null && lugar != Lugar.OUTRO) {
            for (Candidato c : candidatos) {
                if (sugestoes.size() >= MAXIMO_SUGESTOES) {
                    break;
                }
                if (c.lugar() == lugar && c.sugerivel() && jaTratadas.add(c.chave())) {
                    boolean habitual = c.temHistorico() && c.pontos() >= 2;
                    sugestoes.add(new Sugestao(c.nome(), habitual ? "vocês costumam ter" : "item básico"));
                }
            }
        }
        return sugestoes.size() > MAXIMO_SUGESTOES ? sugestoes.subList(0, MAXIMO_SUGESTOES) : sugestoes;
    }

    /** Liga ou desliga as sugestões de um produto. Devolve falso se o produto não existir. */
    @Transactional
    public boolean ignorar(UUID casaId, String chave, boolean ignorado) {
        ProdutoCasa produto = perfil.findByCasaIdAndChave(casaId, chave).orElse(null);
        if (produto == null) {
            ProdutoComum base = catalogo.findById(chave).orElse(null);
            if (base == null) {
                return false;
            }
            produto = new ProdutoCasa(casaId, chave, base.getNome(), base.getLugar());
        }
        produto.setIgnorado(ignorado);
        perfil.save(produto);
        return true;
    }
}
