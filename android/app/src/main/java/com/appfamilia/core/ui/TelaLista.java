package com.appfamilia.core.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appfamilia.App;
import com.appfamilia.R;
import com.appfamilia.core.rede.ApiService;
import com.appfamilia.core.rede.Chamada;
import java.util.List;

/**
 * Base das telas de lista dos módulos: um texto no topo, a lista, o aviso de lista vazia
 * e o botão de adicionar. Cada módulo diz como carregar e o que fazer nos toques.
 */
public abstract class TelaLista extends AppCompatActivity {

    protected TextView textoTopo;
    private TextView textoVazio;
    private LinhasAdapter adapter;

    /** Busca os dados na API e chama mostrar(). Roda toda vez que a tela aparece. */
    protected abstract void carregar();

    protected abstract void aoAdicionar();

    protected void aoTocar(Linha linha) {
    }

    protected void aoSegurar(Linha linha) {
    }

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_lista);
        textoTopo = findViewById(R.id.texto_topo);
        textoVazio = findViewById(R.id.texto_vazio);
        adapter = new LinhasAdapter(this::aoTocar, this::aoSegurar);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        findViewById(R.id.botao_adicionar).setOnClickListener(v -> aoAdicionar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    protected ApiService api() {
        return ((App) getApplication()).rede().api();
    }

    protected void mostrar(List<Linha> linhas, String textoQuandoVazio) {
        adapter.atualizar(linhas);
        textoVazio.setText(textoQuandoVazio);
        textoVazio.setVisibility(linhas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    protected void definirTopo(String texto) {
        textoTopo.setText(texto);
        textoTopo.setVisibility(View.VISIBLE);
    }

    protected void esconderBotaoAdicionar() {
        findViewById(R.id.botao_adicionar).setVisibility(View.GONE);
    }

    protected void avisar(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    /** Para chamadas que alteram dados: ao concluir, recarrega a lista; em erro, avisa. */
    protected <T> Chamada.Retorno<T> depoisRecarregar() {
        return new Chamada.Retorno<T>() {
            @Override
            public void sucesso(T dados) {
                carregar();
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        };
    }
}
