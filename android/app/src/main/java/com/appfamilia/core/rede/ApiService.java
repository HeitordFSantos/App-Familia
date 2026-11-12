package com.appfamilia.core.rede;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Os endpoints do backend. */
public interface ApiService {

    // ----- Autenticação e núcleo -----

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

    // ----- Agenda -----

    @GET("api/compromissos")
    Call<List<Modelos.Compromisso>> compromissos();

    @POST("api/compromissos")
    Call<Modelos.Compromisso> criarCompromisso(@Body Modelos.Compromisso dados);

    @DELETE("api/compromissos/{id}")
    Call<Void> removerCompromisso(@Path("id") String id);

    // ----- Vacinas -----

    @GET("api/dependentes/{id}/vacinas")
    Call<List<Modelos.DoseVacina>> vacinas(@Path("id") String dependenteId);

    @PUT("api/dependentes/{id}/vacinas/{codigo}")
    Call<Void> marcarVacina(@Path("id") String dependenteId, @Path("codigo") String codigo,
                            @Body Modelos.Aplicacao dados);

    @DELETE("api/dependentes/{id}/vacinas/{codigo}")
    Call<Void> desmarcarVacina(@Path("id") String dependenteId, @Path("codigo") String codigo);

    // ----- Compras -----

    @GET("api/compras")
    Call<List<Modelos.ItemCompra>> compras();

    @POST("api/compras")
    Call<Modelos.ItemCompra> criarItemCompra(@Body Modelos.ItemCompra dados);

    @PUT("api/compras/{id}/comprado")
    Call<Modelos.ItemCompra> marcarComprado(@Path("id") String id, @Body Modelos.Comprado dados);

    @DELETE("api/compras/comprados")
    Call<Void> limparComprados();

    @DELETE("api/compras/{id}")
    Call<Void> removerItemCompra(@Path("id") String id);

    // ----- Finanças -----

    @GET("api/lancamentos")
    Call<Modelos.ResumoMes> lancamentos(@Query("mes") String mes);

    @POST("api/lancamentos")
    Call<Modelos.Lancamento> criarLancamento(@Body Modelos.Lancamento dados);

    @DELETE("api/lancamentos/{id}")
    Call<Void> removerLancamento(@Path("id") String id);

    // ----- Fotos lidas pelo modelo de linguagem -----

    @POST("api/dependentes/{id}/vacinas/foto")
    Call<List<Modelos.SugestaoVacina>> lerCaderneta(@Path("id") String dependenteId, @Body Modelos.Foto foto);

    @POST("api/compras/foto")
    Call<List<Modelos.SugestaoCompra>> sugerirCompras(@Body Modelos.Foto foto);

    // ----- Devocional -----

    @GET("api/devocional/hoje")
    Call<Modelos.DevocionalDia> devocionalDeHoje();

    @GET("api/devocional/plano")
    Call<Modelos.Plano> planoDevocional();

    @PUT("api/devocional/plano")
    Call<Modelos.Plano> trocarPlanoDevocional(@Body Modelos.Plano dados);

    @GET("api/devocional/livros")
    Call<List<Modelos.LivroBiblia>> livrosDaBiblia();
}
