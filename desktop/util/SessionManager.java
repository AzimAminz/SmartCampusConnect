package util;

public class SessionManager {
    private static SessionManager instance;
    
    private String token;
    private String userId;
    private String role;
    private String fullName;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startSession(String token, String userId, String role, String fullName) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.fullName = fullName;
    }

    public void clearSession() {
        this.token = null;
        this.userId = null;
        this.role = null;
        this.fullName = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }
}
