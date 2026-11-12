package com.appfamilia;

import android.app.Application;
import com.appfamilia.core.Sessao;
import com.appfamilia.core.rede.Rede;

/** Guarda os objetos únicos do app: a sessão do usuário e o acesso à API. */
public class App extends Application {

    private Sessao sessao;
    private Rede rede;

    @Override
    public void onCreate() {
        super.onCreate();
        sessao = new Sessao(this);
        rede = new Rede(sessao);
    }

    public Sessao sessao() {
        return sessao;
    }

    public Rede rede() {
        return rede;
    }
}
