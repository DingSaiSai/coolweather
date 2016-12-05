package com.coolweather.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/10/31.
 */

public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;

    private TextView titleTV;
    private ListView listLV;
    private ArrayAdapter adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<>();//数据源
    private List<Province> provinceList;//省列表
    private List<City> cityList;//市列表
    private List<County> countyList;//县列表
    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的城市
    private int currentLevel;//当前选中的级别

    /**
     * 是否从weatherActivity中跳转过来
     */
    private boolean isFromWeatherActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        //已经选择了城市且不是从WeatherActivity中跳转而来的，才会直接跳转到WeatherActivity
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(this);
        if (spf.getBoolean("city_selected", false) && !isFromWeatherActivity) {
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.choose_area);
        titleTV = (TextView) findViewById(R.id.ca_tv_title);
        listLV = (ListView) findViewById(R.id.ca_lv_list);
        adapter = new ArrayAdapter<String>(this,
                                           android.R.layout.simple_list_item_1,
                                           dataList);
        listLV.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);//创建数据库实例
        listLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {//点击了省份
                    selectedProvince = provinceList.get(position);
                    //加载市级数据
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY) {//点击了市
                    selectedCity = cityList.get(position);
                    //加载县级数据
                    queryCounties();
                }
                else if (currentLevel == LEVEL_COUNTRY) { //点击了县
                    String countyCode = countyList.get(position).getCountyCode();
                    //跳转到天气显示页面
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    intent.putExtra("county_code", countyCode);
                    startActivity(intent);
                    finish();

                }

            }
        });
        queryProvinces();
    }


    /**
     * 查询全国所有的省,优先从数据库中查询，如果没有再去服务器中查询
     */
    private void queryProvinces() {
        //从数据库中查询
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province p : provinceList) {
                dataList.add(p.getProvinceName());//用provinceList中的数据替换dataList中的数
            }
            adapter.notifyDataSetChanged();
            listLV.setSelection(0);
            titleTV.setText("中国");
            currentLevel = LEVEL_PROVINCE;//当前级别为省级别
        }
        //从服务器查询数据
        else {
            queryFromServer(null, "province");
        }

    }

    /**
     * 查询选中省内的所有市，优先从数据库查询，如果没有查到再到服务器上查询
     */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City c : cityList) {
                dataList.add(c.getCityName());//用cityList中的数据替换dataList中的数据
            }
            adapter.notifyDataSetChanged();
            listLV.setSelection(0);
            titleTV.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;//当前级别为市级别
        }
        else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    /**
     * 查询选中市内的所有的县，优先从数据库中查询，如果数据库中没有，再到服务器上查询
     */
    private void queryCounties() {
        countyList = coolWeatherDB.loadCountries(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County c : countyList) {
                dataList.add(c.getCountyName());

            }
            adapter.notifyDataSetChanged();
            listLV.setSelection(0);
            titleTV.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTRY;//当前级别为县级别
        }
        else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }


    /**
     * 根据传入的代号和类型从服务器上查询省市县数据
     */

    private void queryFromServer(final String code, final String type) {
        String address;
        if (TextUtils.isEmpty(code)) {//查询的是省级数据
            address = "www.weather.com.cn/data/list3/city.xml";
        }
        else {//查询的是市或县
            address = "www.weather.com.cn/data/list3/city" + code + ".xml";
        }
        showProgressDialog();
        //发送请求
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                //解析数据
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(coolWeatherDB, response);
                }
                else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(coolWeatherDB, response,
                                                          selectedProvince.getId());
                }
                else if ("county".equals(type)) {
                    result = Utility.handleCountriesResponse(coolWeatherDB, response,
                                                             selectedCity.getId());
                }

                if (result) {
                    //解析成功，数据已经存储到数据库
                    //通过runOnUiThread回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            }
                            else if ("city".equals(type)) {
                                queryCities();
                            }
                            else if ("county".equals(type)) {
                                queryCounties();
                            }

                        }
                    });
                }

            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, " 加载失败", Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });

    }

    /**
     * 显示进度对话框
     */
    private ProgressDialog dialog;

    private void showProgressDialog() {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setMessage("正在加载");
            dialog.setCancelable(false);
        }
        dialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }


    /**
     * 捕获back键，根据当前的级别来看，应该返回市列表，省列表，还是直接退出
     */
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTRY) {
            queryCities();
        }
        else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        }
        else {//从WeatherActivity跳转过来的，应重新回到WeatherActivity
            if (isFromWeatherActivity) {
                Intent intent = new Intent(this, WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }


}
