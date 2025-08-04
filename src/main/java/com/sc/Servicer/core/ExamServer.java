package com.sc.Servicer.core;

import com.sc.client.student_client.Student;
import com.sc.client.teacher_client.Teacher;
import com.sc.common.Constants;
import com.sc.entity.ExamRecord;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端程序入口
 */
public class ExamServer {
    private static ExecutorService service = Executors.newFixedThreadPool(10);
    private static Map<String,Student> studentMap = null;//学习信息
    private static Map<String, Teacher> teacherMap = null;// 教师信息
    private static Map<String, ExamRecord> examRecordMap = null;//考试记录


    static {
        loadStuMap();
        loadTeaMap();
        loadExamRecordMap();
    }



    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(Constants.port);//创建服务器socket
            //创建死循环，持续接收客户端连接
            while (true) {
                System.out.println("等待连接");
                Socket socket = serverSocket.accept();//得到客户端连接
                //为连接创建线程池
                service.execute(new ClientHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static synchronized Map<String, ExamRecord> getExamRecordMap() {
        return examRecordMap;
    }
    // 从文件加载用户数据
    public static synchronized void loadStuMap() {
        File file = new File(Constants.student_info_path);
        if (!file.exists()) {
            studentMap = new HashMap<>();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {

            if (fis.available() > 0) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                studentMap = (Map<String, Student>) ois.readObject();
            } else {
                studentMap = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            studentMap = new HashMap<>();
        }
    }
    private static void loadExamRecordMap() {
        File file = new File(Constants.exam_record_path);
        if (!file.exists()) {
            examRecordMap = new HashMap<>();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.available() > 0) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                examRecordMap = (Map<String, ExamRecord>) ois.readObject();
            } else {
                examRecordMap = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            examRecordMap = new HashMap<>();
        }
    }
    // 从文件加载用户数据
    public static synchronized void loadTeaMap() {
        File file = new File(Constants.teacher_info_path);
        if (!file.exists()) {
            teacherMap = new HashMap<>();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {

            if (fis.available() > 0) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                teacherMap = (Map<String, Teacher>) ois.readObject();
            } else {
                teacherMap = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            teacherMap = new HashMap<>();
        }
    }

    public synchronized static Map<String, Student> getStudentMap() {
        return studentMap;
    }

    public static void setStudentMap(Map<String, Student> studentMap) {
        ExamServer.studentMap = studentMap;
    }

    public synchronized static Map<String, Teacher> getTeacherMap() {
        return teacherMap;
    }

    public static void setTeacherMap(Map<String, Teacher> teacherMap) {
        ExamServer.teacherMap = teacherMap;
    }


}
