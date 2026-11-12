package com.appfamilia.feature.devocional;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.appfamilia.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Configura o horário do devocional: quando começa, quanto dura e quais aplicativos ficam bloqueados. */
public class DevocionalActivity extends AppCompatActivity {

    private DevocionalConfig config;
    private int hora;
    private int minuto;
    private final List<CheckBox> caixasDeApps = new ArrayList<>();
    private Button botaoHorario;
    private Button botaoAcessoDeUso;
    private Button botaoSobreposicao;

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        setContentView(R.layout.activity_devocional);
        setTitle("Horário do devocional");
        config = new DevocionalConfig(this);
        hora = config.hora();
        minuto = config.minuto();

        CompoundButton ativo = findViewById(R.id.switch_ativo);
        ativo.setChecked(config.ativo());
        EditText duracao = findViewById(R.id.campo_duracao);
        duracao.setText(String.valueOf(config.duracaoMinutos()));

        botaoHorario = findViewById(R.id.botao_horario);
        atualizarHorario();
        botaoHorario.setOnClickListener(v -> {
            MaterialTimePicker seletor = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(hora)
                    .setMinute(minuto)
                    .setTitleText("Início")
                    .build();
            seletor.addOnPositiveButtonClickListener(clique -> {
                hora = seletor.getHour();
                minuto = seletor.getMinute();
                atualizarHorario();
            });
            seletor.show(getSupportFragmentManager(), "inicio");
        });

        montarListaDeApps();

        botaoAcessoDeUso = findViewById(R.id.botao_acesso_uso);
        botaoAcessoDeUso.setOnClickListener(v -> abrir(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        botaoSobreposicao = findViewById(R.id.botao_sobreposicao);
        botaoSobreposicao.setOnClickListener(v -> abrir(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));

        findViewById(R.id.botao_salvar).setOnClickListener(v -> salvar(ativo.isChecked(), duracao.getText().toString()));

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // O usuário pode ter acabado de voltar das configurações do sistema
        boolean uso = Devocional.temAcessoDeUso(this);
        boolean sobreposicao = Devocional.temSobreposicao(this);
        botaoAcessoDeUso.setText(uso ? "Acesso de uso: permitido" : "Permitir acesso de uso");
        botaoAcessoDeUso.setEnabled(!uso);
        botaoSobreposicao.setText(sobreposicao ? "Sobrepor a outros apps: permitido" : "Permitir sobrepor a outros apps");
        botaoSobreposicao.setEnabled(!sobreposicao);
    }

    private void atualizarHorario() {
        botaoHorario.setText(String.format(Locale.US, "Início: %02d:%02d", hora, minuto));
    }

    /** Uma caixa de marcação para cada aplicativo conhecido que está instalado no aparelho. */
    private void montarListaDeApps() {
        LinearLayout lista = findViewById(R.id.lista_apps);
        Set<String> escolhidos = config.apps();
        for (String[] app : DevocionalConfig.APPS_CONHECIDOS) {
            if (!instalado(app[1])) {
                continue;
            }
            CheckBox caixa = new CheckBox(this);
            caixa.setText(app[0]);
            caixa.setTag(app[1]);
            caixa.setChecked(escolhidos.contains(app[1]));
            lista.addView(caixa);
            caixasDeApps.add(caixa);
        }
        if (caixasDeApps.isEmpty()) {
            TextView aviso = new TextView(this);
            aviso.setText("Nenhum dos aplicativos que o app sabe bloquear está instalado neste aparelho.");
            lista.addView(aviso);
        }
    }

    private boolean instalado(String pacote) {
        try {
            getPackageManager().getPackageInfo(pacote, 0);
            return true;
        } catch (PackageManager.NameNotFoundException naoInstalado) {
            return false;
        }
    }

    private void abrir(Intent configuracoes) {
        try {
            startActivity(configuracoes);
        } catch (Exception telaInexistente) {
            Toast.makeText(this, "Abra as configurações do aparelho e procure esta permissão para o App Família",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void salvar(boolean ativo, String textoDuracao) {
        int duracao;
        try {
            duracao = Integer.parseInt(textoDuracao.trim());
        } catch (NumberFormatException e) {
            duracao = 0;
        }
        if (duracao < 5 || duracao > 240) {
            Toast.makeText(this, "A duração deve ficar entre 5 e 240 minutos", Toast.LENGTH_LONG).show();
            return;
        }
        // Mantém a escolha dos apps conhecidos que não estão instalados e troca a dos instalados
        Set<String> apps = new HashSet<>(config.apps());
        for (CheckBox caixa : caixasDeApps) {
            String pacote = (String) caixa.getTag();
            if (caixa.isChecked()) {
                apps.add(pacote);
            } else {
                apps.remove(pacote);
            }
        }
        config.salvar(ativo, hora, minuto, duracao, apps);
        Devocional.agendar(this);

        if (ativo && (!Devocional.temAcessoDeUso(this) || !Devocional.temSobreposicao(this))) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Faltam permissões")
                    .setMessage("O horário foi salvo, mas o bloqueio só funciona depois de permitir o acesso de uso "
                            + "e a sobreposição a outros apps, nos botões desta tela.")
                    .setPositiveButton("Entendi", null)
                    .show();
        } else {
            Toast.makeText(this, ativo ? "Horário salvo e ativado" : "Bloqueio desativado", Toast.LENGTH_SHORT).show();
        }
    }
}
