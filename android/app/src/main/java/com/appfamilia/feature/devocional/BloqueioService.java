package com.appfamilia.feature.devocional;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import com.appfamilia.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Roda durante o horário do devocional. A cada instante confere qual aplicativo está
 * aberto; se for um dos bloqueados, cobre a tela com um aviso. Para sozinho no fim do período.
 */
public class BloqueioService extends Service {

    private static final String CANAL = "devocional";
    private static final int ID_NOTIFICACAO = 7002;
    private static final long INTERVALO = 700;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private final Runnable verificacao = this::verificar;

    private UsageStatsManager uso;
    private WindowManager janelas;
    private View cobertura;
    private Set<String> bloqueados;
    private long fim;
    private long ultimaConsulta;
    private String appAberto;

    @Override
    public int onStartCommand(Intent intent, int flags, int idInicio) {
        DevocionalConfig config = new DevocionalConfig(this);
        fim = config.fimDoPeriodoAtual();
        bloqueados = config.apps();

        // Um serviço em primeiro plano precisa mostrar a notificação logo ao iniciar
        Notification notificacao = notificacao();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_NOTIFICACAO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_NOTIFICACAO, notificacao);
        }
        if (!config.ativo() || fim == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        uso = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        janelas = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // Olha a última hora para descobrir o que já estava aberto quando o período começou
        ultimaConsulta = System.currentTimeMillis() - 60 * 60 * 1000L;
        relogio.removeCallbacks(verificacao);
        relogio.post(verificacao);
        return START_STICKY;
    }

    private void verificar() {
        long agora = System.currentTimeMillis();
        if (agora >= fim) {
            stopSelf();
            return;
        }
        atualizarAppAberto(agora);
        if (appAberto != null && bloqueados.contains(appAberto)) {
            cobrir();
        } else {
            descobrir();
        }
        relogio.postDelayed(verificacao, INTERVALO);
    }

    /** Lê os eventos de uso desde a última consulta; o último "foi para a frente" é o app aberto. */
    private void atualizarAppAberto(long agora) {
        try {
            UsageEvents eventos = uso.queryEvents(ultimaConsulta, agora);
            UsageEvents.Event evento = new UsageEvents.Event();
            while (eventos.hasNextEvent()) {
                eventos.getNextEvent(evento);
                if (evento.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    appAberto = evento.getPackageName();
                }
            }
            ultimaConsulta = agora;
        } catch (Exception semAcessoDeUso) {
            // sem a permissão de acesso de uso não há como saber o app aberto
        }
    }

    private void cobrir() {
        if (cobertura != null || !Devocional.temSobreposicao(this)) {
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.overlay_bloqueio, null);
        String ate = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(fim));
        ((TextView) view.findViewById(R.id.texto_ate)).setText("Este aplicativo volta às " + ate + ".");
        view.findViewById(R.id.botao_voltar).setOnClickListener(v -> {
            Intent inicio = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(inicio);
            appAberto = null;
            descobrir();
        });

        int tipo = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams parametros = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                tipo,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
        try {
            janelas.addView(view, parametros);
            cobertura = view;
        } catch (Exception semPermissao) {
            cobertura = null;
        }
    }

    private void descobrir() {
        if (cobertura != null) {
            try {
                janelas.removeView(cobertura);
            } catch (Exception jaRemovida) {
                // nada a fazer
            }
            cobertura = null;
        }
    }

    private Notification notificacao() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificacoes = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificacoes.createNotificationChannel(new NotificationChannel(
                    CANAL, "Horário do devocional", NotificationManager.IMPORTANCE_LOW));
        }
        String ate = fim > 0 ? new SimpleDateFormat("HH:mm", Locale.US).format(new Date(fim)) : "";
        return new NotificationCompat.Builder(this, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle("Horário do devocional")
                .setContentText(ate.isEmpty() ? "Encerrando" : "Redes sociais bloqueadas até " + ate)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        relogio.removeCallbacks(verificacao);
        descobrir();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
