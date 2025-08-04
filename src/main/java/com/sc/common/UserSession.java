package com.sc.common;

import java.net.Socket;
import java.io.ObjectOutputStream;
import java.util.Date;

public class UserSession {
    private String username;
    private String userId; // 学号/工号/管理员账号
    private String userType; // ADMIN, TEACHER, STUDENT
    private Socket socket;
    private ObjectOutputStream outputStream;
    private Date loginTime;

    public UserSession(String username, String userId, String userType, Socket socket, ObjectOutputStream outputStream) {
        this.username = username;
        this.userId = userId;
        this.userType = userType;
        this.socket = socket;
        this.outputStream = outputStream;
        this.loginTime = new Date();
    }

    // Getters
    public String getUsername() { return username; }
    public String getUserId() { return userId; }
    public String getUserType() { return userType; }
    public Socket getSocket() { return socket; }
    public ObjectOutputStream getOutputStream() { return outputStream; }
    public Date getLoginTime() { return loginTime; }

    public boolean isActive() {
        return socket != null && !socket.isClosed();
    }

    // 重写equals和hashCode方法，以便正确比较会话
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserSession that = (UserSession) obj;
        return userId.equals(that.userId) && userType.equals(that.userType);
    }

    @Override
    public int hashCode() {
        return userId.hashCode() + userType.hashCode();
    }
}