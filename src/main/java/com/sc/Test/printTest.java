package com.sc.Test;

import com.sc.Servicer.core.ExamServer;

public class printTest {
    public static void main(String[] args) {
        System.out.println(ExamServer.getExamRecordMap());
        System.out.println(ExamServer.getExamRecordMap().get("1").getScore());
    }
}
