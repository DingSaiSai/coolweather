package com.coolweather.app.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

/**
 * Created by Administrator on 2016/11/29.
 * 实现自动更新天气————创建一个长期在后台运行的定时任务
 */

public class AutoUpdateService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    //服务每次被启动时都会被调用
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }).start();
        AlarmManager manager= (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour=8*60*60*1000;//8小时的毫秒数
        long triggerTime= SystemClock.elapsedRealtime()+anHour;//触发时间
        Intent i=new Intent(this,AutoUpdateService.class);
        PendingIntent pi=PendingIntent.getBroadcast(this,0,i,0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerTime,pi);

        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 更新天气信息
     */
    private void updateWeather() {
        SharedPreferences spf= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode=spf.getString("weather_code","");
        String address="http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                //解析JSON格式的数据并保存到SharedPreferences中
                Utility.handleWeatherResponse(AutoUpdateService.this,response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();

            }
        });

    }
}
