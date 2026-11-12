package com.appfamilia.feature.compras;

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
import com.appfamilia.core.ui.TelaLista;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista de compras da Casa, compartilhada entre os adultos. Tocar marca como comprado,
 * segurar remove. A criação de itens a partir de foto entra aqui em uma próxima etapa.
 */
public class ComprasActivity extends TelaLista {

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setTitle("Lista de compras");
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            Chamada.enviar(api().limparComprados(), depoisRecarregar());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
