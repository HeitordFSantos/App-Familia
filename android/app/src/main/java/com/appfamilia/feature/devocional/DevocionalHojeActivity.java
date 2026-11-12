package com.appfamilia.feature.devocional;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.appfamilia.App;
import com.appfamilia.R;
import com.appfamilia.core.rede.ApiService;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Devocional do dia da Casa: passagem, reflexão, aplicação, oração e páginas para ler mais.
 * O servidor monta o texto uma vez por dia; a primeira abertura do dia pode demorar.
 */
public class DevocionalHojeActivity extends AppCompatActivity {

    private App app;
    private View conteudo;
    private TextView estado;
    private Button tentarDeNovo;
    private Button botaoPlano;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_devocional_hoje);
        setTitle("Devocional de hoje");
        app = (App) getApplication();

        conteudo = findViewById(R.id.conteudo);
        estado = findViewById(R.id.texto_estado);
        tentarDeNovo = findViewById(R.id.botao_tentar);
        tentarDeNovo.setOnClickListener(v -> carregar());
        botaoPlano = findViewById(R.id.botao_plano);
        botaoPlano.setOnClickListener(v -> abrirPlano());
        findViewById(R.id.botao_bloqueio).setOnClickListener(
                v -> startActivity(new Intent(this, DevocionalActivity.class)));

        carregar();
        carregarPlano();
    }

    private ApiService api() {
        return app.rede().api();
    }

    private void carregar() {
        conteudo.setVisibility(View.GONE);
        tentarDeNovo.setVisibility(View.GONE);
        estado.setVisibility(View.VISIBLE);
        estado.setText("Preparando o devocional de hoje…\nNa primeira abertura do dia pode levar cerca de um minuto.");
        Chamada.enviar(api().devocionalDeHoje(), new Chamada.Retorno<Modelos.DevocionalDia>() {
            @Override
            public void sucesso(Modelos.DevocionalDia devocional) {
                mostrar(devocional);
            }

            @Override
            public void erro(String mensagem, int status) {
                estado.setText(mensagem);
                tentarDeNovo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void mostrar(Modelos.DevocionalDia d) {
        estado.setVisibility(View.GONE);
        conteudo.setVisibility(View.VISIBLE);
        texto(R.id.texto_titulo, d.titulo);
        texto(R.id.texto_referencia, d.referencia);
        texto(R.id.texto_biblico, d.textoBiblico == null
                ? "Leia esta passagem na sua Bíblia. O texto aparece aqui quando o servidor tiver uma tradução licenciada configurada."
                : d.textoBiblico);
        texto(R.id.texto_direitos, d.direitos);
        texto(R.id.texto_reflexao, d.reflexao);
        secao(R.id.rotulo_aplicacao, R.id.texto_aplicacao, d.aplicacao);
        secao(R.id.rotulo_oracao, R.id.texto_oracao, d.oracao);

        LinearLayout fontes = findViewById(R.id.lista_fontes);
        fontes.removeAllViews();
        boolean temFontes = d.fontes != null && !d.fontes.isEmpty();
        findViewById(R.id.rotulo_fontes).setVisibility(temFontes ? View.VISIBLE : View.GONE);
        if (temFontes) {
            for (Modelos.Fonte fonte : d.fontes) {
                if (fonte.url == null || !fonte.url.startsWith("http")) {
                    continue;
                }
                Button link = new Button(this);
                link.setText(fonte.titulo == null ? fonte.url : fonte.titulo);
                link.setAllCaps(false);
                link.setOnClickListener(v -> abrirLink(fonte.url));
                fontes.addView(link);
            }
        }
    }

    private void texto(int id, String valor) {
        TextView view = findViewById(id);
        view.setText(valor == null ? "" : valor);
        view.setVisibility(valor == null || valor.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void secao(int idRotulo, int idTexto, String valor) {
        texto(idTexto, valor);
        findViewById(idRotulo).setVisibility(valor == null || valor.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void abrirLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception semNavegador) {
            Toast.makeText(this, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show();
        }
    }

    // ----- Plano de leitura -----

    private void carregarPlano() {
        Chamada.enviar(api().planoDevocional(), new Chamada.Retorno<Modelos.Plano>() {
            @Override
            public void sucesso(Modelos.Plano plano) {
                botaoPlano.setText("Plano: " + plano.descricao);
            }

            @Override
            public void erro(String mensagem, int status) {
                botaoPlano.setText("Plano de leitura");
            }
        });
    }

    private void abrirPlano() {
        if (!app.sessao().admin()) {
            Toast.makeText(this, "Só o administrador da Casa pode trocar o plano", Toast.LENGTH_LONG).show();
            return;
        }
        Chamada.enviar(api().livrosDaBiblia(), new Chamada.Retorno<List<Modelos.LivroBiblia>>() {
            @Override
            public void sucesso(List<Modelos.LivroBiblia> livros) {
                mostrarDialogoDoPlano(livros);
            }

            @Override
            public void erro(String mensagem, int status) {
                Toast.makeText(DevocionalHojeActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDialogoDoPlano(List<Modelos.LivroBiblia> livros) {
        View view = getLayoutInflater().inflate(R.layout.dialogo_plano, null);
        RadioButton porLivro = view.findViewById(R.id.radio_livro);
        Spinner livro = view.findViewById(R.id.spinner_livro);
        EditText tema = view.findViewById(R.id.campo_tema);
        List<String> nomes = new ArrayList<>();
        for (Modelos.LivroBiblia l : livros) {
            nomes.add(l.nome);
        }
        livro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nomes));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Plano do devocional")
                .setMessage("Vale a partir do próximo devocional. O de hoje, se já foi montado, continua o mesmo.")
                .setView(view)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Modelos.Plano novo;
            if (porLivro.isChecked()) {
                novo = new Modelos.Plano("LIVRO", livros.get(livro.getSelectedItemPosition()).codigo);
            } else {
                String t = tema.getText().toString().trim();
                if (t.isEmpty()) {
                    Toast.makeText(this, "Escreva o tema", Toast.LENGTH_SHORT).show();
                    return;
                }
                novo = new Modelos.Plano("TEMA", t);
            }
            Chamada.enviar(api().trocarPlanoDevocional(novo), new Chamada.Retorno<Modelos.Plano>() {
                @Override
                public void sucesso(Modelos.Plano plano) {
                    botaoPlano.setText("Plano: " + plano.descricao);
                }

                @Override
                public void erro(String mensagem, int status) {
                    Toast.makeText(DevocionalHojeActivity.this, mensagem, Toast.LENGTH_LONG).show();
                }
            });
            dialogo.dismiss();
        });
    }
}
