package com.sc.client.student_client;

import com.sc.client.Client;
import com.sc.common.Constants;
import com.sc.common.InputHandler;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;
import com.sc.entity.ExamRecord;
import com.sc.entity.Student;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生客户端
 */
public class StudentClient implements Client ,Runnable{
    public InputHandler handler = new InputHandler();
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private Socket socket;
    private Student student;

    private Thread examListenerThread;
    private boolean isExamming = false;
    // 同时优化create_menu方法中的异常处理
    @Override
    public void create_menu() throws IOException, ClassNotFoundException {
        while (true) {
            try {
                System.out.println("\n欢迎来到学生端");
                System.out.println("1.开始考试");
                System.out.println("2.交卷");
                System.out.println("3.查看成绩");
                System.out.println("4.返回上一级");
                int num = handler.getIntInput("请输入命令：");
                switch (num){
                    case 1:{
                        beginExam();
                        break;
                    }
                    case 2:{
                        submitPaper();
                        break;
                    }
                    case 3:{
                        getScore();
                        break;
                    }
                    case 4:{
                        return;
                    }
                    default:
                        printFancyMessage("请输入有效的命令编号(1-4)", "info");
                }
            } catch (Exception e) {
                printFancyMessage("操作出错: " + e.getMessage(), "error");
                // 保留原始异常堆栈用于调试
                e.printStackTrace();
            }
        }
    }

    private void getScore() throws IOException, ClassNotFoundException {
        sendMsg(new RequestMsg(Constants.STUDENT_GET_SCORE, student));
        ResponseMsg responseMsg = receiveMsg();

        if (responseMsg != null && responseMsg.getCode() == 200) {
            ExamRecord record = (ExamRecord) responseMsg.getData();
            if (record == null) {
                printFancyMessage("未找到考试记录", "info");
                return;
            }

            // 未提交试卷
            if (!record.isSubmit_status()) {
                printFancyMessage("尚未提交试卷，无法查询成绩", "info");
                return;
            }

            // 已提交但未批改
            if (!record.isGraded()) {
                printFancyMessage("试卷已提交，等待教师批改中...", "info");
                return;
            }

            // 已批改，显示成绩
            String message = String.format(
                    "考试成绩：\n姓名：%s\n学号：%s\n提交时间：%s\n成绩：%.1f分",
                    student.getName(), student.getId_card(),
                    record.getSubmit_time(), record.getScore()
            );
            printFancyMessage(message, "success");
        } else if (responseMsg != null && responseMsg.getCode() == 500) {
            printFancyMessage("查询成绩失败：" + responseMsg.getMessage(), "error");
        } else {
            printFancyMessage("服务器响应异常", "error");
        }
    }


    private void submitPaper() throws IOException, InterruptedException {
        // 检查是否在考试中
        if (!isExamming) {
            System.out.println("当前不在考试中，无法提交试卷");
            return;
        }

        // 1. 生成文件名（保持不变）
        String answerFileName = student.getName() + "_" + student.getId_card() + ".txt";

        // 2. 关键：每次检查前重新构建File对象，避免缓存旧状态
        File answerFile = new File(Constants.answer_path, answerFileName);
        System.out.println("检查的文件路径：" + answerFile.getAbsolutePath());

        // 3. 添加重试机制，给用户时间创建文件，并每次重新检查
        int maxRetries = 3;
        int retryCount = 0;
        boolean fileValid = false;

        while (retryCount < maxRetries) {
            // 每次循环都重新创建File对象，确保状态最新
            answerFile = new File(Constants.answer_path, answerFileName);

            // 检查文件是否存在
            if (!answerFile.exists()) {
                System.out.println("第" + (retryCount + 1) + "次检查：文件不存在，请创建文件");
            }
            // 检查文件是否为空（使用length()判断，避免缓存）
            else if (answerFile.length() == 0) {
                System.out.println("第" + (retryCount + 1) + "次检查：文件为空，请写入内容");
            }
            // 检查是否有读取权限
            else if (!answerFile.canRead()) {
                System.out.println("第" + (retryCount + 1) + "次检查：无读取权限");
            } else {
                // 文件状态正常
                fileValid = true;
                break;
            }

            // 等待3秒后重试（给用户操作时间）
            retryCount++;
            if (retryCount < maxRetries) {
                System.out.println("3秒后重试...（剩余" + (maxRetries - retryCount) + "次）");
                Thread.sleep(3000);
            }
        }

        if (!fileValid) {
            System.out.println("文件检查失败，无法提交");
            return;
        }
        // 检查答案文件是否存在
        if (!answerFile.exists() || answerFile.length() == 0) {
            System.out.println("答案文件不存在或为空，请确认文件路径：" + answerFile.getAbsolutePath());
            return;
        }

        try {
            // 读取答案文件内容（核心：将文件内容转为字符串传输，而非File对象）
            String answerContent = readAnswerFile(answerFile);
            if (answerContent == null || answerContent.trim().isEmpty()) {
                System.out.println("答案内容为空，无法提交");
                return;
            }

            // 构建提交数据（包含必要信息和答案内容）
            Map<String, Object> submissionData = new HashMap<>();
            submissionData.put("student", student);         // 学生信息
            submissionData.put("submitTime", System.currentTimeMillis());  // 提交时间戳
            submissionData.put("answerContent", answerContent);  // 答案内容（关键）
            submissionData.put("fileName", answerFileName);      // 文件名（用于服务器记录）

            // 发送提交请求
            System.out.println("正在提交试卷...");
            sendMsg(new RequestMsg(Constants.STUDENT_SUBMIT, submissionData));

            // 等待服务器响应（设置超时，避免无限等待）
            ResponseMsg responseMsg;
            try {
                responseMsg = receiveMsg();  // 使用默认超时（30秒）
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("提交成功！" + responseMsg.getMessage());
                        isExamming = false;  // 提交成功后更新考试状态
                        stopListeningForExamMessages();
                    } else {
                        System.out.println("提交失败：" + responseMsg.getMessage());
                    }
                } else {
                    System.out.println("未收到服务器响应，提交状态未知");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("提交超时，请检查网络后重试");
            } catch (ClassNotFoundException e) {
                System.out.println("服务器响应格式错误：" + e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("读取答案文件失败：" + e.getMessage());
        }
    }

    // 辅助方法：读取答案文件内容为字符串
    private String readAnswerFile(File file) throws IOException {
        // 使用缓冲流高效读取文本内容
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");  // 保留换行符
            }
            return content.toString().trim();  // 去除首尾空白
        }
    }


    private void beginExam() throws IOException, ClassNotFoundException {
        if (isExamming) {
            System.out.println("当前已处于考试中，请勿重复开始");
            return;
        }

        sendMsg(new RequestMsg(Constants.STUDENT_BEGIN_EXAM, student));

        try {
            ResponseMsg responseMsg = receiveMsg();
            if (responseMsg != null && responseMsg.getCode() == 200) {
                processExamStart(responseMsg);
            } else if (responseMsg != null) {
                System.out.println("开始考试失败：" + responseMsg.getMessage());
            }
        } catch (SocketTimeoutException e) {
            System.out.println("服务器响应超时，请重试");
        }
    }

    private void processExamStart(ResponseMsg responseMsg) {
        // 添加类型检查
        Object data = responseMsg.getData();
        if (!(data instanceof Map)) {
            System.out.println("服务器返回数据格式错误，期望Map类型，实际收到: " +
                    (data != null ? data.getClass().getName() : "null"));
            return;
        }
        Map<String, Object> examData = (Map<String, Object>) responseMsg.getData();
        String paperContent = (String) examData.get("paperContent");
        Long startTime = (Long) examData.get("startTime");
        String paperName = (String) examData.get("paperName");

        System.out.println("考试开始！试卷：" + paperName);
        System.out.println("开始时间：" + new java.util.Date(startTime));

        Thread savePaperThread = new Thread(() -> savePaperToLocalFile(paperName, paperContent));
        savePaperThread.start();

        System.out.println("试卷内容：");
        System.out.println(paperContent);
        isExamming = true;

        listenForExamMessages();

        try {
            savePaperThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void savePaperToLocalFile(String paperName, String paperContent) {
        try {
            // 创建试卷保存目录
            File paperDir = new File(Constants.student_paper_path);
            if (!paperDir.exists()) {
                paperDir.mkdirs();
            }


            File paperFile = new File(paperDir, paperName);
            if (paperFile.exists()) {
                return;
            }

            // 写入试卷内容
            try (FileWriter writer = new FileWriter(paperFile)) {
                writer.write(paperContent);
            }

            System.out.println("\n试卷已保存到: " + paperFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("\n保存试卷到本地文件失败: " + e.getMessage());
        }
    }
    private void listenForExamMessages() {
        examListenerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (socket == null || socket.isClosed()) {
                        printFancyMessage("连接已断开，停止监听考试消息", "info");
                        break;
                    }

                    try {
                        ResponseMsg responseMsg = receiveMsg(0);
                        // 若读取到null，说明服务器主动关闭连接
                        if (responseMsg == null) {
                            printFancyMessage("服务器已关闭连接", "info");
                            break;
                        }
                        handleExamMessage(responseMsg);
                    } catch (SocketTimeoutException e) {
                        System.out.println("已经超时");
                        continue; // 超时不处理，继续监听
                    } catch (EOFException e) {
                        // 捕获“流结束”异常（连接正常关闭），不提示错误
                        printFancyMessage("连接已正常关闭", "info");
                        break;
                    } catch (StreamCorruptedException | OptionalDataException e) {
                        // 仅在调试时输出，生产环境可忽略或重新连接

                        break;
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("其他通信错误：" + e.getMessage());
                        break;
                    }
                }
            } finally {
                // 仅在正常关闭时提示，异常关闭不提示
                if (!Thread.currentThread().isInterrupted()) {
                    printFancyMessage("考试监听已停止", "info");
                }
            }
        });
        examListenerThread.setDaemon(true);
        examListenerThread.start();
    }

    // 添加一个美化消息输出的工具方法
    private void printFancyMessage(String message, String type) {
        String line = "======================================";
        String prefix = "【信息】";

        if ("error".equals(type)) {
            prefix = "【错误】";
        } else if ("success".equals(type)) {
            prefix = "【成功】";
        }

        System.out.println("\n" + line);
        System.out.println(prefix + " " + message);
        System.out.println(line + "\n");
    }
    private void handleExamMessage(ResponseMsg responseMsg) {
        switch (responseMsg.getCode()) {
            case 201: // 考试提醒
                System.out.println("\n=== 考试提醒 ===");
                System.out.println(responseMsg.getData());
                System.out.println("===============");
                break;
            case 202: // 考试结束
                System.out.println("\n=== 考试结束 ===");
                System.out.println(responseMsg.getData());
                System.out.println("===============");
                return;
            default:
                System.out.println("收到未知消息: " + responseMsg.getMessage());
                break;
        }
    }

    // 中断监听线程的方法
    private void stopListeningForExamMessages() {
        if (examListenerThread != null && examListenerThread.isAlive()) {
            examListenerThread.interrupt();
        }
    }

    @Override
    public boolean login() {
        try{
            while (true) {
                String name = handler.getNonEmptyStringInput("请输入姓名：");
                String id = handler.getNonEmptyStringInput("请输入准考证：");
                Student student = new Student(name,id);
                //发送请求
                sendMsg(new RequestMsg(Constants.STUDENT_LOGIN,student));

                ResponseMsg responseMsg;
                try {
                    responseMsg = receiveMsg();
                }catch (SocketTimeoutException e) {
                    System.out.println("服务器响应超时，请重试");
                    return false; // 或其他处理
                }
                if (responseMsg != null) {
                    if (responseMsg.getCode() == 200) {
                        System.out.println("登录成功");
                        this.student = student;
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
        } catch (IOException e) {
            System.out.println("登录失败：" + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("类型异常");
        }

        return false;
    }

    @Override
    public void
    sendMsg(RequestMsg requestMsg) throws IOException {
        oos.reset(); // 重置序列化缓存
        oos.writeObject(requestMsg);
        oos.flush();
    }

    @Override
    public ResponseMsg receiveMsg() throws IOException, ClassNotFoundException,SocketTimeoutException{
        Object response = ois.readObject();
        if (response instanceof ResponseMsg) {
            return (ResponseMsg) response;
        }
        return null;
    }
    public ResponseMsg receiveMsg(int timeout) throws IOException, ClassNotFoundException {
        // 添加空值检查
        if (socket == null) {
            throw new IOException("Socket连接未建立或已关闭");
        }

        int originalTimeout = socket.getSoTimeout();
        try {
            socket.setSoTimeout(timeout);
            Object response = ois.readObject();
            if (response instanceof ResponseMsg) {
                return (ResponseMsg) response;
            }
            return null;
        } finally {
            // 恢复原始超时设置
            socket.setSoTimeout(originalTimeout);
        }
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
    public void run() { //线程
        try {
            // 修复：使用成员变量而不是创建局部变量
            this.socket = new Socket("127.0.0.1", Constants.port);
            this.socket.setSoTimeout(30000);
            oos = new ObjectOutputStream(this.socket.getOutputStream());
            ois = new ObjectInputStream(this.socket.getInputStream());
            // 连接成功后，先发送身份标识
            oos.writeObject(Constants.STUDENT);
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
