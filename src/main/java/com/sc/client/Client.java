package com.sc.client;

import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;

import java.io.IOException;
import java.net.Socket;

public interface Client {
    void create_menu() throws IOException, ClassNotFoundException;//创建菜单
    boolean login();//登录
    void sendMsg(RequestMsg requestMsg) throws IOException;//发送请求
    ResponseMsg receiveMsg() throws IOException, ClassNotFoundException;
    void closeResources();
}
