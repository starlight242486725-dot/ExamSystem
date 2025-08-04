package com.sc.common;

import java.io.File;

/**
 * 常量定义，端口号，路径，消息类型
 */
public class Constants{
    public static final long EXAM_DURATION_MINUTES = 130;
    public static final long REMINDER_MINUTES = 30;



    public static int port = 888;
    public static String root_path = "ExamSys";
    public static String student_info_path = root_path+"/Server/stuInfo/stu.obj";
    public static String teacher_info_path = root_path+"/Server/teaInfo/tea.obj";
    public static String exam_record_path = root_path+"/Server/examRecord/examRecord.obj";
    public static String student_submit_path = root_path+"/Server/stuPaper";
    public static String paper_path = root_path+"/Server/paper";
    public static String student_paper_path = root_path + "/Client/StuClient/papers";
    public static String answer_path = root_path+"/Client/StuClient/submit";
    public static String graded_paper_path = root_path+"/Server/gradedPaper";
    public static String tea_client_stuAnswer = root_path+"/Client/TeaClient";

    //管理员唯一账号
    public static final String name = "admin";
    public static final String pass = "123456";

    //命令
    public static final int ADMIN_LOGIN = 101;
    public static final int ADMIN_ADD_STU = 102;
    public static final int ADMIN_ADD_TEA = 103;
    public static final int ADMIN_DEL_STU = 104;
    public static final int ADMIN_DEL_TEA = 105;
    public static final int ADMIN_GET_ALL_STU = 106;
    public static final int ADMIN_GET_ALL_TEA = 107;

    public static final int STUDENT_LOGIN = 201;
    public static final int STUDENT_BEGIN_EXAM = 202;
    public static final int STUDENT_SUBMIT = 203;
    public static final int STUDENT_GET_SCORE = 204;

    public static final int TEACHER_LOGIN = 301;
    public static final int TEACHER_VIEW_PENDING_PAPERS = 302;
    public static final int TEACHER_GRADE_PAPER = 303;
    public static final int TEACHER_VIEW_GRADED_PAPERS = 304;
    public static final int TEACHER_VIEW_PAPER = 305;

    public static final String ADMIN = "ADMIN";       // 管理员
    public static final String STUDENT = "STUDENT";   // 学生
    public static final String TEACHER = "TEACHER";   // 老师


}
