package com.sc.Servicer.core;

import com.sc.Servicer.handle.AdminHandler;
import com.sc.Servicer.handle.BaseHandler;
import com.sc.Servicer.handle.StudentHandler;
import com.sc.Servicer.handle.TeacherHandler;
import com.sc.Servicer.utils.Validator;
import com.sc.common.Constants;
import com.sc.common.ResponseMsg;

import java.io.*;
import java.net.Socket;

/**
 * 处理客户端请求的线程（分发器）
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private String role;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private final Validator validator = new Validator();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            System.out.println("客户端连接：" + socket.getInetAddress().getHostAddress());
            // 初始化流
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            // 第一步：读取客户端发送的身份标识
            Object roleObj = ois.readObject();
            if (roleObj instanceof String) {
                this.role = (String) roleObj;
                System.out.println("客户端身份：" + this.role);

                // 验证身份是否合法
                if (!validator.isValidRole(this.role)) {
                    oos.writeObject("身份不合法，连接关闭");
                    oos.flush();
                    socket.close();
                    System.out.println("流关闭");
                    return;
                }
            } else {
                oos.writeObject("身份格式错误，连接关闭");
                oos.flush();
                socket.close();
                System.out.println("流关闭");
                return;
            }

            // 第二步：根据身份分发给专门的处理器
            handleBusinessByRole();

        }  catch (IOException e) {
            System.out.println("客户端连接断开");
        } catch (ClassNotFoundException e) {
            System.out.println("类型异常");
        } finally {
            closeResources();
        }
    }

    // 根据身份分发给不同的处理器
    private void handleBusinessByRole() throws IOException, ClassNotFoundException {
        BaseHandler handler = null;

        switch (role) {
            case Constants.ADMIN:
                handler = new AdminHandler(socket, oos, ois);
                break;
            case Constants.STUDENT:
                handler = new StudentHandler(socket, oos, ois);
                break;
            case Constants.TEACHER:
                handler = new TeacherHandler(socket, oos, ois);
                break;
            default:
                sendMessage(new ResponseMsg("不支持的客户端类型", 500));
                return;
        }

        // 交由专门的处理器处理业务
        handler.handleBusiness();
    }

    private void sendMessage(ResponseMsg response) throws IOException {
        oos.reset();
        oos.writeObject(response);
        oos.flush();
    }

    private void closeResources() {
        try {
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
            System.out.println("关闭输入流失败: " + e.getMessage());
        }

        try {
            if (oos != null) {
                oos.close();
            }
        } catch (IOException e) {
            System.out.println("关闭输出流失败: " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("关闭Socket失败: " + e.getMessage());
        }
    }
}
