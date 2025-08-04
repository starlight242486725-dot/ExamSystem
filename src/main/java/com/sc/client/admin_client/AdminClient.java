package com.sc.client.admin_client;

import com.sc.client.Client;
import com.sc.client.student_client.Student;
import com.sc.client.teacher_client.Teacher;
import com.sc.common.Constants;
import com.sc.common.InputHandler;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class AdminClient implements Client, Runnable {
    private final InputHandler handler = new InputHandler();
    // 流对象提升为成员变量，方便在其他方法中使用
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    private Socket socket;


    @Override
    public void create_menu() throws IOException {
        while (true) {
            System.out.println("欢迎来到管理员端");
            System.out.println("1.添加学生");
            System.out.println("2.添加教师");
            System.out.println("3.删除学生");
            System.out.println("4.删除教师");
            System.out.println("5.查询所有学生");
            System.out.println("6.查询所有老师");
            System.out.println("7.退出系统");
            int num = handler.getIntInput("输入命令：");
            switch (num){
                case 1:{
                    addStu();
                    break;
                }
                case 2:{
                    addTea();
                    break;
                }
                case 3:{
                    delStu();
                    break;
                }
                case 4:{
                    delTea();
                    break;
                }
                case 5:{
                    getAllStu();
                    break;
                }
                case 6:{
                    getAllTea();
                    break;
                }
                // 在菜单退出时调用
                case 7:{
                    closeResources(); // 退出前关闭资源
                    return;
                }
                default:
                    System.out.println("输入编号有误");
                    break;
            }
        }

    }

    /**
     * 查找所有学生并打印信息
     */
    private void getAllStu(){
        try {
            //发送请求
            sendMsg(new RequestMsg(106,null));
            ResponseMsg responseMsg;
            try {
                responseMsg = receiveMsg();
                if (responseMsg.getCode() == 200) {
                    Map<String,Student> studentMap = (Map<String, Student>) responseMsg.getData();
                    if (studentMap == null) {
                        System.out.println("空指针异常");
                        return;
                    }
                    System.out.println("准考证\t姓名");
                    for (String s : studentMap.keySet()){
                        System.out.println(s+"\t\t"+studentMap.get(s).getName());
                    }
                }else {
                    System.out.println(responseMsg.getMessage());
                }


            } catch (SocketTimeoutException e){
                System.out.println("响应超时");
            } catch (ClassNotFoundException e) {
                System.out.println("类型异常");
            }
        } catch (IOException e) {
            System.out.println("IO异常");
        }


    }
    private void getAllTea(){
        try {
            //发送请求
            sendMsg(new RequestMsg(107,null));
            ResponseMsg responseMsg;
            try {
                responseMsg = receiveMsg();
                if (responseMsg.getCode() == 200) {
                    Map<String,Teacher> teacherMap = (Map<String, Teacher>) responseMsg.getData();
                    if (teacherMap == null) {
                        System.out.println("空指针异常");
                        return;
                    }
                    System.out.println("工号\t姓名");
                    for (String s : teacherMap.keySet()){
                        System.out.println(s+"\t\t"+teacherMap.get(s).getName());
                    }
                }else {
                    System.out.println(responseMsg.getMessage());
                }


            } catch (SocketTimeoutException e){
                System.out.println("响应超时");
            } catch (ClassNotFoundException e) {
                System.out.println("类型异常");
            }
        } catch (IOException e) {
            System.out.println("IO异常");
        }
    }
    /**
     * 删除教师
     */
    private void delTea() {
        String id_card = handler.getNonEmptyStringInput("请输入工号：");
        try{
            sendMsg(new RequestMsg(105,id_card));
            //等待响应
            //接送服务器响应
            ResponseMsg responseMsg;
            // 在 receiveMsg() 调用处处理超时
            try {
                responseMsg = receiveMsg();
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("删除成功");
                    }else {
                        System.out.println(responseMsg.getMessage());
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("服务器响应超时，请重试");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("类型异常");
            }
        } catch (IOException e) {
            System.out.println("删除老师时，出现异常");
        }
    }

    /**
     * 删除学生
     */
    private void delStu() {
        String id_card = handler.getNonEmptyStringInput("请输入准考证：");
        try{
            sendMsg(new RequestMsg(104,id_card));
            //等待响应
            //接送服务器响应
            ResponseMsg responseMsg;
            // 在 receiveMsg() 调用处处理超时
            try {
                responseMsg = receiveMsg();
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("删除成功");
                    }else {
                        System.out.println(responseMsg.getMessage());
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("服务器响应超时，请重试");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("类型异常");
            }
        } catch (IOException e) {
            System.out.println("删除学生时，出现异常");
        }
    }

    /**
     * 添加学生
     */
    public void addStu(){
        String name = handler.getNonEmptyStringInput("请输入姓名：");
        String id_card = handler.getNonEmptyStringInput("请输入准考证：");
        Student student = new Student(name,id_card);

        try {

            //发送请求
            RequestMsg requestMsg = new RequestMsg(102,student);
            sendMsg(requestMsg);

            //等待响应
            //接送服务器响应
            ResponseMsg responseMsg;
            // 在 receiveMsg() 调用处处理超时
            try {
                responseMsg = receiveMsg();
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("添加成功");
                    }else {
                        System.out.println(responseMsg.getMessage());
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("服务器响应超时，请重试");
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("添加学生时发生异常");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("接收到未知响应");
            e.printStackTrace();
        }
    }

    /**
     * 添加教师
     */
    private void addTea() {
        String name = handler.getNonEmptyStringInput("请输入姓名：");
        String id_card = handler.getNonEmptyStringInput("请输入工号：");
        Teacher teacher = new Teacher(name,id_card);
        try {

            //发送请求
            RequestMsg requestMsg = new RequestMsg(103,teacher);
            sendMsg(requestMsg);

            //等待响应
            //接送服务器响应
            ResponseMsg responseMsg;
            // 在 receiveMsg() 调用处处理超时
            try {
                responseMsg = receiveMsg();
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("添加成功");
                    }else {
                        System.out.println(responseMsg.getMessage());
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("服务器响应超时，请重试");
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("添加教师时发生异常");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("接收到未知响应");
            e.printStackTrace();
        }
    }

    /**
     * 登录
     * @return 返回登录结果
     */
    @Override
    public boolean login() {//输入姓名，密码，写入流中向服务器发送
        try {
            while (true) {
                String name = handler.getNonEmptyStringInput("请输入管理员账户：");
                String pass = handler.getNonEmptyStringInput("请输入密码：");
                Admin admin = new Admin(name, pass);
                RequestMsg requestMsg = new RequestMsg(101,admin);
                //发送登录信息
                sendMsg(requestMsg);
                System.out.println("登录请求已经发送");

                //接送服务器响应
                ResponseMsg responseMsg;
                // 在 receiveMsg() 调用处处理超时
                try {
                    responseMsg = receiveMsg();
                } catch (SocketTimeoutException e) {
                    System.out.println("服务器响应超时，请重试");
                    return false; // 或其他处理
                }
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("登录成功");
                        return true;
                    }else {
                        System.out.println(responseMsg.getMessage());
                        String isContinue = handler.getNonEmptyStringInput("是否继续(y/n):");
                        if (!"y".equalsIgnoreCase(isContinue)) {
                            break;
                        }
                    }
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("登录失败：" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送请求到服务端
     * @param requestMsg 发送的消息体
     * @throws IOException
     */
    @Override
    public void sendMsg(RequestMsg requestMsg) throws IOException {
        oos.reset(); // 重置序列化缓存
        oos.writeObject(requestMsg);
        oos.flush();
    }

    /**
     * 接收服务端响应
     * @return 服务端响应
     * @throws IOException
     * @throws ClassNotFoundException
     */

    @Override
    public ResponseMsg receiveMsg() throws IOException, ClassNotFoundException {
        Object response = ois.readObject();
        if (response instanceof ResponseMsg) {
            return (ResponseMsg) response;
        }
        return null;
    }

    /**
     * 线程执行体
     */
    @Override
    public void run() {
        //与客户端建立连接
        try {
            socket = new Socket("127.0.0.1", Constants.port);
            socket.setSoTimeout(30000); // 30秒超时
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            // 连接成功后，先发送身份标识
            oos.writeObject(Constants.ADMIN);
            oos.flush();

            if (login()) {
                create_menu();
            } else {
                // 登录失败/取消，关闭资源
                closeResources();
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeResources(); // 连接失败时也关闭资源
        }
    }

    /**
     * 关闭资源
     */
    // 添加关闭资源的方法
    public void closeResources() {
        // 标记资源已关闭（可选，用于状态判断）
        boolean isClosed = false;

        // 关闭输入流
        if (ois != null) {
            try {
                ois.close();
                isClosed = true;
            } catch (IOException e) {
                System.out.println("输入流关闭失败：" + e.getMessage());
            }
            ois = null; // 置空，避免重复关闭
        }

        // 关闭输出流
        if (oos != null) {
            try {
                oos.close();
                isClosed = true;
            } catch (IOException e) {
                System.out.println("输出流关闭失败：" + e.getMessage());
            }
            oos = null; // 置空，避免重复关闭
        }

        // 关闭Socket（底层连接）
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                isClosed = true;
            } catch (IOException e) {
                System.out.println("Socket关闭失败：" + e.getMessage());
            }
            socket = null; // 置空，避免重复关闭
        }

        if (isClosed) {
            System.out.println("资源已关闭");
        }
    }

}
