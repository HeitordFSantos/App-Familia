package com.appfamilia.core;

import android.content.Context;
import android.content.SharedPreferences;
import com.appfamilia.BuildConfig;
import com.appfamilia.core.rede.Modelos;
import okhttp3.HttpUrl;

/** Dados do usuário logado e o endereço do servidor, guardados no aparelho. */
public class Sessao {

    private final SharedPreferences prefs;

    public Sessao(Context contexto) {
        prefs = contexto.getSharedPreferences("sessao", Context.MODE_PRIVATE);
    }

    public boolean logado() {
        return token() != null;
    }

    public String token() {
        return prefs.getString("token", null);
    }

    public String nome() {
        return prefs.getString("nome", "");
    }

    public boolean admin() {
        return "ADMIN".equals(prefs.getString("papel", ""));
    }

    public void salvar(Modelos.SessaoResposta sessao) {
        prefs.edit()
                .putString("token", sessao.token)
                .putString("nome", sessao.nome)
                .putString("papel", sessao.papel)
                .apply();
    }

    public void sair() {
        prefs.edit().remove("token").remove("nome").remove("papel").apply();
    }

    public String servidor() {
        return prefs.getString("servidor", BuildConfig.SERVIDOR_PADRAO);
    }

    /** Aceita "192.168.0.10:8080" ou uma URL completa. Devolve false se o endereço for inválido. */
    public boolean definirServidor(String endereco) {
        String url = endereco.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (HttpUrl.parse(url) == null) {
            return false;
        }
        prefs.edit().putString("servidor", url).apply();
        return true;
    }
}
