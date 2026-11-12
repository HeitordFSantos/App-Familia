package com.appfamilia.feature.compras;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.appfamilia.R;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.SeletorDeFoto;
import com.appfamilia.core.ui.TelaLista;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista de compras da Casa, compartilhada entre os adultos. Tocar marca como comprado,
 * segurar remove. O menu sugere itens a partir de uma foto do armário ou da geladeira.
 */
public class ComprasActivity extends TelaLista {

    private SeletorDeFoto seletorDeFoto;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setTitle("Lista de compras");
        seletorDeFoto = new SeletorDeFoto(this, this::sugerirPorFoto);
    }

    @Override
    protected void carregar() {
        Chamada.enviar(api().compras(), new Chamada.Retorno<List<Modelos.ItemCompra>>() {
            @Override
            public void sucesso(List<Modelos.ItemCompra> itens) {
                List<Linha> linhas = new ArrayList<>();
                for (Modelos.ItemCompra item : itens) {
                    linhas.add(new Linha(item.nome, item.quantidade, item.comprado, item));
                }
                mostrar(linhas, "A lista está vazia.");
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        });
    }

    @Override
    protected void aoAdicionar() {
        View conteudo = getLayoutInflater().inflate(R.layout.dialogo_item_compra, null);
        EditText nome = conteudo.findViewById(R.id.campo_nome);
        EditText quantidade = conteudo.findViewById(R.id.campo_quantidade);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Adicionar item")
                .setView(conteudo)
                .setPositiveButton("Adicionar", null)
                .setNegativeButton("Fechar", null)
                .create();
        dialogo.show();
        // O diálogo fica aberto depois de adicionar, para lançar vários itens em sequência
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = nome.getText().toString().trim();
            if (n.isEmpty()) {
                avisar("Informe o nome do item");
                return;
            }
            Chamada.enviar(api().criarItemCompra(new Modelos.ItemCompra(n, quantidade.getText().toString().trim())),
                    depoisRecarregar());
            nome.setText("");
            quantidade.setText("");
            nome.requestFocus();
        });
    }

    @Override
    protected void aoTocar(Linha linha) {
        Modelos.ItemCompra item = (Modelos.ItemCompra) linha.dado;
        Chamada.enviar(api().marcarComprado(item.id, new Modelos.Comprado(!item.comprado)), depoisRecarregar());
    }

    @Override
    protected void aoSegurar(Linha linha) {
        Modelos.ItemCompra item = (Modelos.ItemCompra) linha.dado;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover \"" + item.nome + "\"?")
                .setPositiveButton("Remover", (dialogo, qual) ->
                        Chamada.enviar(api().removerItemCompra(item.id), depoisRecarregar()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Limpar comprados");
        menu.add(Menu.NONE, 2, Menu.NONE, "Sugerir itens por foto");
        menu.add(Menu.NONE, 3, Menu.NONE, "Itens habituais");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            Chamada.enviar(api().limparComprados(), depoisRecarregar());
            return true;
        }
        if (item.getItemId() == 3) {
            startActivity(new Intent(this, HabituaisActivity.class));
            return true;
        }
        if (item.getItemId() == 2) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Sugerir itens por foto")
                    .setMessage("Fotografe o armário ou a geladeira aberta. A foto é enviada ao servidor e ao modelo "
                            + "de inteligência artificial configurado nele, e não fica guardada. O modelo só vê o que "
                            + "aparece na foto: confira as sugestões antes de adicionar. O que você desmarcar deixa de ser sugerido com o tempo.")
                    .setPositiveButton("Continuar", (dialogo, qual) -> seletorDeFoto.abrir())
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ----- Sugestões por foto -----

    private void sugerirPorFoto(String fotoBase64) {
        avisar("Analisando a foto… pode levar cerca de um minuto");
        Chamada.enviar(api().sugerirCompras(new Modelos.Foto(fotoBase64)),
                new Chamada.Retorno<Modelos.AnaliseFoto>() {
                    @Override
                    public void sucesso(Modelos.AnaliseFoto analise) {
                        if (analise.sugestoes == null || analise.sugestoes.isEmpty()) {
                            avisar("Nada a sugerir nesta foto");
                        } else {
                            escolher(analise);
                        }
                    }

                    @Override
                    public void erro(String mensagem, int status) {
                        avisar(mensagem);
                    }
                });
    }

    /**
     * Mostra as sugestões já marcadas. O que ficar marcado entra na lista; o que for
     * desmarcado conta como recusa e tende a não ser sugerido de novo. Cancelar não registra nada.
     */
    private void escolher(Modelos.AnaliseFoto analise) {
        List<Modelos.SugestaoCompra> sugestoes = analise.sugestoes;
        CharSequence[] textos = new CharSequence[sugestoes.size()];
        boolean[] escolhidos = new boolean[sugestoes.size()];
        for (int i = 0; i < sugestoes.size(); i++) {
            Modelos.SugestaoCompra s = sugestoes.get(i);
            boolean temMotivo = s.motivo != null && !s.motivo.isEmpty();
            textos[i] = temMotivo ? s.nome + " (" + s.motivo + ")" : s.nome;
            escolhidos[i] = true;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(HabituaisActivity.nomeDoLugar(analise.lugar) + ": adicionar à lista?")
                .setMultiChoiceItems(textos, escolhidos, (dialogo, qual, marcado) -> escolhidos[qual] = marcado)
                .setPositiveButton("Confirmar", (dialogo, qual) -> {
                    List<String> aceitos = new ArrayList<>();
                    List<String> recusados = new ArrayList<>();
                    for (int i = 0; i < sugestoes.size(); i++) {
                        (escolhidos[i] ? aceitos : recusados).add(sugestoes.get(i).nome);
                    }
                    Chamada.enviar(api().confirmarSugestoes(
                            new Modelos.Confirmacao(analise.lugar, aceitos, recusados)), depoisRecarregar());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
