package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import service.RestService;
import util.JsonParser;
import util.SessionManager;

public class DashboardView extends JFrame {
    private final RestService restService = new RestService();
    private final SessionManager session = SessionManager.getInstance();

    // Common Header Components
    private JLabel welcomeLabel;
    private JLabel roleBadge;

    // Student Dashboard Components
    private JLabel gpaLabel, semesterLabel, progLabel, facLabel, emailLabel, phoneLabel;
    private JTable studEnrolTable, studBookingsTable, studLoansTable, notificationTable;
    private DefaultTableModel studEnrolModel, studBookingsModel, studLoansModel, notificationModel;

    // Admin Dashboard Components
    private JLabel statStudentsVal, statEnrolmentsVal, statBookingsVal, statLoansVal;
    private JTable adminBookingsTable, adminLoansTable;
    private DefaultTableModel adminBookingsModel, adminLoansModel;

    // Management Tables
    private JTable studentsTable;
    private DefaultTableModel studentsModel;
    private JTable coursesTable;
    private DefaultTableModel coursesModel;

    public DashboardView() {
        setTitle("SmartCampus Connect - Console Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1024, 720);
        setLocationRelativeTo(null);
        
        // Root Panel Matte Obsidian Background
        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBackground(new Color(0x121212));
        setContentPane(rootPanel);

        // ---- 1. Top Header Bar ----
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(new Color(0x1E1E1E));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2D2D2D)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Logo + App Name
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);
        JLabel logoIcon = new JLabel("🎓");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        JLabel logoTitle = new JLabel("SmartCampus");
        logoTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoTitle.setForeground(Color.WHITE);
        JLabel logoSubtitle = new JLabel("Connect");
        logoSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoSubtitle.setForeground(new Color(0xFF00BFA5)); // Mint Green
        logoPanel.add(logoIcon);
        logoPanel.add(logoTitle);
        logoPanel.add(logoSubtitle);
        headerPanel.add(logoPanel, BorderLayout.WEST);

        // Welcome Session details + Logout
        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        sessionPanel.setOpaque(false);

        welcomeLabel = new JLabel("Welcome, " + session.getFullName() + " (" + session.getUserId() + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        welcomeLabel.setForeground(Color.WHITE);
        sessionPanel.add(welcomeLabel);

        roleBadge = new JLabel(session.getRole());
        roleBadge.setOpaque(true);
        roleBadge.setBackground(session.isAdmin() ? new Color(0x3A261C) : new Color(0x1C2D3A));
        roleBadge.setForeground(session.isAdmin() ? new Color(0xFFB74D) : new Color(0x4FC3F7));
        roleBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        roleBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        sessionPanel.add(roleBadge);

        JButton logoutBtn = new JButton("LOGOUT");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutBtn.setBackground(new Color(0x2A2A2A));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> performLogout());
        sessionPanel.add(logoutBtn);
        headerPanel.add(sessionPanel, BorderLayout.EAST);

        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // ---- 2. Main Tabs Area ----
        JTabbedPane mainTabs = new JTabbedPane(JTabbedPane.TOP);
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mainTabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (session.isAdmin()) {
            // Admin Tabs
            mainTabs.addTab("📊 System Dashboard", createAdminDashboardTab());
            mainTabs.addTab("👤 Student Accounts", createStudentManagementTab());
            mainTabs.addTab("📚 Course Directory (REST)", createCourseDirectoryTab());
            mainTabs.addTab("🚪 Room Bookings (SOAP)", new RoomBookingPanel());
            mainTabs.addTab("📖 Library Manager Console", new LibraryPanel());
        } else {
            // Student Tabs
            mainTabs.addTab("📊 My Academic Hub", createStudentDashboardTab());
            mainTabs.addTab("📚 Course Enrolment (REST)", createStudentEnrolmentTab());
            mainTabs.addTab("🚪 Room Bookings (SOAP)", new RoomBookingPanel());
            mainTabs.addTab("📖 Search Books Catalog", new LibraryPanel());
        }

        rootPanel.add(mainTabs, BorderLayout.CENTER);

        // Instant refresh on tab selection
        mainTabs.addChangeListener(e -> {
            int index = mainTabs.getSelectedIndex();
            if (index == -1) return;
            String title = mainTabs.getTitleAt(index);
            if (title.contains("Dashboard") || title.contains("Hub") || title.contains("Accounts") || title.contains("Course")) {
                refreshData();
            } else {
                Component comp = mainTabs.getComponentAt(index);
                if (comp instanceof RoomBookingPanel) {
                    ((RoomBookingPanel) comp).refreshBookings();
                } else if (comp instanceof LibraryPanel) {
                    ((LibraryPanel) comp).refreshAll();
                }
            }
        });

        // 30-Second Auto-Refresh Timer in background
        javax.swing.Timer autoRefreshTimer = new javax.swing.Timer(30000, e -> {
            refreshData();
            int index = mainTabs.getSelectedIndex();
            if (index != -1) {
                Component comp = mainTabs.getComponentAt(index);
                if (comp instanceof RoomBookingPanel) {
                    ((RoomBookingPanel) comp).refreshBookings();
                } else if (comp instanceof LibraryPanel) {
                    ((LibraryPanel) comp).refreshAll();
                }
            }
        });
        autoRefreshTimer.start();

        // Preload stats / info
        refreshData();
    }

    private void performLogout() {
        session.clearSession();
        LoginView login = new LoginView();
        login.setVisible(true);
        dispose();
    }

    // =========================================================================
    // ADMIN DASHBOARD TAB
    // =========================================================================
    private JPanel createAdminDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Grid of Stats Cards
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 15, 0));
        statsGrid.setOpaque(false);

        statsGrid.add(createStatCard("Total Students", "0", new Color(0xFF00BFA5))); // Mint Green
        statsGrid.add(createStatCard("Course Enrolments", "0", new Color(0xFF3F51B5))); // Indigo
        statsGrid.add(createStatCard("Room Bookings", "0", new Color(0xFF4FC3F7))); // Sky Blue
        statsGrid.add(createStatCard("Active Book Loans", "0", new Color(0xFFFFB74D))); // Orange

        panel.add(statsGrid, BorderLayout.NORTH);

        // Dashboard Lists (Room Bookings & Book Loans)
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        tablesPanel.setOpaque(false);

        // Room Bookings
        JPanel bookingsPanel = new JPanel(new BorderLayout(8, 8));
        bookingsPanel.setOpaque(false);
        bookingsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Active Room Bookings", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        String[] bCols = {"Ref", "Student ID", "Room", "Slot", "Date"};
        adminBookingsModel = new DefaultTableModel(bCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        adminBookingsTable = new JTable(adminBookingsModel);
        bookingsPanel.add(new JScrollPane(adminBookingsTable), BorderLayout.CENTER);
        tablesPanel.add(bookingsPanel);

        // Book Loans
        JPanel loansPanel = new JPanel(new BorderLayout(8, 8));
        loansPanel.setOpaque(false);
        loansPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Book Loan Log", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        String[] lCols = {"Ref", "Student ID", "ISBN", "Due Date", "Status"};
        adminLoansModel = new DefaultTableModel(lCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        adminLoansTable = new JTable(adminLoansModel);
        loansPanel.add(new JScrollPane(adminLoansTable), BorderLayout.CENTER);
        tablesPanel.add(loansPanel);

        panel.add(tablesPanel, BorderLayout.CENTER);

        // Refresh Button
        JButton refreshBtn = new JButton("🔄 Refresh Dashboard Analytics");
        refreshBtn.setBackground(new Color(0x1E1E1E));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshData());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatCard(String title, String initialVal, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(0x1E1E1E));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(0x8A8A8A));
        card.add(titleLbl, BorderLayout.NORTH);

        JLabel valLbl = new JLabel(initialVal);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valLbl.setForeground(Color.WHITE);
        card.add(valLbl, BorderLayout.CENTER);

        // Store reference to update later
        if ("Total Students".equals(title)) statStudentsVal = valLbl;
        else if ("Course Enrolments".equals(title)) statEnrolmentsVal = valLbl;
        else if ("Room Bookings".equals(title)) statBookingsVal = valLbl;
        else if ("Active Book Loans".equals(title)) statLoansVal = valLbl;

        return card;
    }

    // =========================================================================
    // STUDENT DASHBOARD TAB
    // =========================================================================
    private JPanel createStudentDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Profile Panel Card
        JPanel profileCard = new JPanel(new GridBagLayout());
        profileCard.setBackground(new Color(0x1E1E1E));
        profileCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Academic Profile Details", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints pbc = new GridBagConstraints();
        pbc.gridx = 0;
        pbc.gridy = 0;
        pbc.anchor = GridBagConstraints.WEST;
        pbc.insets = new Insets(6, 10, 6, 20);

        gpaLabel = new JLabel("GPA: Loading...");
        gpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gpaLabel.setForeground(new Color(0xFF00BFA5));
        profileCard.add(gpaLabel, pbc);

        semesterLabel = new JLabel("Semester: Loading...");
        semesterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        semesterLabel.setForeground(Color.WHITE);
        pbc.gridx = 1;
        profileCard.add(semesterLabel, pbc);

        progLabel = new JLabel("Programme: Loading...");
        progLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        progLabel.setForeground(Color.WHITE);
        pbc.gridx = 0;
        pbc.gridy = 1;
        profileCard.add(progLabel, pbc);

        facLabel = new JLabel("Faculty: Loading...");
        facLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        facLabel.setForeground(Color.WHITE);
        pbc.gridx = 1;
        profileCard.add(facLabel, pbc);

        emailLabel = new JLabel("Email: Loading...");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emailLabel.setForeground(Color.WHITE);
        pbc.gridx = 0;
        pbc.gridy = 2;
        profileCard.add(emailLabel, pbc);

        phoneLabel = new JLabel("Phone: Loading...");
        phoneLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        phoneLabel.setForeground(Color.WHITE);
        pbc.gridx = 1;
        profileCard.add(phoneLabel, pbc);

        panel.add(profileCard, BorderLayout.NORTH);

        // Multi Grid for lists
        JPanel listGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        listGrid.setOpaque(false);

        // 1. Enrolments
        JPanel p1 = new JPanel(new BorderLayout(5, 5));
        p1.setOpaque(false);
        p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Active Enrolments", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        studEnrolModel = new DefaultTableModel(new String[]{"Course Code", "Course Title", "Enrolled Date"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studEnrolTable = new JTable(studEnrolModel);
        p1.add(new JScrollPane(studEnrolTable), BorderLayout.CENTER);
        listGrid.add(p1);

        // 2. Room Bookings
        JPanel p2 = new JPanel(new BorderLayout(5, 5));
        p2.setOpaque(false);
        p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Room Bookings", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        studBookingsModel = new DefaultTableModel(new String[]{"Ref", "Room", "Slot", "Booking Date"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studBookingsTable = new JTable(studBookingsModel);
        p2.add(new JScrollPane(studBookingsTable), BorderLayout.CENTER);
        listGrid.add(p2);

        // 3. Book Loans
        JPanel p3 = new JPanel(new BorderLayout(5, 5));
        p3.setOpaque(false);
        p3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Library Loans", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        studLoansModel = new DefaultTableModel(new String[]{"Ref", "Book Title", "Due Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studLoansTable = new JTable(studLoansModel);
        p3.add(new JScrollPane(studLoansTable), BorderLayout.CENTER);
        listGrid.add(p3);

        // 4. Notifications
        JPanel p4 = new JPanel(new BorderLayout(5, 5));
        p4.setOpaque(false);
        p4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Alert logs", 0, 0, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        notificationModel = new DefaultTableModel(new String[]{"Alert details", "Date Received"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        notificationTable = new JTable(notificationModel);
        p4.add(new JScrollPane(notificationTable), BorderLayout.CENTER);
        listGrid.add(p4);

        panel.add(listGrid, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // STUDENT COURSE ENROLMENT TAB
    // =========================================================================
    private JPanel createStudentEnrolmentTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("University Course Catalog");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        panel.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"Course Code", "Title", "Lecturer", "Credit Hours", "Capacity", "Max Capacity", "Status"};
        coursesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        coursesTable = new JTable(coursesModel);
        coursesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        coursesTable.setRowHeight(25);
        
        coursesTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String status = value.toString();
                    if ("Available".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFF00BFA5)); // Mint Green
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else if ("Enrolled".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFFFF6B6B)); // Crimson Red
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setForeground(Color.WHITE);
                    }
                }
                return label;
            }
        });
        
        panel.add(new JScrollPane(coursesTable), BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton enrolBtn = new JButton("Register / Enrol Course");
        enrolBtn.setBackground(new Color(0xFF00BFA5)); // Mint Green
        enrolBtn.setForeground(Color.BLACK);
        enrolBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enrolBtn.addActionListener(e -> {
            int row = coursesTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a course to register.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String status = coursesTable.getValueAt(row, 6).toString();
            if ("Enrolled".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "You are already enrolled in this course.", "Already Enrolled", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String courseCode = coursesTable.getValueAt(row, 0).toString();
            
            // We need student profile's internal database ID. We'll fetch it from student profile data loaded.
            new Thread(() -> {
                try {
                    // Let's get student internal ID from session
                    String dashboardJson = restService.getDashboardData(session.getToken());
                    Map<String, String> dashData = JsonParser.parseObject(dashboardJson);
                    // Parse nested profile to get ID
                    String profileStr = getNestedJson(dashboardJson, "profile");
                    Map<String, String> prof = JsonParser.parseObject(profileStr);
                    String internalId = prof.get("id");
                    
                    restService.enrolStudent(session.getToken(), internalId, courseCode);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Course enrolment successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Failed to enrol: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        actions.add(enrolBtn);

        JButton dropBtn = new JButton("Drop Selected Course");
        dropBtn.setBackground(new Color(0xFFFF6B6B)); // Crimson Red
        dropBtn.setForeground(Color.WHITE);
        dropBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dropBtn.addActionListener(e -> {
            int row = coursesTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a course to drop.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String status = coursesTable.getValueAt(row, 6).toString();
            if (!"Enrolled".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "You cannot drop a course you are not enrolled in.", "Not Enrolled", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String courseCode = coursesTable.getValueAt(row, 0).toString();
            
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to drop course " + courseCode + "?", "Confirm Drop", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    String dashboardJson = restService.getDashboardData(session.getToken());
                    String profileStr = getNestedJson(dashboardJson, "profile");
                    Map<String, String> prof = JsonParser.parseObject(profileStr);
                    String internalId = prof.get("id");
                    
                    restService.dropCourse(session.getToken(), internalId, courseCode);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Course dropped successfully.", "Dropped", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Failed to drop course: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        actions.add(dropBtn);

        JButton refreshBtn = new JButton("🔄 Refresh Courses");
        refreshBtn.setBackground(new Color(0x2A2A2A));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshData());
        actions.add(refreshBtn);

        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    // Helper to extract nested json object matching key
    private String getNestedJson(String raw, String key) {
        int index = raw.indexOf("\"" + key + "\"");
        if (index == -1) return "";
        int start = raw.indexOf("{", index);
        if (start == -1) return "";
        int braces = 1;
        int end = start + 1;
        while (braces > 0 && end < raw.length()) {
            char c = raw.charAt(end);
            if (c == '{') braces++;
            else if (c == '}') braces--;
            end++;
        }
        return raw.substring(start, end);
    }

    // =========================================================================
    // ADMIN STUDENT MANAGEMENT TAB
    // =========================================================================
    private JPanel createStudentManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Header controls
        JPanel ctrl = new JPanel(new BorderLayout());
        ctrl.setOpaque(false);
        JLabel title = new JLabel("SmartCampus Registered Students Directory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        ctrl.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        searchPanel.setOpaque(false);
        JTextField searchField = new JTextField(30);
        searchField.putClientProperty("JTextField.placeholderText", "🔍 Search by name, ID, email, programme...");
        searchField.setBackground(new Color(0x2A2A2A));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2D2D2D)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchPanel.add(searchField);
        ctrl.add(searchPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("Register Student");
        addBtn.setBackground(new Color(0xFF00BFA5));
        addBtn.setForeground(Color.BLACK);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.addActionListener(e -> showStudentForm(null));
        btnPanel.add(addBtn);

        JButton editBtn = new JButton("Edit Profile");
        editBtn.setBackground(new Color(0xFF3F51B5));
        editBtn.setForeground(Color.WHITE);
        editBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        editBtn.addActionListener(e -> {
            int row = studentsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student from table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = studentsTable.getValueAt(row, 0).toString();
            new Thread(() -> {
                try {
                    // Fetch full students list, locate the matching one
                    String listJson = restService.getStudents(session.getToken());
                    List<Map<String, String>> list = JsonParser.parseList(listJson);
                    for (Map<String, String> stud : list) {
                        if (id.equals(stud.get("id"))) {
                            SwingUtilities.invokeLater(() -> showStudentForm(stud));
                            return;
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }).start();
        });
        btnPanel.add(editBtn);

        JButton delBtn = new JButton("Delete Record");
        delBtn.setBackground(new Color(0xFFFF6B6B));
        delBtn.setForeground(Color.WHITE);
        delBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        delBtn.addActionListener(e -> {
            int row = studentsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student record to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = studentsTable.getValueAt(row, 0).toString();
            String name = studentsTable.getValueAt(row, 2).toString();
            
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete student '" + name + "'?\nThis will cascade and delete their user accounts and tokens immediately.", "Confirm deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    restService.deleteStudent(session.getToken(), id);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Student profile deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Failed to delete student: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        btnPanel.add(delBtn);

        ctrl.add(btnPanel, BorderLayout.EAST);
        panel.add(ctrl, BorderLayout.NORTH);

        // Students Table
        String[] cols = {"DB ID", "Matric No", "Name", "Email", "Programme", "Faculty", "Semester", "GPA"};
        studentsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        studentsTable = new JTable(studentsModel);
        studentsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        studentsTable.setRowHeight(25);

        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(studentsModel);
        studentsTable.setRowSorter(sorter);
        
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    try {
                        sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        panel.add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("🔄 Refresh Student Directory");
        refreshBtn.setBackground(new Color(0x2A2A2A));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshData());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void showStudentForm(Map<String, String> data) {
        boolean isEdit = (data != null);
        JDialog dialog = new JDialog(this, isEdit ? "Modify Student Details" : "Register New Student", true);
        dialog.setSize(400, isEdit ? 520 : 580);
        dialog.setLocationRelativeTo(this);

        JPanel dPanel = new JPanel(new GridBagLayout());
        dPanel.setBackground(new Color(0x1E1E1E));
        dPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        dialog.setContentPane(dPanel);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(6, 6, 6, 6);

        // Programme Type Dropdown (Add mode only)
        JComboBox<String> progTypeCombo = null;
        if (!isEdit) {
            JLabel progTypeLabel = new JLabel("Programme Type *");
            progTypeLabel.setForeground(Color.LIGHT_GRAY);
            progTypeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

            String[] programmeTypes = {"Diploma", "Bachelor / Degree", "Prasiswazah (Postgrad)"};
            progTypeCombo = new JComboBox<>(programmeTypes);
            progTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            progTypeCombo.setBackground(new Color(0x2A2A2A));
            progTypeCombo.setForeground(Color.WHITE);
            progTypeCombo.setSelectedItem("Bachelor / Degree");

            JPanel progTypePanel = new JPanel(new BorderLayout(0, 4));
            progTypePanel.setOpaque(false);
            progTypePanel.add(progTypeLabel, BorderLayout.NORTH);
            progTypePanel.add(progTypeCombo, BorderLayout.CENTER);
            dPanel.add(progTypePanel, c);
            c.gridy++;
        }

        // Faculty Dropdown (Both modes)
        JLabel facLabel = new JLabel("Faculty *");
        facLabel.setForeground(Color.LIGHT_GRAY);
        facLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        String[] faculties = {"FTMK", "FTKM", "FTKEK", "FTKIP", "FTKE", "FPTT"};
        JComboBox<String> facCombo = new JComboBox<>(faculties);
        facCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        facCombo.setBackground(new Color(0x2A2A2A));
        facCombo.setForeground(Color.WHITE);
        if (isEdit) {
            String val = data.get("faculty");
            if (val != null) {
                facCombo.setSelectedItem(val);
            }
        }

        JPanel facPanel = new JPanel(new BorderLayout(0, 4));
        facPanel.setOpaque(false);
        facPanel.add(facLabel, BorderLayout.NORTH);
        facPanel.add(facCombo, BorderLayout.CENTER);
        dPanel.add(facPanel, c);
        c.gridy++;

        JTextField nameField = new JTextField(isEdit ? data.get("name") : "");
        nameField.putClientProperty("JTextField.placeholderText", "Full Name");
        dPanel.add(nameField, c);

        JTextField emailField = null;
        if (isEdit) {
            emailField = new JTextField(data.get("email"));
            emailField.putClientProperty("JTextField.placeholderText", "Institutional Email");
            emailField.setEditable(false);
            emailField.setEnabled(false);
            c.gridy++;
            dPanel.add(emailField, c);
        }

        JTextField progField = new JTextField(isEdit ? data.get("programme") : "");
        progField.putClientProperty("JTextField.placeholderText", "Degree Programme Name (e.g. Bachelor of CS)");
        c.gridy++;
        dPanel.add(progField, c);

        JTextField semField = new JTextField(isEdit ? data.get("semester") : "1");
        semField.putClientProperty("JTextField.placeholderText", "Semester (e.g. 1)");
        c.gridy++;
        dPanel.add(semField, c);

        JTextField gpaField = new JTextField(isEdit ? data.get("gpa") : "3.5");
        gpaField.putClientProperty("JTextField.placeholderText", "GPA (e.g. 3.75)");
        c.gridy++;
        dPanel.add(gpaField, c);

        JTextField phoneField = new JTextField(isEdit ? data.get("phoneNumber") : "");
        phoneField.putClientProperty("JTextField.placeholderText", "Contact Number");
        c.gridy++;
        dPanel.add(phoneField, c);

        final JComboBox<String> finalProgTypeCombo = progTypeCombo;
        final JTextField finalEmailField = emailField;

        JButton saveBtn = new JButton(isEdit ? "Update Student" : "Register Student");
        saveBtn.setBackground(new Color(0xFF00BFA5));
        saveBtn.setForeground(Color.BLACK);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = isEdit ? finalEmailField.getText().trim() : "auto";
            String prog = progField.getText().trim();
            String fac = (String) facCombo.getSelectedItem();
            String sem = semField.getText().trim();
            String gpa = gpaField.getText().trim();
            String phone = phoneField.getText().trim();

            if (name.isEmpty() || email.isEmpty() || prog.isEmpty() || fac == null || fac.isEmpty() || sem.isEmpty() || gpa.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all student attributes.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new Thread(() -> {
                try {
                    if (isEdit) {
                        restService.updateStudent(session.getToken(), data.get("id"), name, email, prog, fac, sem, gpa, phone);
                    } else {
                        String progType = (String) finalProgTypeCombo.getSelectedItem();
                        String progCode = "B"; // default
                        if ("Diploma".equals(progType)) {
                            progCode = "D";
                        } else if ("Bachelor / Degree".equals(progType)) {
                            progCode = "B";
                        } else if ("Prasiswazah (Postgrad)".equals(progType)) {
                            progCode = "P";
                        }
                        restService.addStudent(session.getToken(), name, email, prog, fac, sem, gpa, phone, progCode);
                    }
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, isEdit ? "Student details updated!" : "Student registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Transaction failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        c.gridy++;
        c.insets = new Insets(15, 6, 6, 6);
        dPanel.add(saveBtn, c);

        dialog.setVisible(true);
    }

    // =========================================================================
    // ADMIN COURSE DIRECTORY TAB
    // =========================================================================
    private JPanel createCourseDirectoryTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        JPanel ctrl = new JPanel(new BorderLayout());
        ctrl.setOpaque(false);
        JLabel title = new JLabel("Campus Active Course Syllabus");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        ctrl.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        searchPanel.setOpaque(false);
        JTextField searchField = new JTextField(30);
        searchField.putClientProperty("JTextField.placeholderText", "🔍 Search by course code, title, lecturer...");
        searchField.setBackground(new Color(0x2A2A2A));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2D2D2D)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchPanel.add(searchField);
        ctrl.add(searchPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("Add Course");
        addBtn.setBackground(new Color(0xFF00BFA5));
        addBtn.setForeground(Color.BLACK);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.addActionListener(e -> showCourseForm(null));
        btnPanel.add(addBtn);

        JButton editBtn = new JButton("Edit Course");
        editBtn.setBackground(new Color(0xFF3F51B5));
        editBtn.setForeground(Color.WHITE);
        editBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        editBtn.addActionListener(e -> {
            int row = coursesTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a course from table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String courseCode = coursesTable.getValueAt(row, 0).toString();
            new Thread(() -> {
                try {
                    String listJson = restService.getCourses(session.getToken());
                    List<Map<String, String>> list = JsonParser.parseList(listJson);
                    for (Map<String, String> crs : list) {
                        if (courseCode.equals(crs.get("courseCode"))) {
                            SwingUtilities.invokeLater(() -> showCourseForm(crs));
                            return;
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }).start();
        });
        btnPanel.add(editBtn);

        JButton delBtn = new JButton("Delete Course");
        delBtn.setBackground(new Color(0xFFFF6B6B));
        delBtn.setForeground(Color.WHITE);
        delBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        delBtn.addActionListener(e -> {
            int row = coursesTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a course to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String courseCode = coursesTable.getValueAt(row, 0).toString();
            String titleStr = coursesTable.getValueAt(row, 1).toString();

            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete course '" + titleStr + "' (" + courseCode + ")?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    String listJson = restService.getCourses(session.getToken());
                    List<Map<String, String>> list = JsonParser.parseList(listJson);
                    String id = null;
                    for (Map<String, String> crs : list) {
                        if (courseCode.equals(crs.get("courseCode"))) {
                            id = crs.get("id");
                            break;
                        }
                    }
                    if (id == null) {
                        throw new Exception("Course not found in database.");
                    }
                    restService.deleteCourse(session.getToken(), id);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Course deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Failed to delete course: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        btnPanel.add(delBtn);

        ctrl.add(btnPanel, BorderLayout.EAST);
        panel.add(ctrl, BorderLayout.NORTH);

        // Table
        String[] cols = {"Course Code", "Title", "Lecturer", "Faculty", "Credits", "Sustained Cap", "Max Cap"};
        coursesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        coursesTable = new JTable(coursesModel);
        coursesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        coursesTable.setRowHeight(25);

        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(coursesModel);
        coursesTable.setRowSorter(sorter);
        
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    try {
                        sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        panel.add(new JScrollPane(coursesTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("🔄 Refresh Course Directory");
        refreshBtn.setBackground(new Color(0x2A2A2A));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshData());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void showCourseForm(Map<String, String> data) {
        boolean isEdit = (data != null);
        JDialog dialog = new JDialog(this, isEdit ? "Modify Course Details" : "Add New Course", true);
        dialog.setSize(400, 520);
        dialog.setLocationRelativeTo(this);

        JPanel dPanel = new JPanel(new GridBagLayout());
        dPanel.setBackground(new Color(0x1E1E1E));
        dPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        dialog.setContentPane(dPanel);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(6, 6, 6, 6);

        // Course Code Field
        JTextField codeField = new JTextField(isEdit && data.get("courseCode") != null ? data.get("courseCode") : "");
        codeField.putClientProperty("JTextField.placeholderText", "Course Code (e.g. CS101)");
        if (isEdit) {
            codeField.setEditable(false);
            codeField.setEnabled(false);
        }
        dPanel.add(codeField, c);
        c.gridy++;

        // Title Field
        JTextField titleField = new JTextField(isEdit && data.get("courseTitle") != null ? data.get("courseTitle") : "");
        titleField.putClientProperty("JTextField.placeholderText", "Course Title");
        dPanel.add(titleField, c);
        c.gridy++;

        // Lecturer Field
        JTextField lecturerField = new JTextField(isEdit && data.get("lecturer") != null ? data.get("lecturer") : "");
        lecturerField.putClientProperty("JTextField.placeholderText", "Lecturer Name");
        dPanel.add(lecturerField, c);
        c.gridy++;

        // Faculty Dropdown
        JLabel facLabel = new JLabel("Faculty *");
        facLabel.setForeground(Color.LIGHT_GRAY);
        facLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        String[] faculties = {"FTMK", "FTKM", "FTKEK", "FTKIP", "FTKE", "FPTT"};
        JComboBox<String> facCombo = new JComboBox<>(faculties);
        facCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        facCombo.setBackground(new Color(0x2A2A2A));
        facCombo.setForeground(Color.WHITE);
        if (isEdit) {
            String val = data.get("faculty");
            if (val != null) {
                facCombo.setSelectedItem(val);
            }
        }

        JPanel facPanel = new JPanel(new BorderLayout(0, 4));
        facPanel.setOpaque(false);
        facPanel.add(facLabel, BorderLayout.NORTH);
        facPanel.add(facCombo, BorderLayout.CENTER);
        dPanel.add(facPanel, c);
        c.gridy++;

        // Credit Hours Field
        JTextField creditField = new JTextField(isEdit && data.get("creditHours") != null ? data.get("creditHours") : "3");
        creditField.putClientProperty("JTextField.placeholderText", "Credit Hours (e.g. 3)");
        dPanel.add(creditField, c);
        c.gridy++;

        // Max Capacity Field
        JTextField capacityField = new JTextField(isEdit && data.get("maxCapacity") != null ? data.get("maxCapacity") : "30");
        capacityField.putClientProperty("JTextField.placeholderText", "Maximum Capacity (e.g. 30)");
        dPanel.add(capacityField, c);
        c.gridy++;

        // Semester Field
        JTextField semesterField = new JTextField(isEdit && data.get("semester") != null ? data.get("semester") : "2025/2026 SEM 1");
        semesterField.putClientProperty("JTextField.placeholderText", "Semester (e.g. 2024/2025 SEM 1)");
        dPanel.add(semesterField, c);
        c.gridy++;

        JButton saveBtn = new JButton(isEdit ? "Update Course" : "Add Course");
        saveBtn.setBackground(new Color(0xFF00BFA5));
        saveBtn.setForeground(Color.BLACK);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            String title = titleField.getText().trim();
            String lecturer = lecturerField.getText().trim();
            String faculty = (String) facCombo.getSelectedItem();
            String credit = creditField.getText().trim();
            String capacity = capacityField.getText().trim();
            String semester = semesterField.getText().trim();

            if (code.isEmpty() || title.isEmpty() || lecturer.isEmpty() || faculty == null || faculty.isEmpty() || credit.isEmpty() || capacity.isEmpty() || semester.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all course attributes.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Numeric check
            try {
                Integer.parseInt(credit);
                Integer.parseInt(capacity);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Credit Hours and Max Capacity must be integers.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new Thread(() -> {
                try {
                    if (isEdit) {
                        restService.updateCourse(session.getToken(), data.get("id"), title, lecturer, faculty, credit, capacity, semester);
                    } else {
                        restService.addCourse(session.getToken(), code, title, lecturer, faculty, credit, capacity, semester);
                    }
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, isEdit ? "Course details updated!" : "Course added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshData();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Transaction failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        c.gridy++;
        c.insets = new Insets(15, 6, 6, 6);
        dPanel.add(saveBtn, c);

        dialog.setVisible(true);
    }

    // =========================================================================
    // DATA REFRESHERS
    // =========================================================================
    private void refreshData() {
        new Thread(() -> {
            try {
                if (session.isAdmin()) {
                    // Fetch System Dashboard
                    String dashJson = restService.getDashboardData(session.getToken());
                    Map<String, String> dash = JsonParser.parseObject(dashJson);
                    
                    // Fetch stats
                    String totalStudents = dash.get("totalStudents");
                    String totalRoomBookings = dash.get("totalRoomBookings");
                    String totalBookLoans = dash.get("totalBookLoans");

                    // Fetch details
                    String allLoansJson = getNestedJsonList(dashJson, "allBookLoans");
                    List<Map<String, String>> allLoans = JsonParser.parseList(allLoansJson);

                    String allBookingsJson = getNestedJsonList(dashJson, "allRoomBookings");
                    List<Map<String, String>> allBookings = JsonParser.parseList(allBookingsJson);

                    // Fetch students
                    String studentsJson = restService.getStudents(session.getToken());
                    List<Map<String, String>> studentsList = JsonParser.parseList(studentsJson);

                    // Fetch courses
                    String coursesJson = restService.getCourses(session.getToken());
                    List<Map<String, String>> coursesList = JsonParser.parseList(coursesJson);

                    // Calculate total course enrolments from course capacities
                    int totalEnrol = 0;
                    if (coursesList != null) {
                        for (Map<String, String> c : coursesList) {
                            String capStr = c.get("currentCapacity");
                            if (capStr != null) {
                                try {
                                    totalEnrol += Integer.parseInt(capStr);
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                    String totalEnrolments = String.valueOf(totalEnrol);

                    SwingUtilities.invokeLater(() -> {
                        if (statStudentsVal != null) statStudentsVal.setText(totalStudents);
                        if (statEnrolmentsVal != null) statEnrolmentsVal.setText(totalEnrolments);
                        if (statBookingsVal != null) statBookingsVal.setText(totalRoomBookings);
                        if (statLoansVal != null) statLoansVal.setText(totalBookLoans);

                        // Bookings Table
                        adminBookingsModel.setRowCount(0);
                        for (Map<String, String> bk : allBookings) {
                            adminBookingsModel.addRow(new Object[]{
                                bk.get("bookingReference"),
                                bk.get("studentId"),
                                bk.get("roomName"),
                                bk.get("slot"),
                                bk.get("bookingDate")
                            });
                        }

                        // Loans Table
                        adminLoansModel.setRowCount(0);
                        for (Map<String, String> ln : allLoans) {
                            adminLoansModel.addRow(new Object[]{
                                ln.get("loanReference"),
                                ln.get("studentId"),
                                ln.get("bookIsbn"),
                                ln.get("dueDate"),
                                ln.get("status")
                            });
                        }

                        // Students Table
                        studentsModel.setRowCount(0);
                        for (Map<String, String> st : studentsList) {
                            studentsModel.addRow(new Object[]{
                                st.get("id"),
                                st.get("studentId"),
                                st.get("name"),
                                st.get("email"),
                                st.get("programme"),
                                st.get("faculty"),
                                st.get("semester"),
                                st.get("gpa")
                            });
                        }

                        // Courses Table
                        coursesModel.setRowCount(0);
                        for (Map<String, String> cr : coursesList) {
                            coursesModel.addRow(new Object[]{
                                cr.get("courseCode"),
                                cr.get("courseTitle"),
                                cr.get("lecturer"),
                                cr.get("faculty"),
                                cr.get("creditHours"),
                                cr.get("currentCapacity"),
                                cr.get("maxCapacity")
                            });
                        }
                    });
                } else {
                    // Fetch Student Hub
                    String dashJson = restService.getDashboardData(session.getToken());
                    Map<String, String> dash = JsonParser.parseObject(dashJson);

                    // Profile
                    String profileJson = getNestedJson(dashJson, "profile");
                    Map<String, String> prof = JsonParser.parseObject(profileJson);

                    // Enrolments
                    String enrolsJson = getNestedJsonList(dashJson, "enrolments");
                    List<Map<String, String>> enrols = JsonParser.parseList(enrolsJson);

                    // Bookings
                    String bookingsJson = getNestedJsonList(dashJson, "roomBookings");
                    List<Map<String, String>> bookings = JsonParser.parseList(bookingsJson);

                    // Loans
                    String loansJson = getNestedJsonList(dashJson, "bookLoans");
                    List<Map<String, String>> loans = JsonParser.parseList(loansJson);

                    // Notifications
                    String notifiesJson = getNestedJsonList(dashJson, "notifications");
                    List<Map<String, String>> notifies = JsonParser.parseList(notifiesJson);

                    // Courses
                    String coursesJson = restService.getCourses(session.getToken());
                    List<Map<String, String>> coursesList = JsonParser.parseList(coursesJson);

                    SwingUtilities.invokeLater(() -> {
                        welcomeLabel.setText("Welcome, " + prof.get("name") + " (" + prof.get("studentId") + ")");
                        gpaLabel.setText("CGPA: " + prof.get("gpa"));
                        semesterLabel.setText("Semester: " + prof.get("semester"));
                        progLabel.setText("Programme: " + prof.get("programme"));
                        facLabel.setText("Faculty: " + prof.get("faculty"));
                        emailLabel.setText("Institutional Email: " + prof.get("email"));
                        phoneLabel.setText("Contact: " + prof.get("phoneNumber"));

                        // Enrolments Table
                        studEnrolModel.setRowCount(0);
                        for (Map<String, String> en : enrols) {
                            if ("ACTIVE".equalsIgnoreCase(en.get("status"))) {
                                studEnrolModel.addRow(new Object[]{
                                    en.get("courseCode"),
                                    en.get("courseTitle"),
                                    en.get("enrolledAt") != null ? en.get("enrolledAt").substring(0, 10) : ""
                                });
                            }
                        }

                        // Bookings Table
                        studBookingsModel.setRowCount(0);
                        for (Map<String, String> bk : bookings) {
                            studBookingsModel.addRow(new Object[]{
                                bk.get("bookingReference"),
                                bk.get("roomName"),
                                bk.get("slot"),
                                bk.get("bookingDate")
                            });
                        }

                        // Loans Table
                        studLoansModel.setRowCount(0);
                        for (Map<String, String> ln : loans) {
                            studLoansModel.addRow(new Object[]{
                                ln.get("loanReference"),
                                ln.get("bookTitle"),
                                ln.get("dueDate"),
                                ln.get("status")
                            });
                        }

                        // Notifications Table
                        notificationModel.setRowCount(0);
                        for (Map<String, String> nt : notifies) {
                            notificationModel.addRow(new Object[]{
                                nt.get("message"),
                                nt.get("createdAt") != null ? nt.get("createdAt").substring(0, 16).replace("T", " ") : ""
                            });
                        }

                        // Catalog / Syllabus Table
                        java.util.Set<String> enrolledCourseCodes = new java.util.HashSet<>();
                        for (Map<String, String> en : enrols) {
                            if ("ACTIVE".equalsIgnoreCase(en.get("status"))) {
                                String code = en.get("courseCode");
                                if (code != null) enrolledCourseCodes.add(code);
                            }
                        }

                        coursesModel.setRowCount(0);
                        for (Map<String, String> cr : coursesList) {
                            String code = cr.get("courseCode");
                            String status = enrolledCourseCodes.contains(code) ? "Enrolled" : "Available";
                            coursesModel.addRow(new Object[]{
                                code,
                                cr.get("courseTitle"),
                                cr.get("lecturer"),
                                cr.get("creditHours"),
                                cr.get("currentCapacity"),
                                cr.get("maxCapacity"),
                                status
                            });
                        }
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        }).start();
    }

    private String getNestedJsonList(String raw, String key) {
        int index = raw.indexOf("\"" + key + "\"");
        if (index == -1) return "";
        int start = raw.indexOf("[", index);
        if (start == -1) return "";
        int brackets = 1;
        int end = start + 1;
        while (brackets > 0 && end < raw.length()) {
            char c = raw.charAt(end);
            if (c == '[') brackets++;
            else if (c == ']') brackets--;
            end++;
        }
        return raw.substring(start, end);
    }
}
