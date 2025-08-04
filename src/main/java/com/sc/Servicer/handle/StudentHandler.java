package com.sc.Servicer.handle;

import com.sc.Servicer.core.ExamServer;
import com.sc.Servicer.utils.FileManager;
import com.sc.entity.Student;
import com.sc.common.Constants;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;
import com.sc.entity.ExamRecord;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudentHandler extends BaseHandler {

    public StudentHandler(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        super(socket, oos, ois);
    }

    @Override
    public void handleBusiness() throws IOException, ClassNotFoundException {
        // 登录循环
        while (true) {
            if (stuLogin()) {
                sendMessage(new ResponseMsg("登录成功", 200));
                break;
            } else {
                sendMessage(new ResponseMsg("登录失败，请重试", 500));
            }
        }

        // 业务处理循环
        while (true) {
            RequestMsg requestMsg = readRequest();
            if (requestMsg == null) {
                sendMessage(new ResponseMsg("请求格式错误", 500));
                continue;
            }

            switch (requestMsg.getCmd()) {
                case Constants.STUDENT_BEGIN_EXAM:
                    stuBeginExam((Student) requestMsg.getData());
                    break;
                case Constants.STUDENT_SUBMIT:
                    handleStudentSubmit((Map<String, Object>) requestMsg.getData());
                    break;
                case Constants.STUDENT_GET_SCORE:
                    handleStudentGetScore((Student) requestMsg.getData());
                    break;
                default:
                    sendMessage(new ResponseMsg("未知命令", 500));
            }
        }
    }

    private boolean stuLogin() throws IOException, ClassNotFoundException {
        RequestMsg requestMsg = readRequest();
        if (requestMsg == null) {
            return false;
        }

        Student student = (Student) requestMsg.getData();
        return ExamServer.getStudentMap().containsKey(student.getId_card()) &&
                ExamServer.getStudentMap().get(student.getId_card()).getName()
                        .equals(student.getName());
    }

    private void stuBeginExam(Student student) throws IOException, ClassNotFoundException {
        // 检查学生是否已经参加过考试并提交
        if (ExamServer.getExamRecordMap().containsKey(student.getId_card())) {
            if (ExamServer.getExamRecordMap().get(student.getId_card()).isSubmit_status()) {
                sendMessage(new ResponseMsg("已经提交，请勿重复考试", 500));
                return;
            }
        }
        File paperDir = new File(Constants.paper_path);
        if (!paperDir.exists() || !paperDir.isDirectory()) {
            sendMessage(new ResponseMsg("试卷目录不存在", 500));
            return;
        }

        File[] paperFiles = paperDir.listFiles();
        if (paperFiles == null || paperFiles.length == 0) {
            sendMessage(new ResponseMsg("没有试卷", 500));
            return;
        }

        // 随机选择一张试卷
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
            sendMessage(new ResponseMsg("读取试卷失败", 500));
            return;
        }

        long startTime = System.currentTimeMillis();
        ExamRecord examRecord = new ExamRecord(student, new Date(startTime).toString());
        examRecord.setBeginTimeMillis(startTime);
        examRecord.setSubmit_status(false);
        ExamServer.getExamRecordMap().put(student.getId_card(), examRecord);

        try {
            FileManager.saveExamRecordMap();
        } catch (Exception e) {
            System.out.println("保存考试记录失败: " + e.getMessage());
        }

        Map<String, Object> examData = new HashMap<>();
        examData.put("paperContent", sb.toString());
        examData.put("startTime", startTime);
        examData.put("paperName", paperFile.getName());

        sendMessage(new ResponseMsg("考试开始", 200, examData));
        startExamTimer(startTime, student);
    }

    private void startExamTimer(long startTime, Student student) {
        Thread timerThread = new Thread(() -> {
            try {
                long examDurationMillis = Constants.EXAM_DURATION_MINUTES * 60 * 1000L;
                long reminderTimeMillis = Constants.REMINDER_MINUTES * 60 * 1000L;
                long reminderTime = startTime + (examDurationMillis - reminderTimeMillis);

                long currentTime = System.currentTimeMillis();
                if (currentTime < reminderTime) {
                    Thread.sleep(reminderTime - currentTime);
                    sendMessage(new ResponseMsg("考试提醒", 201, "距离考试结束还有30分钟"));
                }

                long endTime = startTime + examDurationMillis;
                currentTime = System.currentTimeMillis();
                if (currentTime < endTime) {
                    Thread.sleep(endTime - currentTime);
                    sendMessage(new ResponseMsg("考试结束", 202, "考试时间已到，请停止答题并提交"));
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("计时器线程异常: " + e.getMessage());
            }
        });

        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void handleStudentSubmit(Map<String, Object> submissionData) throws IOException {
        try {
            Student student = (Student) submissionData.get("student");
            String answerContent = (String) submissionData.get("answerContent");
            long submitTime = (Long) submissionData.get("submitTime");

            if (student == null || answerContent == null || answerContent.trim().isEmpty()) {
                sendMessage(new ResponseMsg("提交数据不完整", 500));
                return;
            }

            long beginTime = 0;
            ExamRecord examRecord = ExamServer.getExamRecordMap().get(student.getId_card());

            if (examRecord != null) {
                // 关键：提交前再次检查是否已提交
                if (examRecord.isSubmit_status()) {
                    sendMessage(new ResponseMsg("您已提交过答卷，不可重复提交", 500));
                    return;
                }
                examRecord.setSubmit_status(true);
                examRecord.setSubmit_time(new Date(submitTime).toString());
                examRecord.setSubmitTimeMillis(submitTime);
                beginTime = examRecord.getBeginTimeMillis();
            } else {
                sendMessage(new ResponseMsg("未找到考试记录，提交失败", 500));
                return;
            }

            long durationMillis = submitTime - beginTime;
            long durationMinutes = durationMillis / (1000 * 60);
            String beginTimeStr = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(beginTime));

            try {
                FileManager.saveExamRecordMap();
            } catch (Exception e) {
                System.out.println("更新考试记录失败: " + e.getMessage());
            }

            File answerDir = new File(Constants.student_submit_path);
            if (!answerDir.exists()) {
                answerDir.mkdirs();
            }

            String uniqueFileName = student.getId_card() + "_" + student.getName() + "_" +
                    beginTimeStr + "_" + durationMinutes + "min_pending.txt";
            File answerFile = new File(answerDir, uniqueFileName);

            try (FileWriter writer = new FileWriter(answerFile)) {
                writer.write("学生姓名: " + student.getName() + "\n");
                writer.write("准考证号: " + student.getId_card() + "\n");
                writer.write("提交时间: " + new java.util.Date(submitTime).toString() + "\n");
                writer.write("==========================================\n\n");
                writer.write(answerContent);
            }

            System.out.println("学生 " + student.getName() + " (" + student.getId_card() +
                    ") 提交了试卷，保存至: " + answerFile.getAbsolutePath());

            sendMessage(new ResponseMsg("试卷提交成功，已保存至服务器", 200));
        } catch (Exception e) {
            System.out.println("处理学生提交时发生异常: " + e.getMessage());
            sendMessage(new ResponseMsg("提交处理失败: " + e.getMessage(), 500));
        }
    }

    private void handleStudentGetScore(Student student) throws IOException {
        ExamRecord examRecord = ExamServer.getExamRecordMap().get(student.getId_card());
        if (examRecord != null) {
            sendMessage(new ResponseMsg("获取数据成功", 200, examRecord));
        }else {
            sendMessage(new ResponseMsg("未找到考试记录", 500));
        }
    }
}
