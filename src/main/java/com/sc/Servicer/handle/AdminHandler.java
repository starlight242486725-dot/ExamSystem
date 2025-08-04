package com.sc.Servicer.handle;

import com.sc.Servicer.core.ExamServer;
import com.sc.Servicer.utils.FileManager;
import com.sc.entity.Admin;
import com.sc.entity.Student;
import com.sc.entity.Teacher;
import com.sc.common.Constants;
import com.sc.common.RequestMsg;
import com.sc.common.ResponseMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AdminHandler extends BaseHandler{
    public AdminHandler(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        super(socket, oos, ois);
    }

    @Override
    public void handleBusiness() throws IOException, ClassNotFoundException {
        // 登录循环
        while (true) {
            if (adminLogin()) {
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
                case Constants.ADMIN_ADD_STU:
                    adminAddStu((Student) requestMsg.getData());
                    break;
                case Constants.ADMIN_ADD_TEA:
                    adminAddTea((Teacher) requestMsg.getData());
                    break;
                case Constants.ADMIN_DEL_STU:
                    adminDelStu((String) requestMsg.getData());
                    break;
                case Constants.ADMIN_DEL_TEA:
                    adminDelTea((String) requestMsg.getData());
                    break;
                case Constants.ADMIN_GET_ALL_STU:
                    adminGetAllStu();
                    break;
                case Constants.ADMIN_GET_ALL_TEA:
                    adminGetAllTea();
                    break;
                default:
                    sendMessage(new ResponseMsg("未知命令", 500));
            }
        }
    }
    private boolean adminLogin() throws IOException, ClassNotFoundException {
        RequestMsg loginRequest = readRequest();
        if (loginRequest == null || loginRequest.getCmd() != Constants.ADMIN_LOGIN) {
            return false;
        }

        Admin admin = (Admin) loginRequest.getData();
        return Constants.name.equals(admin.getName()) && Constants.pass.equals(admin.getPass());
    }
    private void adminAddStu(Student student) throws IOException {
        if (student == null) {
            sendMessage(new ResponseMsg("学生不能为空", 500));
            return;
        }

        String stu_id = student.getId_card();
        if (ExamServer.getStudentMap().containsKey(stu_id)) {
            sendMessage(new ResponseMsg("学号已经存在", 500));
            return;
        }

        ExamServer.getStudentMap().put(stu_id, student);
        sendMessage(new ResponseMsg("添加学生成功", 200));

        try {
            FileManager.saveStudentMap();
        } catch (IOException e) {
            System.out.println("保存学生数据到文件失败: " + e.getMessage());
        }
    }
    private void adminAddTea(Teacher teacher) throws IOException {
        if (teacher == null) {
            sendMessage(new ResponseMsg("教师不能为空", 500));
            return;
        }

        String tea_id = teacher.getId_card();
        if (ExamServer.getTeacherMap().containsKey(tea_id)) {
            sendMessage(new ResponseMsg("工号已经存在", 500));
            return;
        }

        ExamServer.getTeacherMap().put(tea_id, teacher);
        sendMessage(new ResponseMsg("添加教师成功", 200));

        try {
            FileManager.saveTeacherMap();
        } catch (IOException e) {
            System.out.println("保存教师数据到文件失败: " + e.getMessage());
        }
    }

    private void adminDelStu(String id) throws IOException {
        if (id == null || id.isEmpty() || !ExamServer.getStudentMap().containsKey(id)) {
            sendMessage(new ResponseMsg("未查找到用户", 400));
            return;
        }

        ExamServer.getStudentMap().remove(id);
        sendMessage(new ResponseMsg("删除成功", 200));
        // 添加保存操作
        try {
            FileManager.saveStudentMap();
        } catch (IOException e) {
            System.out.println("保存学生数据到文件失败: " + e.getMessage());
            // 可以选择是否向客户端发送错误信息
        }
    }

    private void adminDelTea(String id) throws IOException {
        if (id == null || id.isEmpty() || !ExamServer.getTeacherMap().containsKey(id)) {
            sendMessage(new ResponseMsg("未查找到用户", 400));
            return;
        }

        ExamServer.getTeacherMap().remove(id);
        sendMessage(new ResponseMsg("删除成功", 200));
        // 添加保存操作
        try {
            FileManager.saveTeacherMap();
        } catch (IOException e) {
            System.out.println("保存教师数据到文件失败: " + e.getMessage());
            // 可以选择是否向客户端发送错误信息
        }
    }

    private void adminGetAllStu() throws IOException {
        if (ExamServer.getStudentMap().isEmpty()) {
            sendMessage(new ResponseMsg("暂无学生信息", 500));
            return;
        }
        sendMessage(new ResponseMsg("查询成功", 200, ExamServer.getStudentMap()));
    }

    private void adminGetAllTea() throws IOException {
        if (ExamServer.getTeacherMap().isEmpty()) {
            sendMessage(new ResponseMsg("暂无教师信息", 500));
            return;
        }
        sendMessage(new ResponseMsg("查询成功", 200, ExamServer.getTeacherMap()));
    }

}
