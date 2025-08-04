package com.sc.entity;

import com.sc.client.student_client.Student;

import java.io.Serializable;

/**
 * 考试记录，考试时间，提交状态
 */
public class ExamRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private Student student;// 学生
    private boolean submit_status;// 提交状态
    private String begin_time;// 开始时间 时间字符串
    private String submit_time;// 提交时间 时间字符串
    private long beginTimeMillis; // 开始时间毫秒值
    private long submitTimeMillis;// 结束时间毫秒值
    private double score;// 分数

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public long getSubmitTimeMillis() {
        return submitTimeMillis;
    }

    public void setSubmitTimeMillis(long submitTimeMillis) {
        this.submitTimeMillis = submitTimeMillis;
    }

    public long getBeginTimeMillis() {
        return beginTimeMillis;
    }

    public void setBeginTimeMillis(long beginTimeMillis) {
        this.beginTimeMillis = beginTimeMillis;
    }

    public ExamRecord(Student student, String begin_time) {
        this.student = student;
        this.begin_time = begin_time;
    }

    public String getBegin_time() {
        return begin_time;
    }

    public void setBegin_time(String begin_time) {
        this.begin_time = begin_time;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public boolean isSubmit_status() {
        return submit_status;
    }

    public void setSubmit_status(boolean submit_status) {
        this.submit_status = submit_status;
    }

    public String getSubmit_time() {
        return submit_time;
    }

    public void setSubmit_time(String submit_time) {
        this.submit_time = submit_time;
    }

    @Override
    public String toString() {
        return "ExamRecord{" +
                "student=" + student +
                ", submit_status=" + submit_status +
                ", begin_time='" + begin_time + '\'' +
                ", submit_time='" + submit_time + '\'' +
                '}';
    }
}
