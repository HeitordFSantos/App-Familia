package com.appfamilia.feature.inicio;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appfamilia.App;
import com.appfamilia.R;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.feature.agenda.AgendaActivity;
import com.appfamilia.feature.auth.LoginActivity;
import com.appfamilia.feature.compras.ComprasActivity;
import com.appfamilia.feature.dependentes.DependentesActivity;
import com.appfamilia.feature.devocional.DevocionalActivity;
import com.appfamilia.feature.financas.FinancasActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Tela inicial: mostra a Casa, quem faz parte dela e dá acesso aos módulos. */
public class InicioActivity extends AppCompatActivity {

    private App app;
    private TextView textoCasa;
    private TextView textoMembros;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_inicio);
        app = (App) getApplication();

        ((TextView) findViewById(R.id.texto_saudacao)).setText("Olá, " + app.sessao().nome());
        textoCasa = findViewById(R.id.texto_casa);
        textoMembros = findViewById(R.id.texto_membros);

        abrirAoTocar(R.id.botao_agenda, AgendaActivity.class);
        abrirAoTocar(R.id.botao_filhos, DependentesActivity.class);
        abrirAoTocar(R.id.botao_compras, ComprasActivity.class);
        abrirAoTocar(R.id.botao_financas, FinancasActivity.class);
        abrirAoTocar(R.id.botao_devocional, DevocionalActivity.class);

        Button convidar = findViewById(R.id.botao_convidar);
        convidar.setVisibility(app.sessao().admin() ? View.VISIBLE : View.GONE);
        convidar.setOnClickListener(v -> pedirEmailDoConvite());

        findViewById(R.id.botao_sair).setOnClickListener(v -> sair());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarCasa();
    }

    private void abrirAoTocar(int idDoBotao, Class<?> tela) {
        findViewById(idDoBotao).setOnClickListener(v -> startActivity(new Intent(this, tela)));
    }

    private void carregarCasa() {
        Chamada.enviar(app.rede().api().minhaCasa(), new Chamada.Retorno<Modelos.Casa>() {
            @Override
            public void sucesso(Modelos.Casa casa) {
                setTitle(casa.nome);
                textoCasa.setText(casa.nome);
                StringBuilder lista = new StringBuilder();
                for (Modelos.MembroResumo membro : casa.membros) {
                    if (lista.length() > 0) {
                        lista.append("\n");
                    }
                    lista.append(membro.nome);
                    if ("ADMIN".equals(membro.papel)) {
                        lista.append(" (administrador)");
                    }
                }
                textoMembros.setText(lista.toString());
            }

            @Override
            public void erro(String mensagem, int status) {
                if (status == 401) {
                    sair();
                } else {
                    textoMembros.setText(mensagem);
                }
            }
        });
    }

    private void pedirEmailDoConvite() {
        EditText campo = new EditText(this);
        campo.setHint("E-mail da pessoa");
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int margem = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout moldura = new FrameLayout(this);
        moldura.setPadding(margem, margem / 2, margem, 0);
        moldura.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Convidar adulto")
                .setMessage("A pessoa entra na sua Casa ao criar a conta com este e-mail e o código gerado.")
                .setView(moldura)
                .setPositiveButton("Gerar convite", (dialogo, qual) -> convidar(campo.getText().toString().trim()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void convidar(String email) {
        Chamada.enviar(app.rede().api().convidar(new Modelos.NovoConvite(email)), new Chamada.Retorno<Modelos.Convite>() {
            @Override
            public void sucesso(Modelos.Convite convite) {
                String texto = "Convite para o App Família\nE-mail: " + convite.email + "\nCódigo: " + convite.token;
                new MaterialAlertDialogBuilder(InicioActivity.this)
                        .setTitle("Convite gerado")
                        .setMessage(texto + "\n\nVale por 7 dias.")
                        .setPositiveButton("Compartilhar", (dialogo, qual) -> {
                            Intent envio = new Intent(Intent.ACTION_SEND);
                            envio.setType("text/plain");
                            envio.putExtra(Intent.EXTRA_TEXT, texto);
                            startActivity(Intent.createChooser(envio, "Enviar convite"));
                        })
                        .setNegativeButton("Fechar", null)
                        .show();
            }

            @Override
            public void erro(String mensagem, int status) {
                Toast.makeText(InicioActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sair() {
        app.sessao().sair();
        Intent login = new Intent(this, LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
        finish();
    }
}
