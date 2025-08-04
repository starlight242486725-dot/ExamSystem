package com.sc.client.teacher_client;

import com.sc.client.Client;
import com.sc.common.Constants;
import com.sc.common.InputHandler;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TeacherClient implements Client, Runnable {
    public InputHandler handler = new InputHandler();
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Socket socket;
    private Teacher teacher;
    private Map<String, Object> data =null;
    private Map<String,Object> gradedPaper = new HashMap<>();

    @Override
    public void create_menu() throws IOException, ClassNotFoundException {
        while (true) {
            System.out.println("欢迎来到教师端");
            System.out.println("1. 查看待批改试卷");
            System.out.println("2. 批改学生答卷");
            System.out.println("3. 查看已批改试卷");
            System.out.println("4. 返回上一级");
            int num = handler.getIntInput("请输入命令：");
            switch (num) {
                case 1:
                    viewPendingPapers();
                    break;
                case 2:
                    gradeStudentPapers();
                    break;
                case 3:
                    viewGradedPapers();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("输入命令有误");
            }
        }
    }

    private boolean viewPendingPapers() {
        try {
            // 发送请求
            sendMsg(new RequestMsg(Constants.TEACHER_VIEW_PENDING_PAPERS, null));

            // 接收服务器响应
            ResponseMsg responseMsg = receiveMsg();

            if (responseMsg != null) {
                if (responseMsg.getCode() == 200) {
                    if (responseMsg.getData() != null) {
                        data = (Map<String, Object>) responseMsg.getData();

                        // 检查数据中是否包含必要的键
                        if (data.containsKey("待批改试卷数量") && data.containsKey("试卷列表")) {
                            int paperCount = (int) data.get("待批改试卷数量");

                            if (paperCount == 0) {
                                System.out.println("暂无待批改试卷");
                            }

                            System.out.println("=== 待批改试卷列表 ===");
                            System.out.println("总计: " + paperCount + " 份试卷");
                            // 修改表头，添加考试时长列
                            System.out.println("-------------------------------------------------------------------------");
                            System.out.printf("%-5s %-15s %-10s %-20s %-10s %-20s%n", "序号", "学生姓名", "学号", "提交时间", "考试时长", "文件名");
                            System.out.println("-------------------------------------------------------------------------");

                            List<Map<String, String>> papers = (List<Map<String, String>>) data.get("试卷列表");
                            if (papers != null && !papers.isEmpty()) {
                                for (int i = 0; i < papers.size(); i++) {
                                    Map<String, String> paper = papers.get(i);
                                    if (paper != null) {
                                        String studentName = paper.getOrDefault("姓名", "未知");
                                        String studentId = paper.getOrDefault("学号", "未知");
                                        String timestamp = paper.getOrDefault("提交时间", "未知");
                                        // 获取考试时长信息
                                        String duration = paper.getOrDefault("考试时长", "未知");
                                        String fileName = paper.getOrDefault("文件名", "未知");

                                        System.out.printf("%-5d %-15s %-10s %-20s %-10s %-20s%n",
                                                i + 1,
                                                studentName,
                                                studentId,
                                                timestamp,
                                                duration,
                                                fileName);
                                    }
                                }
                            }
                            System.out.println("-------------------------------------------------------------------------");
                            return true;
                        } else {
                            System.out.println("返回数据格式不正确");
                        }
                    } else {
                        System.out.println("暂无待批改试卷");
                    }
                } else {
                    System.out.println("获取待批改试卷列表失败: " + responseMsg.getMessage());
                }
            } else {
                System.out.println("服务器无响应");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("查看待批改试卷时发生异常: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassCastException e) {
            System.out.println("数据类型转换错误: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    private void viewGradedPapers() throws IOException, ClassNotFoundException {// 查看已批改试卷的逻辑
        sendMsg(new RequestMsg(Constants.TEACHER_VIEW_GRADED_PAPERS, null));
        sendMsg(teacher.getId_card());
        // 接收服务器响应
        ResponseMsg responseMsg = receiveMsg();
        if (responseMsg != null) {
            if (responseMsg.getCode() == 200) {
                if (responseMsg.getData() != null) {
                    data = (Map<String, Object>) responseMsg.getData();
                    if (data.containsKey("已批改试卷数量") && data.containsKey("试卷列表")) {
                        int gradedPaperCount = (int) data.get("已批改试卷数量");
                        if (gradedPaperCount == 0) {
                            System.out.println("暂无已批改试卷");
                        }
                        System.out.println("=== 已批改试卷列表 ===");
                        System.out.println("总计: " + gradedPaperCount + " 份试卷");
                        System.out.println("-------------------------------------------------------------------------");
                        System.out.printf("%-5s %-15s %-10s %-20s %-10s %-10s %-20s%n", "序号", "学生姓名", "学号", "提交时间","批改教师", "考试时长", "文件名");
                        System.out.println("-------------------------------------------------------------------------");
                        List<Map<String, String>> gradedPapers = (List<Map<String, String>>) data.get("试卷列表");
                        if (gradedPapers != null && !gradedPapers.isEmpty()) {
                            for (int i = 0; i < gradedPapers.size(); i++){
                                Map<String, String> paper = gradedPapers.get(i);
                                if (paper != null) {
                                    String studentName = paper.getOrDefault("姓名", "未知");
                                    String studentId = paper.getOrDefault("学号", "未知");
                                    String timestamp = paper.getOrDefault("提交时间", "未知");
                                    String teacherName = teacher.getName();
                                    String duration = paper.getOrDefault("考试时长", "未知");
                                    String fileName = paper.getOrDefault("文件名", "未知");
                                    System.out.printf("%-5s %-15s %-10s %-20s %-10s %-10s %-20s%n",
                                            i + 1,
                                            studentName,
                                            studentId,
                                            timestamp,
                                            teacherName,
                                            duration,
                                            fileName
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void gradeStudentPapers() throws IOException, ClassNotFoundException {
        // 查看待批改试卷的逻辑
        if (viewPendingPapers()) {//内部已经校验了data的内容
            int num = handler.getIntInput("请输入待批改试卷序号：");
            while (true) {
                if (num <= 0 || num > (int) data.get("待批改试卷数量")) {
                    System.out.println("输入序号有误");
                    num = handler.getIntInput("请重新输入待批改试卷序号：");
                }else {
                    break;
                }
            }
            List<Map<String, String>> papers = (List<Map<String, String>>) data.get("试卷列表");
            Map<String, String> paper = papers.get(num - 1);
            System.out.println("选中的试卷: " + paper);
            //批改的试卷
            String fileName = paper.get("文件名");

            //分数
            double score = handler.getDoubleInput("请输入分数：");
            //批改时间
            String time = new Date(System.currentTimeMillis()).toString();
            Map<String, Object> gradedPaper = new HashMap<>();
            gradedPaper.put("文件名", fileName);
            gradedPaper.put("分数", score);
            gradedPaper.put("批改教师", teacher);
            gradedPaper.put("批改时间", time);
            oos.reset();
            oos.writeObject(new RequestMsg(Constants.TEACHER_GRADE_PAPER,gradedPaper));
            oos.flush();
            // 接收服务器响应
            ResponseMsg responseMsg = receiveMsg();
            if (responseMsg != null) {
                if (responseMsg.getCode() == 200) {
                    System.out.println("批改成功");
                } else {
                    System.out.println("批改失败: " + responseMsg.getMessage());
                }
            } else {
                System.out.println("服务器无响应");
            }
        }


    }

    @Override
    public boolean login() {
        try {
            while (true) {
                String name = handler.getNonEmptyStringInput("请输入教师姓名：");
                String id = handler.getNonEmptyStringInput("请输入教师编号：");
                Teacher teacher = new Teacher(name, id);
                //发送请求
                sendMsg(new RequestMsg(Constants.TEACHER_LOGIN, teacher));

                ResponseMsg responseMsg;
                try {
                    responseMsg = receiveMsg();
                } catch (SocketTimeoutException e) {
                    System.out.println("服务器响应超时，请重试");
                    return false; // 或其他处理
                }
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("登录成功");
                        this.teacher = teacher;
                        return true;
                    } else {
                        System.out.println(responseMsg.getMessage());
                        String isContinue = handler.getNonEmptyStringInput("是否继续(y/n):");
                        if (!"y".equalsIgnoreCase(isContinue)) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    @Override
    public void sendMsg(RequestMsg requestMsg) throws IOException {
        oos.reset(); // 重置序列化缓存
        oos.writeObject(requestMsg);
        oos.flush();
    }
    public void sendMsg(String str) throws IOException {
        oos.reset(); // 重置序列化缓存
        oos.writeObject(str);
        oos.flush();
    }

    @Override
    public ResponseMsg receiveMsg() throws IOException, ClassNotFoundException {
        Object response = ois.readObject();
        if (response instanceof ResponseMsg) {
            return (ResponseMsg) response;
        }
        return null;
    }

    @Override
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

    @Override
    public void run() {
        try {
            this.socket = new Socket("127.0.0.1", Constants.port);
            this.socket.setSoTimeout(30000);
            this.oos = new ObjectOutputStream(this.socket.getOutputStream());
            this.ois = new ObjectInputStream(this.socket.getInputStream());
            //发送身份标识
            oos.writeObject(Constants.TEACHER);
            oos.flush();
            if (login()) {
                create_menu();
            } else {
                closeResources();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("连接失败：" + e.getMessage());
            closeResources(); // 发生异常时也要关闭资源
        }
    }
}
