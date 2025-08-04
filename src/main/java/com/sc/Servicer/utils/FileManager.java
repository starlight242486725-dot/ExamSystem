package com.sc.Servicer.utils;

import com.sc.Servicer.core.ExamServer;
import com.sc.common.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class FileManager {
    public synchronized static void saveStudentMap() throws IOException {
        File f = new File(Constants.student_info_path);
        if (!f.exists()) {
            f.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(ExamServer.getStudentMap());
        oos.flush();
        oos.close();
    }
    public synchronized static void saveTeacherMap() throws IOException {
        File f = new File(Constants.teacher_info_path);
        if (!f.exists()) {
            f.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(ExamServer.getTeacherMap());
        oos.flush();
        oos.close();
    }
    // 保存考试记录到文件
    public static synchronized void saveExamRecordMap() throws IOException {
        File f = new File(Constants.exam_record_path);
        if (!f.exists()) {
            f.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(ExamServer.getExamRecordMap());
        oos.flush();
        oos.close();
    }
}
