package com.appfamilia.feature.vacinas;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.appfamilia.core.Formatos;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.SeletorDeFoto;
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
    private SeletorDeFoto seletorDeFoto;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        dependenteId = getIntent().getStringExtra(EXTRA_ID);
        setTitle("Vacinas · " + getIntent().getStringExtra(EXTRA_NOME));
        esconderBotaoAdicionar();
        seletorDeFoto = new SeletorDeFoto(this, this::lerCaderneta);
        definirTopo("Referência: Calendário Nacional de Vacinação da criança. Toque em uma dose para "
                + "marcar quando foi aplicada, ou use o menu para ler a caderneta por foto. Quem define o que aplicar é a unidade de saúde, com a caderneta.");
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

    // ----- Caderneta por foto -----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Ler caderneta por foto");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Ler caderneta por foto")
                    .setMessage("Fotografe uma página por vez, com boa luz. A foto é enviada ao servidor e ao modelo "
                            + "de inteligência artificial configurado nele para leitura, e não fica guardada. "
                            + "Você confere as doses encontradas antes de qualquer coisa ser marcada.")
                    .setPositiveButton("Continuar", (dialogo, qual) -> seletorDeFoto.abrir())
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void lerCaderneta(String fotoBase64) {
        avisar("Lendo a caderneta… pode levar cerca de um minuto");
        Chamada.enviar(api().lerCaderneta(dependenteId, new Modelos.Foto(fotoBase64)),
                new Chamada.Retorno<List<Modelos.SugestaoVacina>>() {
                    @Override
                    public void sucesso(List<Modelos.SugestaoVacina> sugestoes) {
                        if (sugestoes.isEmpty()) {
                            avisar("Nenhuma dose legível nesta foto. Tente outra, mais nítida, ou marque à mão.");
                        } else {
                            conferir(sugestoes);
                        }
                    }

                    @Override
                    public void erro(String mensagem, int status) {
                        avisar(mensagem);
                    }
                });
    }

    /** Mostra o que o modelo leu. Só o que o usuário deixar marcado é gravado. */
    private void conferir(List<Modelos.SugestaoVacina> sugestoes) {
        CharSequence[] textos = new CharSequence[sugestoes.size()];
        boolean[] escolhidas = new boolean[sugestoes.size()];
        for (int i = 0; i < sugestoes.size(); i++) {
            Modelos.SugestaoVacina s = sugestoes.get(i);
            textos[i] = s.vacina + " · " + s.dose + "\n" + Formatos.dataBr(s.aplicadaEm)
                    + (s.jaMarcada ? " (já marcada)" : "");
            escolhidas[i] = !s.jaMarcada;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confira com a caderneta")
                .setMultiChoiceItems(textos, escolhidas, (dialogo, qual, marcada) -> escolhidas[qual] = marcada)
                .setPositiveButton("Marcar selecionadas", (dialogo, qual) -> {
                    List<Modelos.SugestaoVacina> confirmadas = new ArrayList<>();
                    for (int i = 0; i < sugestoes.size(); i++) {
                        if (escolhidas[i]) {
                            confirmadas.add(sugestoes.get(i));
                        }
                    }
                    marcarEmSequencia(confirmadas, 0);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Grava uma dose por vez; no fim, recarrega a lista. */
    private void marcarEmSequencia(List<Modelos.SugestaoVacina> confirmadas, int posicao) {
        if (posicao >= confirmadas.size()) {
            carregar();
            return;
        }
        Modelos.SugestaoVacina s = confirmadas.get(posicao);
        Chamada.enviar(api().marcarVacina(dependenteId, s.codigo, new Modelos.Aplicacao(s.aplicadaEm)),
                new Chamada.Retorno<Void>() {
                    @Override
                    public void sucesso(Void nada) {
                        marcarEmSequencia(confirmadas, posicao + 1);
                    }

                    @Override
                    public void erro(String mensagem, int status) {
                        avisar(s.vacina + ": " + mensagem);
                        marcarEmSequencia(confirmadas, posicao + 1);
                    }
                });
    }
}
