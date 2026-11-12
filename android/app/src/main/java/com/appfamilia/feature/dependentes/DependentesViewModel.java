package com.appfamilia.feature.dependentes;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.appfamilia.App;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import java.util.ArrayList;
import java.util.List;

public class DependentesViewModel extends AndroidViewModel {

    private final App app;
    private final MutableLiveData<List<Modelos.Dependente>> lista = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> erro = new MutableLiveData<>();

    public DependentesViewModel(@NonNull Application application) {
        super(application);
        app = (App) application;
        carregar();
    }

    public LiveData<List<Modelos.Dependente>> lista() {
        return lista;
    }

    /** Mensagem de erro a mostrar uma vez; a tela chama erroMostrado() depois de exibir. */
    public LiveData<String> erro() {
        return erro;
    }

    public void erroMostrado() {
        erro.setValue(null);
    }

    public void carregar() {
        Chamada.enviar(app.rede().api().dependentes(), new Chamada.Retorno<List<Modelos.Dependente>>() {
            @Override
            public void sucesso(List<Modelos.Dependente> dados) {
                lista.setValue(dados);
            }

            @Override
            public void erro(String mensagem, int status) {
                erro.setValue(mensagem);
            }
        });
    }

    public void adicionar(String nome, String dataNascimento) {
        Chamada.enviar(app.rede().api().criarDependente(new Modelos.Dependente(nome, dataNascimento)),
                new Chamada.Retorno<Modelos.Dependente>() {
                    @Override
                    public void sucesso(Modelos.Dependente criado) {
                        carregar();
                    }

                    @Override
                    public void erro(String mensagem, int status) {
                        erro.setValue(mensagem);
                    }
                });
    }

    public void remover(Modelos.Dependente dependente) {
        Chamada.enviar(app.rede().api().removerDependente(dependente.id), new Chamada.Retorno<Void>() {
            @Override
            public void sucesso(Void nada) {
                carregar();
            }

            @Override
            public void erro(String mensagem, int status) {
                erro.setValue(mensagem);
            }
        });
    }
}
