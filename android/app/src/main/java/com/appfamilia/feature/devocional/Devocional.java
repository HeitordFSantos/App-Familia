package com.appfamilia.feature.devocional;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.appfamilia.core.Alarmes;

/** Liga e desliga o bloqueio conforme a configuração: agenda o próximo início e inicia o serviço. */
public final class Devocional {

    private Devocional() {
    }

    /** Chamado ao salvar a configuração, quando o alarme dispara e depois de reiniciar o aparelho. */
    public static void agendar(Context contexto) {
        PendingIntent inicio = PendingIntent.getBroadcast(contexto, 7001,
                new Intent(contexto, DevocionalReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Alarmes.cancelar(contexto, inicio);

        DevocionalConfig config = new DevocionalConfig(contexto);
        if (!config.ativo()) {
            contexto.stopService(new Intent(contexto, BloqueioService.class));
            return;
        }
        Alarmes.agendar(contexto, config.proximoInicio(), inicio);
        if (config.fimDoPeriodoAtual() > 0) {
            try {
                ContextCompat.startForegroundService(contexto, new Intent(contexto, BloqueioService.class));
            } catch (Exception semPermissaoParaIniciar) {
                // o sistema pode recusar iniciar o serviço em segundo plano; o próximo alarme tenta de novo
            }
        }
    }

    /** Permissão "Acesso de uso": necessária para saber qual aplicativo está aberto. */
    public static boolean temAcessoDeUso(Context contexto) {
        AppOpsManager operacoes = (AppOpsManager) contexto.getSystemService(Context.APP_OPS_SERVICE);
        int modo = operacoes.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), contexto.getPackageName());
        return modo == AppOpsManager.MODE_ALLOWED;
    }

    /** Permissão "Sobrepor a outros apps": necessária para mostrar a tela de bloqueio por cima. */
    public static boolean temSobreposicao(Context contexto) {
        return Settings.canDrawOverlays(contexto);
    }
}
