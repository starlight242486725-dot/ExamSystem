package com.sc.Servicer.handle;

import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public abstract class BaseHandler {
    protected Socket socket;
    protected ObjectOutputStream oos;
    protected ObjectInputStream ois;

    public BaseHandler(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;
    }

    /**
     * 处理具体业务逻辑
     */
    public abstract void handleBusiness() throws IOException, ClassNotFoundException;

    /**
     * 发送消息的通用方法
     */
    protected void sendMessage(ResponseMsg response) throws IOException {
        oos.reset();
        oos.writeObject(response);
        oos.flush();
    }

    /**
     * 读取请求的通用方法
     */
    protected RequestMsg readRequest() throws IOException, ClassNotFoundException {
        Object obj = ois.readObject();
        if (obj instanceof RequestMsg) {
            return (RequestMsg) obj;
        }
        return null;
    }

}
