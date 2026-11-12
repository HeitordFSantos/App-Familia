package com.appfamilia.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.appfamilia.App;
import com.appfamilia.R;
import com.appfamilia.core.Sessao;
import com.appfamilia.feature.inicio.InicioActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private Sessao sessao;
    private TextView textoServidor;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        sessao = ((App) getApplication()).sessao();
        if (sessao.logado()) {
            abrirInicio();
            return;
        }
        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        EditText email = findViewById(R.id.campo_email);
        EditText senha = findViewById(R.id.campo_senha);
        Button entrar = findViewById(R.id.botao_entrar);
        Button google = findViewById(R.id.botao_google);
        TextView erro = findViewById(R.id.texto_erro);
        View progresso = findViewById(R.id.progresso);
        textoServidor = findViewById(R.id.texto_servidor);

        entrar.setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String s = senha.getText().toString();
            if (e.isEmpty() || s.isEmpty()) {
                viewModel.mostrarErro("Preencha e-mail e senha");
                return;
            }
            viewModel.entrar(e, s);
        });

        google.setVisibility(GoogleLogin.disponivel() ? View.VISIBLE : View.GONE);
        google.setOnClickListener(v -> GoogleLogin.pedir(this, new GoogleLogin.Retorno() {
            @Override
            public void token(String idToken) {
                viewModel.entrarComGoogle(idToken, null, null);
            }

            @Override
            public void erro(String mensagem) {
                viewModel.mostrarErro(mensagem);
            }
        }));

        findViewById(R.id.botao_criar_conta).setOnClickListener(
                v -> startActivity(new Intent(this, CadastroActivity.class)));

        atualizarServidor();
        textoServidor.setOnClickListener(v -> pedirServidor());

        viewModel.estado().observe(this, estado -> {
            progresso.setVisibility(estado.carregando ? View.VISIBLE : View.GONE);
            entrar.setEnabled(!estado.carregando);
            google.setEnabled(!estado.carregando);
            erro.setText(estado.erro == null ? "" : estado.erro);
            erro.setVisibility(estado.erro == null ? View.GONE : View.VISIBLE);
            if (estado.logado) {
                abrirInicio();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta da tela de cadastro já logado
        if (sessao.logado() && viewModel != null) {
            abrirInicio();
        }
    }

    private void abrirInicio() {
        startActivity(new Intent(this, InicioActivity.class));
        finish();
    }

    private void atualizarServidor() {
        textoServidor.setText("Servidor: " + sessao.servidor() + "  (tocar para alterar)");
    }

    private void pedirServidor() {
        EditText campo = new EditText(this);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campo.setText(sessao.servidor());
        int margem = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout moldura = new android.widget.FrameLayout(this);
        moldura.setPadding(margem, margem / 2, margem, 0);
        moldura.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Endereço do servidor")
                .setMessage("Exemplo com o backend no seu computador: 192.168.0.10:8080")
                .setView(moldura)
                .setPositiveButton("Salvar", (dialogo, qual) -> {
                    if (!sessao.definirServidor(campo.getText().toString())) {
                        viewModel.mostrarErro("Endereço de servidor inválido");
                    }
                    atualizarServidor();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
