package com.sc.common;

import java.io.Serializable;

/**
 * 客户端发送的请求
 */
public class RequestMsg implements Serializable {
    private static final long serialVersionUID = 1L;//序列化版本号，保证小改动不会影响序列化

    private int cmd;//命令类型，具体查看Constants类
    private Object data;//传输的数据

    public RequestMsg( int cmd,Object data) {
        this.data = data;
        this.cmd = cmd;
    }

    public RequestMsg(Object data) {
        this.data = data;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
