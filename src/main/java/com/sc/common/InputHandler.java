package com.sc.common;

import java.util.Scanner;

public class InputHandler {
    private Scanner scanner;

    // 初始化Scanner
    public InputHandler() {
        this.scanner = new Scanner(System.in);
    }

    // 获取整数类型输入
    public int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                int value = scanner.nextInt();
                scanner.nextLine(); // 清除缓冲区
                return value;
            } else {
                System.out.println("输入错误！请输入整数。");
                scanner.nextLine(); // 清除错误输入
            }
        }
    }

    // 获取浮点数类型输入
    public double getDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextDouble()) {
                double value = scanner.nextDouble();
                scanner.nextLine(); // 清除缓冲区
                return value;
            } else {
                System.out.println("输入错误！请输入数字。");
                scanner.nextLine(); // 清除错误输入
            }
        }
    }

    // 获取布尔类型输入
    public boolean getBooleanInput(String prompt) {
        while (true) {
            System.out.print(prompt + "(true/false): ");
            if (scanner.hasNextBoolean()) {
                boolean value = scanner.nextBoolean();
                scanner.nextLine(); // 清除缓冲区
                return value;
            } else {
                System.out.println("输入错误！请输入true或false。");
                scanner.nextLine(); // 清除错误输入
            }
        }
    }

    // 获取非空字符串输入
    public String getNonEmptyStringInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            } else {
                System.out.println("输入错误！字符串不能为空。");
            }
        }
    }

    // 关闭Scanner
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
