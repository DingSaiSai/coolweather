package com.coolweather.app.util;

/**
 * Created by Administrator on 2016/10/23.
 * 回调服务器返回的结果
 */

public interface  HttpCallbackListener {
    //在服务器成功响应请求时调用
    void onFinish(String response);//参数代表服务器返回的数据
    //当进行网络操作出现错误时调用
    void onError(Exception e);//参数记录错误的详细信息
}
