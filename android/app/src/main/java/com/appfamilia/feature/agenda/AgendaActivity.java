package com.appfamilia.feature.agenda;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.appfamilia.R;
import com.appfamilia.core.Formatos;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.TelaLista;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compromissos da Casa de hoje em diante. Tocar em Adicionar cria; segurar um item remove. */
public class AgendaActivity extends TelaLista {

    private static final String[] LEMBRETE_TEXTOS =
            {"Sem lembrete", "Na hora", "15 minutos antes", "1 hora antes", "1 dia antes"};
    private static final Integer[] LEMBRETE_MINUTOS = {null, 0, 15, 60, 1440};

    private List<Modelos.Dependente> filhos = new ArrayList<>();
    private String dataEscolhida;
    private int horaEscolhida = -1;
    private int minutoEscolhido;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setTitle("Agenda");
        pedirPermissaoDeNotificacao();
    }

    @Override
    protected void carregar() {
        // Os filhos vêm primeiro para a lista poder mostrar de quem é cada compromisso
        Chamada.enviar(api().dependentes(), new Chamada.Retorno<List<Modelos.Dependente>>() {
            @Override
            public void sucesso(List<Modelos.Dependente> dados) {
                filhos = dados;
                carregarCompromissos();
            }

            @Override
            public void erro(String mensagem, int status) {
                carregarCompromissos();
            }
        });
    }

    private void carregarCompromissos() {
        Chamada.enviar(api().compromissos(), new Chamada.Retorno<List<Modelos.Compromisso>>() {
            @Override
            public void sucesso(List<Modelos.Compromisso> compromissos) {
                List<Linha> linhas = new ArrayList<>();
                for (Modelos.Compromisso c : compromissos) {
                    String titulo = c.titulo;
                    String filho = nomeDoFilho(c.dependenteId);
                    if (filho != null) {
                        titulo += " · " + filho;
                    }
                    String detalhe = Formatos.dataHoraBr(c.inicio);
                    if (c.lembreteMinutos != null) {
                        detalhe += " · com lembrete";
                    }
                    if (c.descricao != null && !c.descricao.isEmpty()) {
                        detalhe += "\n" + c.descricao;
                    }
                    linhas.add(new Linha(titulo, detalhe, null, c));
                }
                mostrar(linhas, "Nenhum compromisso de hoje em diante.");
                Lembretes.sincronizar(AgendaActivity.this, compromissos);
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        });
    }

    private String nomeDoFilho(String dependenteId) {
        if (dependenteId != null) {
            for (Modelos.Dependente filho : filhos) {
                if (dependenteId.equals(filho.id)) {
                    return filho.nome;
                }
            }
        }
        return null;
    }

    @Override
    protected void aoAdicionar() {
        dataEscolhida = null;
        horaEscolhida = -1;
        View conteudo = getLayoutInflater().inflate(R.layout.dialogo_compromisso, null);
        EditText titulo = conteudo.findViewById(R.id.campo_titulo);
        EditText descricao = conteudo.findViewById(R.id.campo_descricao);
        Button data = conteudo.findViewById(R.id.botao_data);
        Button hora = conteudo.findViewById(R.id.botao_hora);
        Spinner lembrete = conteudo.findViewById(R.id.spinner_lembrete);
        Spinner para = conteudo.findViewById(R.id.spinner_para);

        lembrete.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LEMBRETE_TEXTOS));
        lembrete.setSelection(3);
        List<String> pessoas = new ArrayList<>();
        pessoas.add("Família toda");
        for (Modelos.Dependente filho : filhos) {
            pessoas.add(filho.nome);
        }
        para.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pessoas));

        data.setOnClickListener(v -> {
            MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker().setTitleText("Data").build();
            seletor.addOnPositiveButtonClickListener(millis -> {
                dataEscolhida = Formatos.isoDoSeletor(millis);
                data.setText("Data: " + Formatos.dataBr(dataEscolhida));
            });
            seletor.show(getSupportFragmentManager(), "data");
        });
        hora.setOnClickListener(v -> {
            MaterialTimePicker seletor = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(9)
                    .setMinute(0)
                    .setTitleText("Horário")
                    .build();
            seletor.addOnPositiveButtonClickListener(clique -> {
                horaEscolhida = seletor.getHour();
                minutoEscolhido = seletor.getMinute();
                hora.setText(String.format(Locale.US, "Horário: %02d:%02d", horaEscolhida, minutoEscolhido));
            });
            seletor.show(getSupportFragmentManager(), "hora");
        });

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Novo compromisso")
                .setView(conteudo)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = titulo.getText().toString().trim();
            if (t.isEmpty() || dataEscolhida == null || horaEscolhida < 0) {
                avisar("Informe o título, a data e o horário");
                return;
            }
            Modelos.Compromisso novo = new Modelos.Compromisso();
            novo.titulo = t;
            novo.descricao = descricao.getText().toString().trim();
            novo.inicio = String.format(Locale.US, "%sT%02d:%02d:00", dataEscolhida, horaEscolhida, minutoEscolhido);
            novo.lembreteMinutos = LEMBRETE_MINUTOS[lembrete.getSelectedItemPosition()];
            int escolhido = para.getSelectedItemPosition();
            novo.dependenteId = escolhido > 0 ? filhos.get(escolhido - 1).id : null;
            Chamada.enviar(api().criarCompromisso(novo), depoisRecarregar());
            dialogo.dismiss();
        });
    }

    @Override
    protected void aoSegurar(Linha linha) {
        Modelos.Compromisso compromisso = (Modelos.Compromisso) linha.dado;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover \"" + compromisso.titulo + "\"?")
                .setPositiveButton("Remover", (dialogo, qual) ->
                        Chamada.enviar(api().removerCompromisso(compromisso.id), depoisRecarregar()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }
}
