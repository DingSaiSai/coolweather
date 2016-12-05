package com.coolweather.app.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Administrator on 2016/10/23.
 * 将通用的网络操作提取到一个公共类中，并提供一个静态方法,
 * 当想想要发起网络请求时，只需要简单调用一下这个方法即可。
 */

public class HttpUtil {
    public static void sendHttpRequest(final String address, final HttpCallbackListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setRequestProperty("Accept-Language", "zh-CN");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    InputStream in = connection.getInputStream();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();//用于存储从网络读取的数据
                    String line;
                    while ((line = buffer.readLine()) != null) {
                        response.append(line);
                    }

                    //子线程无法通过return语句来返回数据，这里将服务器返回的数据存到了
                    // HttpCallbackListener的onFinish()方法中
                    if (listener != null) {
                        listener.onFinish(response.toString());//回调onFinish()方法
                    }

                }
                catch (Exception e) {
                    if (listener != null) {
                        listener.onError(e); //回调onError（）方法
                    }

                }
                finally {
                    connection.disconnect();
                }

            }
        }).start();
    }
}
