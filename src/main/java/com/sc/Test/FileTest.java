package com.sc.Test;

import com.sc.common.Constants;

import java.io.File;

public class FileTest {
    public static void main(String[] args) {
        System.out.println(Constants.paper_path);
        File file = new File(Constants.paper_path);
        System.out.println(file.exists());
        System.out.println(file.isFile());
        System.out.println(file.isDirectory());
        File[] files = file.listFiles();
        for (File f : files) {
            System.out.println(f.getName());
        }
    }
}
