package com.appfamilia.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

/** Agenda um aviso para um horário. Usa alarme exato quando o sistema permite. */
public final class Alarmes {

    private Alarmes() {
    }

    public static void agendar(Context contexto, long quando, PendingIntent acao) {
        AlarmManager alarmes = (AlarmManager) contexto.getSystemService(Context.ALARM_SERVICE);
        try {
            if (Build.VERSION.SDK_INT < 31 || alarmes.canScheduleExactAlarms()) {
                alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, acao);
                return;
            }
        } catch (SecurityException semPermissao) {
            // cai no alarme aproximado abaixo
        }
        alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, acao);
    }

    public static void cancelar(Context contexto, PendingIntent acao) {
        AlarmManager alarmes = (AlarmManager) contexto.getSystemService(Context.ALARM_SERVICE);
        alarmes.cancel(acao);
    }
}
