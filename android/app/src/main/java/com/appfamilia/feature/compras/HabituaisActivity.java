package com.appfamilia.feature.compras;

import android.os.Bundle;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.TelaLista;
import java.util.ArrayList;
import java.util.List;

/**
 * Produtos que a Casa conhece, do mais para o menos usado. A caixa marcada indica que o
 * produto pode ser sugerido; tocar liga ou desliga. A pontuação sobe com fotos, lista e
 * compras, e desce quando a sugestão é recusada.
 */
public class HabituaisActivity extends TelaLista {

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setTitle("Itens habituais");
        esconderBotaoAdicionar();
        definirTopo("A lista aprende com o uso: produtos que aparecem nas fotos, entram na lista ou são comprados "
                + "sobem; os que você desmarca nas sugestões descem. Toque para nunca mais sugerir um produto, "
                + "ou para voltar a sugerir.");
    }

    @Override
    protected void carregar() {
        Chamada.enviar(api().habituais(), new Chamada.Retorno<List<Modelos.ItemHabitual>>() {
            @Override
            public void sucesso(List<Modelos.ItemHabitual> itens) {
                List<Linha> linhas = new ArrayList<>();
                for (Modelos.ItemHabitual item : itens) {
                    String detalhe = nomeDoLugar(item.lugar) + " · " + item.pontos
                            + (item.pontos == 1 || item.pontos == -1 ? " ponto" : " pontos");
                    if (item.ignorado) {
                        detalhe += " · não sugerir";
                    } else if (!item.sugerivel) {
                        detalhe += " · ainda pouco usado";
                    }
                    linhas.add(new Linha(item.nome, detalhe, !item.ignorado, item));
                }
                mostrar(linhas, "Nenhum produto conhecido ainda.");
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        });
    }

    @Override
    protected void aoAdicionar() {
        // os produtos entram pelo uso, não por cadastro
    }

    @Override
    protected void aoTocar(Linha linha) {
        Modelos.ItemHabitual item = (Modelos.ItemHabitual) linha.dado;
        Chamada.enviar(api().ignorarHabitual(item.chave, new Modelos.Ignorado(!item.ignorado)), depoisRecarregar());
    }

    static String nomeDoLugar(String lugar) {
        if (lugar == null) {
            return "Sem lugar";
        }
        switch (lugar) {
            case "GELADEIRA":
                return "Geladeira";
            case "FREEZER":
                return "Freezer";
            case "ARMARIO":
                return "Armário";
            case "LIMPEZA":
                return "Limpeza";
            case "HIGIENE":
                return "Higiene";
            default:
                return "Outro lugar";
        }
    }
}
