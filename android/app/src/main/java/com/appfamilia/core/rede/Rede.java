package com.appfamilia.core.rede;

import com.appfamilia.core.Sessao;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Monta o cliente da API. Envia o token em toda chamada e se refaz se o servidor mudar. */
public class Rede {

    private final Sessao sessao;
    private ApiService api;
    private String servidorAtual;

    public Rede(Sessao sessao) {
        this.sessao = sessao;
    }

    public synchronized ApiService api() {
        String servidor = sessao.servidor();
        if (api == null || !servidor.equals(servidorAtual)) {
            OkHttpClient cliente = new OkHttpClient.Builder()
                    // Leitura de foto e devocional passam por um modelo de linguagem, que demora
                    .readTimeout(180, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(cadeia -> {
                        Request.Builder requisicao = cadeia.request().newBuilder();
                        String token = sessao.token();
                        if (token != null) {
                            requisicao.header("Authorization", "Bearer " + token);
                        }
                        return cadeia.proceed(requisicao.build());
                    })
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(servidor)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
            servidorAtual = servidor;
        }
        return api;
    }
}
