package com.appfamilia.feature.dependentes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appfamilia.R;
import com.appfamilia.core.rede.Modelos;
import com.appfamilia.feature.vacinas.VacinasActivity;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Lista dos filhos da Casa. Tocar no botão adiciona; segurar um item remove. */
public class DependentesActivity extends AppCompatActivity {

    private DependentesViewModel viewModel;
    /** Data escolhida no diálogo de novo filho, no formato AAAA-MM-DD. */
    private String dataEscolhida;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_dependentes);
        viewModel = new ViewModelProvider(this).get(DependentesViewModel.class);

        DependenteAdapter adapter = new DependenteAdapter(this::abrirVacinas, this::confirmarRemocao);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        TextView vazio = findViewById(R.id.texto_vazio);

        viewModel.lista().observe(this, itens -> {
            adapter.atualizar(itens);
            vazio.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.erro().observe(this, mensagem -> {
            if (mensagem != null) {
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
                viewModel.erroMostrado();
            }
        });

        findViewById(R.id.botao_adicionar).setOnClickListener(v -> abrirNovo());
    }

    private void abrirNovo() {
        dataEscolhida = null;
        View conteudo = getLayoutInflater().inflate(R.layout.dialogo_dependente, null);
        EditText nome = conteudo.findViewById(R.id.campo_nome);
        Button data = conteudo.findViewById(R.id.botao_data);
        data.setOnClickListener(v -> escolherData(data));

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Adicionar filho(a)")
                .setView(conteudo)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();
        // O clique é definido depois do show() para o diálogo não fechar quando faltar algum campo
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = nome.getText().toString().trim();
            if (n.isEmpty() || dataEscolhida == null) {
                Toast.makeText(this, "Informe o nome e a data de nascimento", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.adicionar(n, dataEscolhida);
            dialogo.dismiss();
        });
    }

    private void escolherData(Button botao) {
        MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Data de nascimento")
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        seletor.addOnPositiveButtonClickListener(millis -> {
            // O seletor devolve a data em UTC; formatar em UTC evita cair no dia anterior
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            dataEscolhida = iso.format(new Date(millis));
            botao.setText("Nascimento: " + DependenteAdapter.dataBr(dataEscolhida));
        });
        seletor.show(getSupportFragmentManager(), "data_nascimento");
    }

    private void abrirVacinas(Modelos.Dependente dependente) {
        startActivity(new Intent(this, VacinasActivity.class)
                .putExtra(VacinasActivity.EXTRA_ID, dependente.id)
                .putExtra(VacinasActivity.EXTRA_NOME, dependente.nome));
    }

    private void confirmarRemocao(Modelos.Dependente dependente) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover " + dependente.nome + "?")
                .setMessage("Os dados ligados a este cadastro deixam de aparecer.")
                .setPositiveButton("Remover", (dialogo, qual) -> viewModel.remover(dependente))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
