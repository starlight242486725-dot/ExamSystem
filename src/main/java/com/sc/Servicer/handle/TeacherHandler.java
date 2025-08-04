package com.sc.Servicer.handle;

import com.sc.Servicer.core.ExamServer;
import com.sc.Servicer.utils.FileManager;
import com.sc.entity.Teacher;
import com.sc.common.Constants;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;
import com.sc.entity.ExamRecord;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TeacherHandler extends BaseHandler {

    public TeacherHandler(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        super(socket, oos, ois);
    }

    @Override
    public void handleBusiness() throws IOException, ClassNotFoundException {
        // 登录循环
        while (true) {
            if (teacherLogin()) {
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
                case Constants.TEACHER_VIEW_PENDING_PAPERS:
                    handleViewPendingPapers();
                    break;
                case Constants.TEACHER_GRADE_PAPER:
                    handleGradePaper(requestMsg);
                    break;
                case Constants.TEACHER_VIEW_GRADED_PAPERS:
                    handleViewGradedPapers();
                    break;
                case Constants.TEACHER_DOWNLOAD_PAPER:  // 新增：处理下载请求
                    handleDownloadPaper(requestMsg);
                    break;
                default:
                    sendMessage(new ResponseMsg("未知命令", 500));
            }
        }
    }

    // 新增：处理教师下载学生答卷的请求
    private void handleDownloadPaper(RequestMsg requestMsg) throws IOException {
        try {
            // 1. 获取请求中的文件名
            Object data = requestMsg.getData();
            if (!(data instanceof String)) {
                sendMessage(new ResponseMsg("请求格式错误，文件名应为字符串", 500));
                return;
            }
            String fileName = (String) data;

            // 2. 验证文件是否存在于待批改目录
            File paperFile = new File(Constants.student_submit_path, fileName);
            if (!paperFile.exists() || !paperFile.isFile()) {
                sendMessage(new ResponseMsg("答卷文件不存在：" + fileName, 500));
                return;
            }

            // 3. 读取文件内容（使用NIO提高效率，支持大文件）
            byte[] fileContent = Files.readAllBytes(Paths.get(paperFile.getAbsolutePath()));
            String content = new String(fileContent, StandardCharsets.UTF_8);  // 明确字符编码

            // 4. 返回文件内容给教师端
            sendMessage(new ResponseMsg("文件下载成功", 200, content));

        } catch (IOException e) {
            sendMessage(new ResponseMsg("下载失败：" + e.getMessage(), 500));
        }
    }

    private boolean teacherLogin() throws IOException, ClassNotFoundException {
        RequestMsg requestMsg = readRequest();
        if (requestMsg == null) {
            return false;
        }

        Teacher teacher = (Teacher) requestMsg.getData();
        return ExamServer.getTeacherMap().containsKey(teacher.getId_card()) &&
                ExamServer.getTeacherMap().get(teacher.getId_card()).getName()
                        .equals(teacher.getName());
    }

    private void handleViewPendingPapers() throws IOException {
        try {
            File dir = new File(Constants.student_submit_path);
            if (!dir.exists() || !dir.isDirectory()) {
                sendMessage(new ResponseMsg("没有待批改的试卷", 500));
                return;
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                sendMessage(new ResponseMsg("没有待批改的试卷", 500));
                return;
            }

            Map<String, Object> paperInfoList = new HashMap<>();
            paperInfoList.put("待批改试卷数量", files.length);

            List<Map<String, String>> paperList = new ArrayList<>();
            for (File file : files) {
                Map<String, String> paperData = new HashMap<>();
                String fileName = file.getName();
                String[] parts = fileName.replace("_pending.txt", "").split("_");
                if (parts.length >= 4) {
                    paperData.put("学号", parts[0]);
                    paperData.put("姓名", parts[1]);
                    paperData.put("提交时间", parts[2]);
                    paperData.put("考试时长", parts[3]);
                    paperData.put("文件名", fileName);
                    paperList.add(paperData);
                }
            }

            paperInfoList.put("试卷列表", paperList);
            sendMessage(new ResponseMsg("获取试卷列表成功", 200, paperInfoList));
        } catch (IOException e) {
            sendMessage(new ResponseMsg("获取待批改试卷列表失败: " + e.getMessage(), 500));
        }
    }

    private void handleViewGradedPapers() throws IOException, ClassNotFoundException {
        RequestMsg requestMsg = readRequest();
        if (requestMsg == null) {
            sendMessage(new ResponseMsg("请求格式错误", 500));
            return;
        }
        String id = (String) requestMsg.getData();

        File dir = new File(Constants.graded_paper_path);

        // 检查目录是否存在
        if (!dir.exists() || !dir.isDirectory()) {
            // 目录不存在时，返回空列表而非错误
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("已批改试卷数量", 0);
            emptyResult.put("试卷列表", new ArrayList<>());
            sendMessage(new ResponseMsg("已批改试卷目录不存在，暂无数据", 200, emptyResult));
            return;
        }

        File[] files = dir.listFiles();
        // 检查目录是否为空
        if (files == null || files.length == 0) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("已批改试卷数量", 0);
            emptyResult.put("试卷列表", new ArrayList<>());
            sendMessage(new ResponseMsg("暂无已批改试卷", 200, emptyResult));
            return;
        }

        Map<String, Object> paperInfoList = new HashMap<>();
        List<Map<String, String>> paperList = new ArrayList<>();
        int count = 0;

        for (File file : files) {
            String fileName = file.getName();
            // 修正文件名处理逻辑，避免替换错误（原代码替换了不存在的_pending.txt）
            String[] parts = fileName.replace("_graded.txt", "").split("_");
            if (parts.length >= 5 && parts[4].equals(id)) {
                Map<String, String> paperData = new HashMap<>();
                paperData.put("学号", parts[0]);
                paperData.put("姓名", parts[1]);
                paperData.put("提交时间", parts[2]);
                paperData.put("考试时长", parts[3]);
                paperData.put("批改教师工号", parts[4]);
                paperData.put("文件名", fileName);
                paperList.add(paperData);
                count++;
            }
        }

        // 处理有目录但无符合条件试卷的情况
        if (count == 0) {
            paperInfoList.put("已批改试卷数量", 0);
            paperInfoList.put("试卷列表", new ArrayList<>());
            sendMessage(new ResponseMsg("当前教师暂无已批改试卷", 200, paperInfoList));
        } else {
            paperInfoList.put("已批改试卷数量", count);
            paperInfoList.put("试卷列表", paperList);
            sendMessage(new ResponseMsg("获取试卷列表成功", 200, paperInfoList));
        }
    }
    private void handleGradePaper(RequestMsg requestMsg) throws IOException {
        Object data = requestMsg.getData();
        if (!(data instanceof Map)) {
            sendMessage(new ResponseMsg("请求格式错误", 500));
            return;
        }

        Map<String, Object> dataMap = (Map<String, Object>) data;
        String gradedPaperFileName = (String) dataMap.get("文件名");
        String score = Double.toString((double) dataMap.get("分数"));
        Teacher teacher = (Teacher) dataMap.get("批改教师");
        String gradingTime = (String) dataMap.get("批改时间");

        File file = new File(Constants.student_submit_path + "/" + gradedPaperFileName);
        if (!file.exists()) {
            sendMessage(new ResponseMsg("文件不存在", 500));
            return;
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            sendMessage(new ResponseMsg("读取文件失败", 500));
            return;
        }

        content.append("==========================================\n");
        content.append("得分: ").append(score).append("\n");
        content.append("批改时间: ").append(gradingTime).append("\n");
        content.append("批改教师: ").append(teacher.getName()).append("\n");

        String gradedFileName = gradedPaperFileName.replace("_pending.txt",
                "_" + teacher.getId_card() + "_graded.txt");
        File gradedFile = new File(Constants.graded_paper_path + "/" + gradedFileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(gradedFile))) {
            writer.write(content.toString());
        } catch (IOException e) {
            sendMessage(new ResponseMsg("保存批改结果失败: " + e.getMessage(), 500));
            return;
        }

        if (!file.delete()) {
            System.out.println("删除文件失败");
        }

        String studentId = gradedPaperFileName.split("_")[0];
        ExamRecord examRecord = ExamServer.getExamRecordMap().get(studentId);
        if (examRecord != null) {
            examRecord.setScore(Double.parseDouble(score));
            examRecord.setGraded(true);
            try {
                FileManager.saveExamRecordMap();
            } catch (Exception e) {
                System.out.println("保存考试记录失败: " + e.getMessage());
            }
        }else {
            System.out.println("未找到对应的考试记录");
        }


        sendMessage(new ResponseMsg("批改成功", 200));
    }
}
