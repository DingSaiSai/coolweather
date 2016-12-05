package com.coolweather.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolweather.app.R;
import com.coolweather.app.service.AutoUpdateService;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

/**
 * Created by Administrator on 2016/11/26.
 */

public class WeatherActivity extends Activity implements View.OnClickListener {
    private LinearLayout weatherInfoLL;
    private TextView cityNameTV;//城市名称
    private TextView publishTimeTV;//发布时间
    private TextView currentDateTV;//显示当前日期
    private TextView weatherDespTV;//显示天气描述信息
    private TextView temp1TV;//显示气温1
    private TextView temp2TV;//显示气温2

    private Button switchBN;//切换城市
    private Button refreshBN;//手动更新


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_layout);
        //初始化各控件
        weatherInfoLL = (LinearLayout) findViewById(R.id.weather_info_layout);
        cityNameTV = (TextView) findViewById(R.id.city_name);
        publishTimeTV = (TextView) findViewById(R.id.publish_time);
        currentDateTV = (TextView) findViewById(R.id.current_date);
        weatherDespTV = (TextView) findViewById(R.id.weather_desp);
        temp1TV = (TextView) findViewById(R.id.temp1);
        temp2TV = (TextView) findViewById(R.id.temp2);

        switchBN = (Button) findViewById(R.id.switch_city);
        refreshBN = (Button) findViewById(R.id.refresh_weather);
        switchBN.setOnClickListener(this);
        refreshBN.setOnClickListener(this);
        //从Intent中取出县代号
        String countyCode = getIntent().getStringExtra("county_code");

        if (!TextUtils.isEmpty(countyCode)) {
            //根据县代号查询天气代号
            publishTimeTV.setText("同步中...");
            cityNameTV.setVisibility(View.INVISIBLE);
            weatherInfoLL.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        }
        else {
            //没有县级代号就直接显示本地天气信息
            showLocalWeather();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_city://切换城市
                Intent intent = new Intent(WeatherActivity.this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_weather://手动刷新天气
                publishTimeTV.setText("同步中...");
                SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(this);
                String weatherCode = spf.getString("weather_code", "");
                if (!TextUtils.isEmpty(weatherCode)) {
                    queryWeatherInfo(weatherCode);

                }
                break;
            default:
                break;
        }

    }

    /**
     * 根据县代号查询天气代号
     */
    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }


    /**
     * 根据天气代号查询对应的天气信息
     */
    private void queryWeatherInfo(String weatherCode) {
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".xml";
        queryFromServer(address, "weatherCode");
    }

    /**
     * 根据传入的地址和 类型 去服务器查找天气代号或者天气信息
     */
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                if ("countyCode".equals(type)) {
                    //从服务器返回的数据中解析出天气代号
                    if (!TextUtils.isEmpty(response)) {
                        String array[] = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                }
                else if ("weatherCode".equals(type)) {
                    //处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLocalWeather();//显示天气信息
                        }
                    });
                }
            }


            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishTimeTV.setText("同步失败");
                    }
                });

            }
        });

    }


    /**
     * 从sharedPreference读取存储的天气信息，并显示到界面上
     */

    private void showLocalWeather() {
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(this);
        cityNameTV.setText(spf.getString("city_name", ""));
        publishTimeTV.setText(spf.getString("publish_time", ""));
        currentDateTV.setText(spf.getString("今天" + "current_date", "") + "发布");
        weatherDespTV.setText(spf.getString("weather_desp", ""));
        temp1TV.setText(spf.getString("temp1", ""));
        temp2TV.setText(spf.getString("temp2", ""));
        weatherInfoLL.setVisibility(View.VISIBLE);
        cityNameTV.setVisibility(View.VISIBLE);

        //激活AutoUpdateService
        Intent intent=new Intent(this, AutoUpdateService.class);
        startService(intent);

    }

}
