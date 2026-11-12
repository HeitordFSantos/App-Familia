package com.appfamilia.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.appfamilia.feature.agenda.Lembretes;
import com.appfamilia.feature.devocional.Devocional;

/** Os alarmes se perdem quando o aparelho reinicia; aqui eles são refeitos. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context contexto, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Lembretes.reagendar(contexto);
            Devocional.agendar(contexto);
        }
    }
}
