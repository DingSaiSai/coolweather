package com.coolweather.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.coolweather.app.service.AutoUpdateService;

/**
 * Created by Administrator on 2016/11/29.
 */

public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //再次启动AutoUpdateService
        Intent i = new Intent(context, AutoUpdateService.class);
        context.startService(i);
    }
}
