package com.appfamilia.feature.devocional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Dispara no horário de início: inicia o bloqueio e agenda o início do dia seguinte. */
public class DevocionalReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context contexto, Intent intent) {
        Devocional.agendar(contexto);
    }
}
