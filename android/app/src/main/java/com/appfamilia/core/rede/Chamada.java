package com.appfamilia.core.rede;

import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Executa uma chamada à API e entrega o resultado, ou a mensagem de erro, na thread principal. */
public final class Chamada {

    public interface Retorno<T> {
        void sucesso(T dados);

        /** status é o código HTTP, ou 0 quando não houve resposta do servidor. */
        void erro(String mensagem, int status);
    }

    private static final Gson GSON = new Gson();

    private Chamada() {
    }

    public static <T> void enviar(Call<T> chamada, Retorno<T> retorno) {
        chamada.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> resposta) {
                if (resposta.isSuccessful()) {
                    retorno.sucesso(resposta.body());
                } else {
                    retorno.erro(mensagemDeErro(resposta), resposta.code());
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable falha) {
                retorno.erro("Sem conexão com o servidor. Confira o endereço e a internet.", 0);
            }
        });
    }

    private static String mensagemDeErro(Response<?> resposta) {
        try {
            if (resposta.errorBody() != null) {
                Modelos.Erro erro = GSON.fromJson(resposta.errorBody().string(), Modelos.Erro.class);
                if (erro != null && erro.erro != null) {
                    return erro.erro;
                }
            }
        } catch (Exception ignorada) {
            // corpo de erro fora do formato esperado: cai na mensagem genérica
        }
        if (resposta.code() == 401) {
            return "Sessão expirada. Entre novamente.";
        }
        return "Erro no servidor (" + resposta.code() + ")";
    }
}
