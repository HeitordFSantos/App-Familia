package com.appfamilia.feature.financas;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.appfamilia.R;
import com.appfamilia.core.Formatos;
import com.appfamilia.core.rede.Chamada;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.core.ui.Linha;
import com.appfamilia.core.ui.TelaLista;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Receitas e despesas da Casa, mês a mês, por lançamento manual. O menu troca o mês.
 * Segurar um lançamento remove.
 */
public class FinancasActivity extends TelaLista {

    /** Primeiro dia do mês que está na tela. */
    private final Calendar mes = Calendar.getInstance();
    private String dataEscolhida;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setTitle("Finanças");
        mes.set(Calendar.DAY_OF_MONTH, 1);
    }

    @Override
    protected void carregar() {
        String mesIso = new SimpleDateFormat("yyyy-MM", Locale.US).format(mes.getTime());
        String mesPorExtenso = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR")).format(mes.getTime());
        Chamada.enviar(api().lancamentos(mesIso), new Chamada.Retorno<Modelos.ResumoMes>() {
            @Override
            public void sucesso(Modelos.ResumoMes resumo) {
                definirTopo(mesPorExtenso
                        + "\nReceitas: " + Formatos.dinheiro(resumo.receitasCentavos)
                        + "\nDespesas: " + Formatos.dinheiro(resumo.despesasCentavos)
                        + "\nSaldo: " + Formatos.dinheiro(resumo.saldoCentavos));
                List<Linha> linhas = new ArrayList<>();
                for (Modelos.Lancamento l : resumo.itens) {
                    boolean despesa = "DESPESA".equals(l.tipo);
                    String titulo = (despesa ? "− " : "+ ") + Formatos.dinheiro(l.valorCentavos) + " · " + l.categoria;
                    String detalhe = Formatos.dataBr(l.data);
                    if (l.descricao != null && !l.descricao.isEmpty()) {
                        detalhe += " · " + l.descricao;
                    }
                    linhas.add(new Linha(titulo, detalhe, null, l));
                }
                mostrar(linhas, "Nenhum lançamento neste mês.");
            }

            @Override
            public void erro(String mensagem, int status) {
                avisar(mensagem);
            }
        });
    }

    @Override
    protected void aoAdicionar() {
        dataEscolhida = Formatos.hojeIso();
        View conteudo = getLayoutInflater().inflate(R.layout.dialogo_lancamento, null);
        RadioButton receita = conteudo.findViewById(R.id.radio_receita);
        EditText valor = conteudo.findViewById(R.id.campo_valor);
        EditText categoria = conteudo.findViewById(R.id.campo_categoria);
        EditText descricao = conteudo.findViewById(R.id.campo_descricao);
        Button data = conteudo.findViewById(R.id.botao_data);
        data.setText("Data: " + Formatos.dataBr(dataEscolhida));
        data.setOnClickListener(v -> {
            MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker().setTitleText("Data").build();
            seletor.addOnPositiveButtonClickListener(millis -> {
                dataEscolhida = Formatos.isoDoSeletor(millis);
                data.setText("Data: " + Formatos.dataBr(dataEscolhida));
            });
            seletor.show(getSupportFragmentManager(), "data");
        });

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Novo lançamento")
                .setView(conteudo)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long centavos = Formatos.centavos(valor.getText().toString());
            String c = categoria.getText().toString().trim();
            if (centavos <= 0 || c.isEmpty()) {
                avisar("Informe um valor maior que zero e a categoria");
                return;
            }
            Modelos.Lancamento novo = new Modelos.Lancamento();
            novo.tipo = receita.isChecked() ? "RECEITA" : "DESPESA";
            novo.valorCentavos = centavos;
            novo.categoria = c;
            novo.descricao = descricao.getText().toString().trim();
            novo.data = dataEscolhida;
            Chamada.enviar(api().criarLancamento(novo), depoisRecarregar());
            dialogo.dismiss();
        });
    }

    @Override
    protected void aoSegurar(Linha linha) {
        Modelos.Lancamento lancamento = (Modelos.Lancamento) linha.dado;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover este lançamento?")
                .setMessage(linha.titulo)
                .setPositiveButton("Remover", (dialogo, qual) ->
                        Chamada.enviar(api().removerLancamento(lancamento.id), depoisRecarregar()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, 0, "Mês anterior");
        menu.add(Menu.NONE, 2, 1, "Próximo mês");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1 || item.getItemId() == 2) {
            mes.add(Calendar.MONTH, item.getItemId() == 1 ? -1 : 1);
            carregar();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
