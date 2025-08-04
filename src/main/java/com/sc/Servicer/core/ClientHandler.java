package com.sc.Servicer.core;

import com.sc.Servicer.utils.FileManager;
import com.sc.Servicer.utils.Validator;
import com.sc.client.admin_client.Admin;
import com.sc.client.student_client.Student;
import com.sc.client.teacher_client.Teacher;
import com.sc.common.Constants;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;
import com.sc.entity.ExamRecord;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.sc.Servicer.utils.FileManager.saveExamRecordMap;

/**
 * 处理客户端请求的线程
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
                }
            } else {
                oos.writeObject("身份格式错误，连接关闭");
                oos.flush();
                socket.close();
                System.out.println("流关闭");
            }
            // 第二步：根据身份执行不同的业务逻辑
            handleBusinessByRole();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("IOException | ClassNotFoundException");
        }

    }

    // 根据身份处理不同业务
    private void handleBusinessByRole() throws IOException, ClassNotFoundException {

        switch (role) {
            case Constants.ADMIN:
                handleAdminBusiness(); // 处理管理员业务
                break;
            case Constants.STUDENT:
                handleStudentBusiness(); // 处理学生业务
                break;
            case Constants.TEACHER:
                handleTeacherBusiness(); // 处理老师业务
                break;
        }

    }


    private void handleTeacherBusiness() throws IOException, ClassNotFoundException {
        // 教师登录逻辑
        while (true) {
            if (teacherLogin()) {
                oos.reset();
                oos.writeObject(new ResponseMsg("登录成功", 200));
                oos.flush();
                break;
            } else {
                oos.reset();
                oos.writeObject(new ResponseMsg("登录失败，请重试", 500));
                oos.flush();
            }
        }

        // 教师业务处理循环
        while (true) {
            Object o = ois.readObject();
            if (!(o instanceof RequestMsg)) {
                oos.reset();
                oos.writeObject(new ResponseMsg("请求格式错误", 500));
                oos.flush();
            } else {
                RequestMsg requestMsg = (RequestMsg) o;
                int cmd = requestMsg.getCmd();
                switch (cmd) {
                    case Constants.TEACHER_VIEW_PENDING_PAPERS: {
                        // 查看待批改试卷
                        handleViewPendingPapers();
                        break;
                    }
                    case Constants.TEACHER_GRADE_PAPER: {
                        // 批改试卷
                        handleGradePaper(requestMsg);
                        break;
                    }
                    case Constants.TEACHER_VIEW_GRADED_PAPERS: {
                        // 查看已批改试卷
                        handleViewGradedPapers();
                        break;
                    }

                }
            }
        }
    }

    private void handleViewGradedPapers() throws IOException, ClassNotFoundException {
        Object o = ois.readObject();
        if (!(o instanceof String)) {
            oos.reset();
            oos.writeObject(new ResponseMsg("请求格式错误", 500));
            oos.flush();
        }

        String id = (String) o;
        System.out.println("查看已批改试卷请求：" + id);
        File dir = new File(Constants.graded_paper_path);
        if (!dir.exists() || !dir.isDirectory()) {
            oos.reset();
            oos.writeObject(new ResponseMsg("目录不存在", 500));
            oos.flush();
            return;
        }
        // 获取所有文件
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            oos.reset();
            oos.writeObject(new ResponseMsg("没有已批改的试卷", 500));
            oos.flush();
            return;
        }

        //创建试卷信息列表
        Map<String,Object> paperInfoList = new HashMap<>();
        //创建试卷列表
        List<Map<String,String>> paperList = new ArrayList<>();
        int count = 0;
        System.out.println(count);
        for (File file : files){
            String fileName = file.getName();
            System.out.println(fileName);
            //解析文件名，以得到学生信息
            String[] parts = fileName.replace("_pending.txt", "").split("_");
            if (parts.length >= 5) {
                if (parts[4].equals(id)) {
                    Map<String,String> paperData = new HashMap<>();
                    paperData.put("学号", parts[0]);
                    paperData.put("姓名", parts[1]);
                    paperData.put("提交时间", parts[2]); // 这里已经是格式化的时间字符串
                    paperData.put("考试时长", parts[3]);
                    paperData.put("批改教师工号",parts[4]);
                    paperData.put("文件名", fileName);
                    paperList.add(paperData);
                    count++;
                }
            }
        }

        paperInfoList.put("已批改试卷数量", count);
        //试卷列表
        paperInfoList.put("试卷列表", paperList);
        oos.reset();
        oos.writeObject(new ResponseMsg("获取试卷列表成功",200,paperInfoList));
        oos.flush();
    }

    private void handleGradePaper(RequestMsg requestMsg) throws IOException {
        Object data = requestMsg.getData();
        if (!(data instanceof Map)) {
            oos.reset();
            oos.writeObject(new ResponseMsg("请求格式错误", 500));
            oos.flush();
        }
        Map<String, Object> dataMap = (Map<String, Object>) data;
        String gradedPaperFileName = (String) dataMap.get("文件名");
        String score = Double.toString((double)dataMap.get("分数"));
        Teacher teacher = (Teacher) dataMap.get("批改教师");
        String gradingTime = (String) dataMap.get("批改时间");

        File file = new File(Constants.student_submit_path + "/" + gradedPaperFileName);
        if (!file.exists()) {
            oos.reset();
            oos.writeObject(new ResponseMsg("文件不存在", 500));
            oos.flush();

        }
        StringBuilder content = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while ((line = br.readLine()) != null) {
                content.append( line).append("\n");
            }
        }catch (IOException e){
            oos.reset();
            oos.writeObject(new ResponseMsg("读取文件失败", 500));
            oos.flush();
        }
// 添加批改信息
        content.append("==========================================\n");
        content.append("得分: ").append(score).append("\n");
        content.append("批改时间: ").append(gradingTime).append("\n");
        content.append("批改教师: ").append(teacher.getName()).append("\n");
        String gradedFileName = gradedPaperFileName.replace("_pending.txt","_"+teacher.getId_card()+"_graded.txt");
        File gradedFile = new File(Constants.graded_paper_path+"/"+gradedFileName);
        // 将更新后的内容写入新的已批改文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(gradedFile))) {
            writer.write(content.toString());
        } catch (IOException e) {
            oos.reset();
            oos.writeObject(new ResponseMsg("保存批改结果失败: " + e.getMessage(), 500));
            oos.flush();
            return;
        }

        // 移动或删除原始待批改文件
        if (!file.delete()) {
            System.out.println("删除文件失败");
        }
        // 更新 ExamRecord 中的成绩信息
        String studentId = gradedPaperFileName.split("_")[0]; // 提取学生 ID
        ExamRecord examRecord = ExamServer.getExamRecordMap().get(studentId);
        if (examRecord != null) {
            examRecord.setScore(Double.parseDouble(score)); // 更新成绩
            try {
                FileManager.saveExamRecordMap(); // 保存更新后的考试记录
            } catch (Exception e) {
                System.out.println("保存考试记录失败: " + e.getMessage());
            }
        } else {
            System.out.println("未找到对应的考试记录");
        }
        // 返回成功响应
        oos.reset();
        oos.writeObject(new ResponseMsg("批改成功", 200));
        oos.flush();
    }

    private void handleViewPendingPapers() {
        try{
            File dir = new File(Constants.student_submit_path);
            if (!dir.exists() || !dir.isDirectory()) {
                oos.reset();
                oos.writeObject(new ResponseMsg("没有待批改的试卷", 500));
                oos.flush();
                return;
            }
            // 获取所有文件
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                oos.reset();
                oos.writeObject(new ResponseMsg("没有待批改的试卷", 500));
                oos.flush();
                return;
            }

            //创建试卷信息列表
            Map<String,Object> paperInfoList = new HashMap<>();
            paperInfoList.put("待批改试卷数量", files.length);
            //试卷列表
            List<Map<String,String>> paperList = new ArrayList<>();
            for (File file : files){
                Map<String,String> paperData = new HashMap<>();
                String fileName = file.getName();
                //解析文件名，以得到学生信息
                String[] parts = fileName.replace("_pending.txt", "").split("_");
                if (parts.length >= 4) {
                    paperData.put("学号", parts[0]);
                    paperData.put("姓名", parts[1]);
                    paperData.put("提交时间", parts[2]); // 这里已经是格式化的时间字符串
                    paperData.put("考试时长", parts[3]);
                    paperData.put("文件名", fileName);
                    paperList.add(paperData);
                }
            }
            paperInfoList.put("试卷列表", paperList);
            oos.reset();
            oos.writeObject(new ResponseMsg("获取试卷列表成功",200,paperInfoList));
            oos.flush();
        } catch (IOException e) {
            try {
                oos.reset();
                oos.writeObject(new ResponseMsg("获取待批改试卷列表失败: " + e.getMessage(), 500));
                oos.flush();
            } catch (IOException ioException) {
                System.out.println("发送错误响应失败: " + ioException.getMessage());
            }
        }
    }

    private boolean teacherLogin() throws IOException, ClassNotFoundException {
        Object o = ois.readObject();
        if (!(o instanceof RequestMsg)) {
            return false;
        } else {
            RequestMsg requestMsg = (RequestMsg) o;
            Teacher teacher = (Teacher) requestMsg.getData();
            if (ExamServer.getTeacherMap().containsKey(teacher.getId_card())) {
                if (ExamServer.getTeacherMap().get(teacher.getId_card()).getName()
                        .equals(teacher.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleStudentBusiness() throws IOException, ClassNotFoundException {
        while (true) {
            if (stuLogin()){
                oos.reset();
                oos.writeObject(new ResponseMsg("登录成功", 200));
                oos.flush();
                break;
            }else {
                oos.reset();
                oos.writeObject(new ResponseMsg("登录失败，请重试", 500));
                oos.flush();
            }
        }
        while (true) {
            Object o = ois.readObject();
            if (!(o instanceof RequestMsg)) {
                oos.reset();
                oos.writeObject(new ResponseMsg("请求格式错误", 500));
                oos.flush();
            } else {
                RequestMsg requestMsg = (RequestMsg) o;
                int cmd = requestMsg.getCmd();
                switch (cmd) {
                    case Constants.STUDENT_BEGIN_EXAM:{
                        Student student = (Student) requestMsg.getData();
                        StuBeginExam(student);
                        break;
                    }
                    case Constants.STUDENT_SUBMIT: {
                        Map<String, Object> submissionData = (Map<String, Object>) requestMsg.getData();
                        handleStudentSubmit(submissionData); // 处理提交
                        break;
                    }
                    case Constants.STUDENT_GET_SCORE: {
                        Student student = (Student) requestMsg.getData();
                        handleStudentGetScore(student);
                        break;
                    }
                }
            }
        }
    }

    private void handleStudentGetScore(Student student) throws IOException {
        ExamRecord examRecord = ExamServer.getExamRecordMap().get(student.getId_card());
        if (examRecord != null) {
            oos.reset();
            oos.writeObject(new ResponseMsg("获取数据成功", 200, examRecord));
            oos.flush();
        }
    }

    private void handleStudentSubmit(Map<String, Object> submissionData) throws IOException {
        try {
            // 从提交数据中获取信息
            Student student = (Student) submissionData.get("student");
            String answerContent = (String) submissionData.get("answerContent");
            long submitTime = (Long) submissionData.get("submitTime");
            String fileName = (String) submissionData.get("fileName");

            // 验证必要数据
            if (student == null || answerContent == null || answerContent.trim().isEmpty()) {
                oos.reset();
                oos.writeObject(new ResponseMsg("提交数据不完整", 500));
                oos.flush();
                return;
            }
            // 获取考试开始时间
            long beginTime = 0;
            ExamRecord examRecord = ExamServer.getExamRecordMap().get(student.getId_card());
            if (examRecord != null) {
                examRecord.setSubmit_status(true);
                examRecord.setSubmit_time(new Date(submitTime).toString());
                examRecord.setSubmitTimeMillis(submitTime);
                beginTime = examRecord.getBeginTimeMillis();
            } else {
                // 考试记录不存在的处理
                System.out.println("警告: 未找到学生 " + student.getId_card() + " 的考试记录");
                // 可以选择返回错误信息给客户端
                oos.reset();
                oos.writeObject(new ResponseMsg("未找到考试记录，提交失败", 500));
                oos.flush();
                return;
            }
            // 计算考试时长（毫秒）
            long durationMillis = submitTime - beginTime;
            // 转换为分钟
            long durationMinutes = durationMillis / (1000 * 60);
            // 生成格式化的时间字符串用于文件名
            String beginTimeStr = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(beginTime));


            // 保存更新后的考试记录
            try {
                FileManager.saveExamRecordMap();
            } catch (Exception e) {
                System.out.println("更新考试记录失败: " + e.getMessage());
            }
            // 创建答案保存目录
            File answerDir = new File(Constants.student_submit_path);
            if (!answerDir.exists()) {
                answerDir.mkdirs();
            }

// 生成唯一的答案文件名（使用格式化的时间字符串）
            String uniqueFileName = student.getId_card() + "_" + student.getName() + "_" +
                    beginTimeStr + "_" + durationMinutes + "min_pending.txt";
            File answerFile = new File(answerDir, uniqueFileName);

            // 保存答案内容到文件
            try (FileWriter writer = new FileWriter(answerFile)) {
                writer.write("学生姓名: " + student.getName() + "\n");
                writer.write("准考证号: " + student.getId_card() + "\n");
                writer.write("提交时间: " + new java.util.Date(submitTime).toString() + "\n");
                writer.write("==========================================\n\n");
                writer.write(answerContent);
            }

            // 记录提交日志
            System.out.println("学生 " + student.getName() + " (" + student.getId_card() + ") 提交了试卷，保存至: " + answerFile.getAbsolutePath());

            // 返回成功响应
            oos.reset();
            oos.writeObject(new ResponseMsg("试卷提交成功，已保存至服务器", 200));
            oos.flush();

        } catch (Exception e) {
            System.out.println("处理学生提交时发生异常: " + e.getMessage());
            oos.reset();
            oos.writeObject(new ResponseMsg("提交处理失败: " + e.getMessage(), 500));
            oos.flush();
        }
    }

    private void StuBeginExam(Student student) throws IOException, ClassNotFoundException {
        //读取试卷
        File paperDir = new File(Constants.paper_path);
        if (!paperDir.exists() || !paperDir.isDirectory()) {
            oos.reset();
            oos.writeObject(new ResponseMsg("试卷目录不存在", 500));
            oos.flush();
            return;
        }
        File[] paperFiles = paperDir.listFiles();
        if (paperFiles == null || paperFiles.length == 0) {
            oos.reset();
            oos.writeObject(new ResponseMsg("没有试卷", 500));
            oos.flush();
            return;
        }
        //随机一张试卷
        Random random = new Random();
        File paperFile = paperFiles[random.nextInt(paperFiles.length)];


        // 读取试卷内容
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(paperFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            oos.reset();
            oos.writeObject(new ResponseMsg("读取试卷失败", 500));
            oos.flush();
            return;
        }
        long startTime = System.currentTimeMillis();

        ExamRecord examRecord = new ExamRecord(student, new Date(startTime).toString());
        examRecord.setBeginTimeMillis(startTime);
        examRecord.setSubmit_status(false); // 初始状态为未提交
        ExamServer.getExamRecordMap().put(student.getId_card(), examRecord);

        // 尝试保存考试记录到文件
        try {
            FileManager.saveExamRecordMap();
        } catch (Exception e) {
            System.out.println("保存考试记录失败: " + e.getMessage());
        }
        // 将试卷内容和开始时间发送给学生
        Map<String, Object> examData = new HashMap<>();
        examData.put("paperContent", sb.toString());
        examData.put("startTime", startTime);
        examData.put("paperName", paperFile.getName());

        oos.reset();
        oos.writeObject(new ResponseMsg("考试开始", 200, examData));
        oos.flush();
        // 开启考试计时器
        startExamTimer(startTime, student);
    }
    private void startExamTimer(long startTime, Student student) {
        // 创建一个新线程用于计时和发送提醒
        Thread timerThread = new Thread(() -> {
            try {
                // 计算提醒时间点（考试开始后一定时间）
                long examDurationMillis = Constants.EXAM_DURATION_MINUTES * 60 * 1000L;
                long reminderTimeMillis = Constants.REMINDER_MINUTES * 60 * 1000L;
                long reminderTime = startTime + (examDurationMillis - reminderTimeMillis);

                // 等待到提醒时间
                long currentTime = System.currentTimeMillis();
                if (currentTime < reminderTime) {
                    Thread.sleep(reminderTime - currentTime);

                    // 发送提醒信息
                    oos.reset();
                    oos.writeObject(new ResponseMsg("考试提醒", 201, "距离考试结束还有30分钟"));
                    oos.flush();
                }

                // 等待到考试结束时间
                long endTime = startTime + examDurationMillis;
                currentTime = System.currentTimeMillis();
                if (currentTime < endTime) {
                    Thread.sleep(endTime - currentTime);

                    // 发送考试结束信息
                    oos.reset();
                    oos.writeObject(new ResponseMsg("考试结束", 202, "考试时间已到，请停止答题并提交"));
                    oos.flush();
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("计时器线程异常: " + e.getMessage());
            }
        });

        timerThread.setDaemon(true); // 设置为守护线程，主连接关闭时自动结束
        timerThread.start();
    }

    private boolean stuLogin() throws IOException, ClassNotFoundException {
        Object o = ois.readObject();//得到请求
        if (!(o instanceof RequestMsg)) {
            return false;
        }else {
            RequestMsg requestMsg = (RequestMsg) o;
            Student student = (Student) requestMsg.getData();
            if (ExamServer.getStudentMap().containsKey(student.getId_card())) {
                if (ExamServer.getStudentMap().get(student.getId_card()).getName().equals(student.getName())){
                    //名字和id_card都匹配
                    return true;
                }
            }
            return false;
        }
    }

    //以下处理管理员端
    private void handleAdminBusiness() throws IOException, ClassNotFoundException {
        // 登录循环：允许客户端多次重试
        while (true) {
            if (adminLogin()) { // 登录成功
                oos.writeObject(new ResponseMsg("登录成功", 200));
                oos.flush();
                break; // 跳出登录循环，进入业务处理
            } else {
                // 登录失败，不退出，继续等待客户端发送新的登录请求
                oos.writeObject(new ResponseMsg("登录失败，请重试", 500));
                oos.flush();
            }
        }
        //循环接收请求
        while (true) {

            Object o = ois.readObject();
            if (!(o instanceof RequestMsg)) {
                oos.reset();
                oos.writeObject(new ResponseMsg("请求格式错误", 500));
                oos.flush();
            } else {
                RequestMsg requestMsg = (RequestMsg) o;
                int cmd = requestMsg.getCmd();
                /*
                 * 这里处理管理员不同的任务添加，删除
                 */
                switch (cmd) {
                    case Constants.ADMIN_ADD_STU: {
                        Student student = (Student) requestMsg.getData();
                        adminAddStu(student);
                        break;
                    }
                    case Constants.ADMIN_ADD_TEA: {
                        Teacher teacher = (Teacher) requestMsg.getData();
                        adminAddTea(teacher);
                        break;
                    }
                    case Constants.ADMIN_DEL_STU:{
                        String id = (String) requestMsg.getData();
                        oos.reset();
                        if (adminDelStu(id)) {
                            oos.writeObject(new ResponseMsg("删除成功",200));
                            oos.flush();
                        }else {
                            oos.writeObject(new ResponseMsg("未查找到用户",400));
                            oos.flush();
                        }
                        break;
                    }
                    case Constants.ADMIN_DEL_TEA:{
                        String id = (String) requestMsg.getData();
                        oos.reset();
                        if (adminDelTea(id)) {
                            oos.writeObject(new ResponseMsg("删除成功",200));
                            oos.flush();
                        }else {
                            oos.writeObject(new ResponseMsg("未查找到用户",400));
                            oos.flush();
                        }
                        break;
                    }
                    case Constants.ADMIN_GET_ALL_STU:{
                        oos.reset();
                        if (ExamServer.getStudentMap().isEmpty()) {
                            oos.writeObject(new ResponseMsg("暂无学生信息",500));
                            oos.flush();
                            break;
                        }
                        oos.writeObject(new ResponseMsg("查询成功",200,ExamServer.getStudentMap()));
                        oos.flush();
                        break;
                    }
                    case Constants.ADMIN_GET_ALL_TEA:{
                        oos.reset();
                        if (ExamServer.getTeacherMap().isEmpty()) {
                            oos.writeObject(new ResponseMsg("暂无教师信息",500));
                            oos.flush();
                            break;
                        }
                        oos.writeObject(new ResponseMsg("查询成功",200,ExamServer.getTeacherMap()));
                        oos.flush();
                        break;
                    }
                }
            }

        }
    }

    private boolean adminDelTea(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (!ExamServer.getTeacherMap().containsKey(id)){
            return false;
        }
        ExamServer.getTeacherMap().remove(id);
        return true;
    }

    private boolean adminDelStu(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (!ExamServer.getStudentMap().containsKey(id)){
            return false;
        }
        ExamServer.getStudentMap().remove(id);
        return true;

    }

    private void adminAddTea(Teacher teacher) throws IOException {
        if (teacher == null) {
            oos.reset();
            oos.writeObject(new ResponseMsg("教师不能为空", 500));
            oos.flush();
            return;
        }
        String tea_id = teacher.getId_card();
        if (ExamServer.getTeacherMap().containsKey(tea_id)) {
            oos.reset();
            oos.writeObject(new ResponseMsg("工号已经存在", 500));
            oos.flush();
            return;
        }
        //添加

        ExamServer.getTeacherMap().put(tea_id, teacher);
        System.out.println(ExamServer.getTeacherMap());
        oos.reset();
        oos.writeObject(new ResponseMsg("添加教师成功", 200));
        oos.flush();

        // 添加异常处理
        try {
            FileManager.saveTeacherMap();
        } catch (IOException e) {
            System.out.println("保存教师数据到文件失败: " + e.getMessage());
            // 可以选择是否向客户端发送错误信息
        }
    }

    private void adminAddStu(Student student) throws IOException {
        if (student == null) {
            oos.reset();
            oos.writeObject(new ResponseMsg("学生不能为空", 500));
            oos.flush();
            return;
        }
        String stu_id = student.getId_card();
        if (ExamServer.getStudentMap().containsKey(stu_id)) {
            oos.reset();
            oos.writeObject(new ResponseMsg("学号已经存在", 500));
            oos.flush();
            return;
        }
        //添加
        ExamServer.getStudentMap().put(stu_id, student);
        System.out.println(ExamServer.getStudentMap());
        oos.reset();
        oos.writeObject(new ResponseMsg("添加学生成功", 200));
        oos.flush();

        // 添加异常处理
        try {
            FileManager.saveStudentMap();
        } catch (IOException e) {
            System.out.println("保存学生数据到文件失败: " + e.getMessage());
            // 可以选择是否向客户端发送错误信息
        }
    }

    // 1. 管理员登录处理（独立方法）
    private boolean adminLogin() throws IOException, ClassNotFoundException {

        // 读取客户端发送的登录请求（包含管理员账号密码）
        Object obj = ois.readObject();
        if (!(obj instanceof RequestMsg)) {
            oos.reset();
            oos.writeObject(new ResponseMsg("登录请求格式错误", 500));
            oos.flush();
            return false;
        }

        RequestMsg loginRequest = (RequestMsg) obj;
        if (loginRequest.getCmd() != Constants.ADMIN_LOGIN) {
            oos.reset();
            oos.writeObject(new ResponseMsg("请先登录", 500));
            oos.flush();
            return false;
        }

        // 验证账号密码
        Admin admin = (Admin) loginRequest.getData();
        System.out.println(admin.getName() + " " + admin.getPass());
        return Constants.name.equals(admin.getName()) && Constants.pass.equals(admin.getPass());
    }
}
