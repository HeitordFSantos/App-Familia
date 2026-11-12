package com.appfamilia.feature.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.appfamilia.App;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import retrofit2.Call;

/** Estado das telas de login e cadastro: parado, carregando, erro ou logado. */
public class AuthViewModel extends AndroidViewModel {

    public static class Estado {
        public final boolean carregando;
        public final boolean logado;
        public final String erro;

        private Estado(boolean carregando, boolean logado, String erro) {
            this.carregando = carregando;
            this.logado = logado;
            this.erro = erro;
        }
    }

    private final App app;
    private final MutableLiveData<Estado> estado = new MutableLiveData<>(new Estado(false, false, null));

    public AuthViewModel(@NonNull Application application) {
        super(application);
        app = (App) application;
    }

    public LiveData<Estado> estado() {
        return estado;
    }

    public void entrar(String email, String senha) {
        enviar(app.rede().api().entrar(new Modelos.Login(email, senha)));
    }

    public void cadastrar(String nome, String email, String senha, String nomeCasa, String convite) {
        Modelos.Cadastro dados = new Modelos.Cadastro();
        dados.nome = nome;
        dados.email = email;
        dados.senha = senha;
        dados.nomeCasa = vazioParaNulo(nomeCasa);
        dados.conviteToken = vazioParaNulo(convite);
        enviar(app.rede().api().cadastrar(dados));
    }

    public void entrarComGoogle(String idToken, String nomeCasa, String convite) {
        Modelos.LoginGoogle dados = new Modelos.LoginGoogle();
        dados.idToken = idToken;
        dados.nomeCasa = vazioParaNulo(nomeCasa);
        dados.conviteToken = vazioParaNulo(convite);
        enviar(app.rede().api().entrarComGoogle(dados));
    }

    public void mostrarErro(String mensagem) {
        estado.setValue(new Estado(false, false, mensagem));
    }

    private void enviar(Call<Modelos.SessaoResposta> chamada) {
        estado.setValue(new Estado(true, false, null));
        Chamada.enviar(chamada, new Chamada.Retorno<Modelos.SessaoResposta>() {
            @Override
            public void sucesso(Modelos.SessaoResposta sessao) {
                app.sessao().salvar(sessao);
                estado.setValue(new Estado(false, true, null));
            }

            @Override
            public void erro(String mensagem, int status) {
                estado.setValue(new Estado(false, false, mensagem));
            }
        });
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }
}
