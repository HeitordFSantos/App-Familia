package com.appfamilia.core.rede;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Os endpoints do backend. */
public interface ApiService {

    @POST("api/auth/cadastro")
    Call<Modelos.SessaoResposta> cadastrar(@Body Modelos.Cadastro dados);

    @POST("api/auth/login")
    Call<Modelos.SessaoResposta> entrar(@Body Modelos.Login dados);

    @POST("api/auth/google")
    Call<Modelos.SessaoResposta> entrarComGoogle(@Body Modelos.LoginGoogle dados);

    @GET("api/casa")
    Call<Modelos.Casa> minhaCasa();

    @GET("api/dependentes")
    Call<List<Modelos.Dependente>> dependentes();

    @POST("api/dependentes")
    Call<Modelos.Dependente> criarDependente(@Body Modelos.Dependente dados);

    @DELETE("api/dependentes/{id}")
    Call<Void> removerDependente(@Path("id") String id);

    @POST("api/convites")
    Call<Modelos.Convite> convidar(@Body Modelos.NovoConvite dados);
}
