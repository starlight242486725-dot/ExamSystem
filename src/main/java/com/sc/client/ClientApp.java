package com.sc.client;

import com.sc.client.admin_client.AdminClient;
import com.sc.client.student_client.StudentClient;
import com.sc.client.teacher_client.TeacherClient;
import com.sc.common.InputHandler;



public class ClientApp {
    public static void main(String[] args) {
        ClientApp app = new ClientApp();
        app.create_menu();
    }
    public void create_menu(){
        InputHandler in = new InputHandler();
        while (true) {
            System.out.println("欢迎来到在线考试系统");
            System.out.println("1.管理员入口");
            System.out.println("2.学生端入口");
            System.out.println("3.教师端入口");
            System.out.println("4.退出系统");
            int num = in.getIntInput("输入命令:");
            switch (num){
                case 1:{
                    //创建管理员线程
                    AdminClient admin = new AdminClient();
                    Thread admin_thread = new Thread(admin);
                    admin_thread.start();
                    try {
                        admin_thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 2:{
                    StudentClient student = new StudentClient();
                    Thread student_thread = new Thread(student);
                    student_thread.start();
                    try {
                        student_thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 3:{
                    TeacherClient teacher = new TeacherClient();
                    Thread teacher_thread = new Thread(teacher);
                    teacher_thread.start();
                    try {
                        teacher_thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 4:{
                    System.out.println("感谢使用在线考试系统");
                    System.exit(0);
                }
            }
        }
    }
}
