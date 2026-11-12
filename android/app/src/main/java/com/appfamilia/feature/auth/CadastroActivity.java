package com.appfamilia.feature.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.appfamilia.R;

/**
 * Cadastro. Quem informa o nome da Casa cria uma Casa nova e vira administrador;
 * quem informa o código de convite entra na Casa de quem convidou.
 */
public class CadastroActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_cadastro);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        EditText nome = findViewById(R.id.campo_nome);
        EditText email = findViewById(R.id.campo_email);
        EditText senha = findViewById(R.id.campo_senha);
        EditText nomeCasa = findViewById(R.id.campo_nome_casa);
        EditText convite = findViewById(R.id.campo_convite);
        Button criar = findViewById(R.id.botao_criar);
        Button google = findViewById(R.id.botao_google);
        TextView erro = findViewById(R.id.texto_erro);
        View progresso = findViewById(R.id.progresso);

        criar.setOnClickListener(v -> {
            String n = nome.getText().toString().trim();
            String e = email.getText().toString().trim();
            String s = senha.getText().toString();
            if (n.isEmpty() || e.isEmpty()) {
                viewModel.mostrarErro("Preencha nome e e-mail");
            } else if (s.length() < 8) {
                viewModel.mostrarErro("A senha precisa ter pelo menos 8 caracteres");
            } else if (!casaOuConvite(nomeCasa, convite)) {
                viewModel.mostrarErro("Informe o nome da Casa ou o código de convite");
            } else {
                viewModel.cadastrar(n, e, s, nomeCasa.getText().toString(), convite.getText().toString());
            }
        });

        google.setVisibility(GoogleLogin.disponivel() ? View.VISIBLE : View.GONE);
        google.setOnClickListener(v -> {
            if (!casaOuConvite(nomeCasa, convite)) {
                viewModel.mostrarErro("Informe o nome da Casa ou o código de convite antes de continuar com o Google");
                return;
            }
            GoogleLogin.pedir(this, new GoogleLogin.Retorno() {
                @Override
                public void token(String idToken) {
                    viewModel.entrarComGoogle(idToken, nomeCasa.getText().toString(), convite.getText().toString());
                }

                @Override
                public void erro(String mensagem) {
                    viewModel.mostrarErro(mensagem);
                }
            });
        });

        viewModel.estado().observe(this, estado -> {
            progresso.setVisibility(estado.carregando ? View.VISIBLE : View.GONE);
            criar.setEnabled(!estado.carregando);
            google.setEnabled(!estado.carregando);
            erro.setText(estado.erro == null ? "" : estado.erro);
            erro.setVisibility(estado.erro == null ? View.GONE : View.VISIBLE);
            if (estado.logado) {
                finish(); // a tela de login percebe a sessão e abre o início
            }
        });
    }

    private static boolean casaOuConvite(EditText nomeCasa, EditText convite) {
        return !nomeCasa.getText().toString().trim().isEmpty() || !convite.getText().toString().trim().isEmpty();
    }
}
