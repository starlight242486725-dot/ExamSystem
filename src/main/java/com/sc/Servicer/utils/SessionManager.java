package com.sc.Servicer.utils;

import com.sc.common.ResponseMsg;
import com.sc.common.UserSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    // 使用ConcurrentHashMap保证线程安全
    private static final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 生成会话键值
     * @param userId 用户ID（学号/工号/管理员账号）
     * @param userType 用户类型
     * @return 会话键值
     */
    private static String generateSessionKey(String userId, String userType) {
        return userType + ":" + userId;
    }

    /**
     * 添加用户会话
     * @param userId 用户ID
     * @param userType 用户类型
     * @param session 用户会话对象
     * @return 如果踢出旧会话则返回true，否则返回false
     */
    public static boolean addSession(String userId, String userType, UserSession session) {
        String sessionKey = generateSessionKey(userId, userType);
        UserSession oldSession = activeSessions.put(sessionKey, session);
        if (oldSession != null) {
            // 强制旧会话下线
            forceLogout(oldSession);
            return true;
        }
        return false;
    }

    /**
     * 移除用户会话
     * @param userId 用户ID
     * @param userType 用户类型
     */
    public static void removeSession(String userId, String userType) {
        String sessionKey = generateSessionKey(userId, userType);
        activeSessions.remove(sessionKey);
    }

    /**
     * 检查用户是否已登录
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn(String userId, String userType) {
        String sessionKey = generateSessionKey(userId, userType);
        UserSession session = activeSessions.get(sessionKey);
        if (session != null && !session.isActive()) {
            // 会话已失效，移除它
            removeSession(userId, userType);
            return false;
        }
        return session != null;
    }

    /**
     * 获取用户会话
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 用户会话对象
     */
    public static UserSession getSession(String userId, String userType) {
        String sessionKey = generateSessionKey(userId, userType);
        return activeSessions.get(sessionKey);
    }

    /**
     * 强制用户下线
     * @param session 用户会话
     */
    private static void forceLogout(UserSession session) {
        try {
            // 发送强制下线通知
            ResponseMsg logoutMsg = new ResponseMsg("您的账号在别处登录，已被强制下线", 401);
            session.getOutputStream().writeObject(logoutMsg);
            session.getOutputStream().flush();
        } catch (Exception e) {
            // 忽略发送错误
        }

        // 关闭连接
        try {
            if (session.getSocket() != null && !session.getSocket().isClosed()) {
                session.getSocket().close();
            }
        } catch (Exception e) {
            // 忽略关闭错误
        }
    }

    /**
     * 清理所有会话（服务器关闭时调用）
     */
    public static void clearAllSessions() {
        for (UserSession session : activeSessions.values()) {
            try {
                if (session.getSocket() != null && !session.getSocket().isClosed()) {
                    session.getSocket().close();
                }
            } catch (Exception e) {
                // 忽略关闭错误
            }
        }
        activeSessions.clear();
    }
}
