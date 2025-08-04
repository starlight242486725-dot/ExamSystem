package com.sc.common;

import java.io.Serializable;

public class ResponseMsg implements Serializable {
    private static final long serialVersionUID = 1L;//序列化版本号，保证小改动不会影响序列化
    private int code;//状态码 200成功 500失败
    private String message;//响应信息
    private Object data;//可选返回的数据
    //不带数据
    public ResponseMsg(String message, int code) {
        this.message = message;
        this.code = code;
    }
    //带数据
    public ResponseMsg(String message, int code, Object data) {
        this.message = message;
        this.code = code;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
