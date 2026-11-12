package com.appfamilia.feature.vacinas;

import android.os.Bundle;
import com.appfamilia.core.Formatos;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.TelaLista;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

/** Agenda vacinal de um filho. Tocar em uma dose marca como aplicada (ou desmarca). */
public class VacinasActivity extends TelaLista {

    public static final String EXTRA_ID = "dependenteId";
    public static final String EXTRA_NOME = "dependenteNome";

    private String dependenteId;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        dependenteId = getIntent().getStringExtra(EXTRA_ID);
        setTitle("Vacinas · " + getIntent().getStringExtra(EXTRA_NOME));
        esconderBotaoAdicionar();
        definirTopo("Referência: Calendário Nacional de Vacinação da criança. Toque em uma dose para "
                + "marcar quando foi aplicada. Quem define o que aplicar é a unidade de saúde, com a caderneta.");
    }

    @Override
    protected void carregar() {
        Chamada.enviar(api().vacinas(dependenteId), new Chamada.Retorno<List<Modelos.DoseVacina>>() {
            @Override
            public void sucesso(List<Modelos.DoseVacina> doses) {
                List<Linha> linhas = new ArrayList<>();
                for (Modelos.DoseVacina dose : doses) {
                    linhas.add(new Linha(dose.vacina + " · " + dose.dose, detalhe(dose),
                            "APLICADA".equals(dose.situacao), dose));
                }
                mostrar(linhas, "Calendário indisponível.");
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        });
    }

    private static String detalhe(Modelos.DoseVacina dose) {
        String texto = idade(dose.idadeMeses) + " · ";
        switch (dose.situacao == null ? "" : dose.situacao) {
            case "APLICADA":
                texto += "aplicada em " + Formatos.dataBr(dose.aplicadaEm);
                break;
            case "PENDENTE":
                texto += "não marcada · prevista para " + Formatos.dataBr(dose.dataPrevista);
                break;
            case "PROXIMA":
                texto += "próxima · " + Formatos.dataBr(dose.dataPrevista);
                break;
            default:
                texto += "a partir de " + Formatos.dataBr(dose.dataPrevista);
                break;
        }
        if (dose.observacao != null) {
            texto += "\n" + dose.observacao;
        }
        return texto;
    }

    private static String idade(int meses) {
        if (meses == 0) {
            return "Ao nascer";
        }
        if (meses < 24) {
            return meses + (meses == 1 ? " mês" : " meses");
        }
        return (meses / 12) + " anos";
    }

    @Override
    protected void aoAdicionar() {
        // esta tela não tem botão de adicionar: as doses vêm do calendário
    }

    @Override
    protected void aoTocar(Linha linha) {
        Modelos.DoseVacina dose = (Modelos.DoseVacina) linha.dado;
        if ("APLICADA".equals(dose.situacao)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Desmarcar " + dose.vacina + " · " + dose.dose + "?")
                    .setPositiveButton("Desmarcar", (dialogo, qual) -> Chamada.enviar(
                            api().desmarcarVacina(dependenteId, dose.codigo), depoisRecarregar()))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Quando foi aplicada?")
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        seletor.addOnPositiveButtonClickListener(millis -> Chamada.enviar(
                api().marcarVacina(dependenteId, dose.codigo, new Modelos.Aplicacao(Formatos.isoDoSeletor(millis))),
                depoisRecarregar()));
        seletor.show(getSupportFragmentManager(), "aplicacao");
    }
}
