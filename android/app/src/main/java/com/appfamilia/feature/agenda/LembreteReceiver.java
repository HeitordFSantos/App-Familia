package com.appfamilia.feature.agenda;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.appfamilia.R;

/** Recebe o alarme do lembrete e mostra a notificação. */
public class LembreteReceiver extends BroadcastReceiver {

    private static final String CANAL = "lembretes";

    @Override
    public void onReceive(Context contexto, Intent intent) {
        NotificationManager notificacoes =
                (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            notificacoes.createNotificationChannel(new NotificationChannel(
                    CANAL, "Lembretes de compromissos", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent abrir = new Intent(contexto, AgendaActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent aoTocar = PendingIntent.getActivity(contexto, 0, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notificacao = new NotificationCompat.Builder(contexto, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(intent.getStringExtra(Lembretes.EXTRA_TITULO))
                .setContentText(intent.getStringExtra(Lembretes.EXTRA_TEXTO))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(aoTocar)
                .build();
        String acao = intent.getAction() == null ? "" : intent.getAction();
        try {
            notificacoes.notify(acao.hashCode(), notificacao);
        } catch (SecurityException semPermissao) {
            // o usuário não permitiu notificações
        }
    }
}
