package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import service.SoapService;
import service.RestService;
import util.JsonParser;
import util.SessionManager;

public class RoomBookingPanel extends JPanel {
    private final SoapService soapService = new SoapService();
    private final RestService restService = new RestService();
    private final SessionManager session = SessionManager.getInstance();

    private JComboBox<String> roomComboBox;
    private JComboBox<String> slotComboBox;
    private JTextField dateField;
    private JTextField purposeField;

    // Admin overrides
    private JTextField studentIdField;
    private JTextField studentNameField;

    // Availability status
    private JButton checkBtn;
    private JLabel statusLabel;

    // Booking actions
    private JButton bookBtn;

    // Bookings list tab
    private JTable bookingsTable;
    private DefaultTableModel bookingsModel;

    private final String[] rooms = {
        "Discussion Room Alpha",
        "Discussion Room Beta",
        "Programming Lab B",
        "Multipurpose Hall",
        "Mini Seminar Room"
    };

    private final String[] slots = {
        "08:00 - 10:00",
        "10:00 - 12:00",
        "12:00 - 14:00",
        "14:00 - 16:00",
        "16:00 - 18:00"
    };

    public RoomBookingPanel() {
        setLayout(new BorderLayout(15, 15));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        tabs.addTab("📅 Book a Slot", createFormTab());
        tabs.addTab("📋 Active Bookings", createBookingsListTab());

        add(tabs, BorderLayout.CENTER);

        // Preload bookings
        SwingUtilities.invokeLater(this::refreshBookings);
    }

    private JPanel createFormTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Upper Description Banner
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setBackground(new Color(0x1E1E1E));
        descPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2D2D2D)),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel descLabel = new JLabel();
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(new Color(0xCCCCCC));
        if (session.isAdmin()) {
            descLabel.setText("<html>As an Administrator, you can reserve slots on behalf of students. Enter their credentials below.</html>");
        } else {
            descLabel.setText("<html>Select your preferred study room, time slot, and date. Booking is verified live in our SOAP database.</html>");
        }
        descPanel.add(descLabel, BorderLayout.CENTER);
        panel.add(descPanel, BorderLayout.NORTH);

        // GridBag layout panel for form
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(new Color(0x1E1E1E));
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x2D2D2D)), "Book Study Room Slot (SOAP)", 0, 0, new Font("Segoe UI", Font.BOLD, 13), Color.WHITE),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.weighty = 0.0;

        // Row 0, Left: Room ComboBox
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        JPanel roomPanel = new JPanel(new BorderLayout(0, 5));
        roomPanel.setOpaque(false);
        JLabel rLbl = new JLabel("Select Room Name:");
        rLbl.setForeground(Color.WHITE);
        rLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roomComboBox = new JComboBox<>(rooms);
        roomComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomComboBox.setBackground(new Color(0x2A2A2A));
        roomComboBox.setForeground(Color.WHITE);
        roomComboBox.addActionListener(e -> resetStatusLabel());
        roomPanel.add(rLbl, BorderLayout.NORTH);
        roomPanel.add(roomComboBox, BorderLayout.CENTER);
        formCard.add(roomPanel, gbc);

        // Row 0, Right: Slot ComboBox
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        JPanel slotPanel = new JPanel(new BorderLayout(0, 5));
        slotPanel.setOpaque(false);
        JLabel sLbl = new JLabel("Select Time Slot:");
        sLbl.setForeground(Color.WHITE);
        sLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        slotComboBox = new JComboBox<>(slots);
        slotComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        slotComboBox.setBackground(new Color(0x2A2A2A));
        slotComboBox.setForeground(Color.WHITE);
        slotComboBox.addActionListener(e -> resetStatusLabel());
        slotPanel.add(sLbl, BorderLayout.NORTH);
        slotPanel.add(slotComboBox, BorderLayout.CENTER);
        formCard.add(slotPanel, gbc);

        // Row 1, Left: Date input
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        JPanel datePanel = new JPanel(new BorderLayout(0, 5));
        datePanel.setOpaque(false);
        JLabel dLbl = new JLabel("Date (YYYY-MM-DD):");
        dLbl.setForeground(Color.WHITE);
        dLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dateField = new JTextField(java.time.LocalDate.now().plusDays(1).toString());
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateField.setBackground(new Color(0x2A2A2A));
        dateField.setForeground(Color.WHITE);
        dateField.setCaretColor(Color.WHITE);
        dateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        dateField.putClientProperty("JTextField.placeholderText", "Date (e.g. 2026-06-18)");
        dateField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { resetStatusLabel(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { resetStatusLabel(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { resetStatusLabel(); }
        });
        datePanel.add(dLbl, BorderLayout.NORTH);
        datePanel.add(dateField, BorderLayout.CENTER);
        formCard.add(datePanel, gbc);

        // Row 1, Right: Purpose input
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        JPanel purposePanel = new JPanel(new BorderLayout(0, 5));
        purposePanel.setOpaque(false);
        JLabel pLbl = new JLabel("Booking Purpose / Description:");
        pLbl.setForeground(Color.WHITE);
        pLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        purposeField = new JTextField();
        purposeField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        purposeField.setBackground(new Color(0x2A2A2A));
        purposeField.setForeground(Color.WHITE);
        purposeField.setCaretColor(Color.WHITE);
        purposeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        purposeField.putClientProperty("JTextField.placeholderText", "Enter booking description or purpose...");
        purposePanel.add(pLbl, BorderLayout.NORTH);
        purposePanel.add(purposeField, BorderLayout.CENTER);
        formCard.add(purposePanel, gbc);

        // Row 2, Admin overrides
        if (session.isAdmin()) {
            // Row 2, Left: Student ID
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.5;
            JPanel studIdPanel = new JPanel(new BorderLayout(0, 5));
            studIdPanel.setOpaque(false);
            JLabel studIdLbl = new JLabel("Student Matric ID:");
            studIdLbl.setForeground(new Color(0xFF00BFA5));
            studIdLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            studentIdField = new JTextField();
            studentIdField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            studentIdField.setBackground(new Color(0x2A2A2A));
            studentIdField.setForeground(Color.WHITE);
            studentIdField.setCaretColor(Color.WHITE);
            studentIdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFF00BFA5), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            studentIdField.putClientProperty("JTextField.placeholderText", "Matric ID (e.g. B032310001)");
            studIdPanel.add(studIdLbl, BorderLayout.NORTH);
            studIdPanel.add(studentIdField, BorderLayout.CENTER);
            formCard.add(studIdPanel, gbc);

            // Row 2, Right: Student Name
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.weightx = 0.5;
            JPanel studNamePanel = new JPanel(new BorderLayout(0, 5));
            studNamePanel.setOpaque(false);
            JLabel studNameLbl = new JLabel("Student Full Name:");
            studNameLbl.setForeground(new Color(0xFF00BFA5));
            studNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            studentNameField = new JTextField();
            studentNameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            studentNameField.setBackground(new Color(0x2A2A2A));
            studentNameField.setForeground(Color.WHITE);
            studentNameField.setCaretColor(Color.WHITE);
            studentNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFF00BFA5), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            studentNameField.putClientProperty("JTextField.placeholderText", "Student Full Name");
            studNamePanel.add(studNameLbl, BorderLayout.NORTH);
            studNamePanel.add(studentNameField, BorderLayout.CENTER);
            formCard.add(studNamePanel, gbc);
        }

        // Row 3: Availability controls (span width)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(15, 15, 5, 15);
        
        JPanel checkPanel = new JPanel(new GridBagLayout());
        checkPanel.setOpaque(false);
        GridBagConstraints cbc = new GridBagConstraints();
        cbc.fill = GridBagConstraints.HORIZONTAL;
        cbc.gridx = 0;
        cbc.gridy = 0;
        cbc.weightx = 0.3;
        
        checkBtn = new JButton("Check Slot Availability");
        checkBtn.setBackground(new Color(0x2D2D2D));
        checkBtn.setForeground(Color.WHITE);
        checkBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        checkBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        checkBtn.addActionListener(e -> performCheckAvailability());
        checkPanel.add(checkBtn, cbc);

        cbc.gridx = 1;
        cbc.weightx = 0.7;
        cbc.insets = new Insets(0, 15, 0, 0);
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(0x2A2A2A));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3D3D3D)),
            BorderFactory.createEmptyBorder(9, 15, 9, 15)
        ));
        checkPanel.add(statusLabel, cbc);
        formCard.add(checkPanel, gbc);

        // Row 4: Confirm Booking button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(15, 15, 15, 15);
        
        bookBtn = new JButton("CONFIRM BOOKING");
        bookBtn.setBackground(new Color(0xFF3F51B5));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bookBtn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        bookBtn.addActionListener(e -> performCreateBooking());
        formCard.add(bookBtn, gbc);

        panel.add(formCard, BorderLayout.CENTER);
        return panel;
    }

    private void resetStatusLabel() {
        statusLabel.setText(" ");
        statusLabel.setBackground(new Color(0x2A2A2A));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3D3D3D)),
            BorderFactory.createEmptyBorder(9, 15, 9, 15)
        ));
    }

    private JPanel createBookingsListTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Description header
        JLabel listDesc = new JLabel();
        listDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        listDesc.setForeground(new Color(0x8A8A8A));
        listDesc.setBorder(new EmptyBorder(5, 5, 5, 5));
        if (session.isAdmin()) {
            listDesc.setText("Total Campus bookings registry ledger (Admin audit access).");
        } else {
            listDesc.setText("Logs of study slots under your personal Matric account.");
        }
        panel.add(listDesc, BorderLayout.NORTH);

        // Table setup
        String[] cols;
        if (session.isAdmin()) {
            cols = new String[]{"Reference", "Student ID", "Room Name", "Slot", "Date", "Status", "Actions"};
        } else {
            cols = new String[]{"Reference", "Room Name", "Slot", "Date", "Status", "Actions"};
        }

        bookingsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Actions column is at index 6 for admin, index 5 for student
                int actionIndex = session.isAdmin() ? 6 : 5;
                return col == actionIndex;
            }
        };

        bookingsTable = new JTable(bookingsModel);
        bookingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bookingsTable.setRowHeight(30);

        int actionIndex = session.isAdmin() ? 6 : 5;
        bookingsTable.getColumnModel().getColumn(actionIndex).setCellRenderer(new ButtonRenderer());
        bookingsTable.getColumnModel().getColumn(actionIndex).setCellEditor(new ButtonEditor(new JTextField()));

        // Status Renderer
        int statusIndex = session.isAdmin() ? 5 : 4;
        bookingsTable.getColumnModel().getColumn(statusIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String status = value.toString();
                    if ("CONFIRMED".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFF00BFA5)); // Mint Green
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else if ("CANCELLED".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFFFF6B6B)); // Crimson Red
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setForeground(Color.WHITE);
                    }
                }
                return label;
            }
        });

        JScrollPane scroll = new JScrollPane(bookingsTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("🔄 Refresh Active Bookings");
        refreshBtn.setBackground(new Color(0x2A2A2A));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshBookings());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void performCheckAvailability() {
        String room = (String) roomComboBox.getSelectedItem();
        String slot = (String) slotComboBox.getSelectedItem();
        String date = dateField.getText().trim();

        if (date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Date.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        checkBtn.setEnabled(false);
        checkBtn.setText("Checking...");
        statusLabel.setText("Checking availability...");
        statusLabel.setForeground(Color.LIGHT_GRAY);

        new Thread(() -> {
            try {
                boolean avail = soapService.checkAvailability(room, slot, date);
                SwingUtilities.invokeLater(() -> {
                    checkBtn.setEnabled(true);
                    checkBtn.setText("Check Slot Availability");
                    if (avail) {
                        statusLabel.setText("Slot is AVAILABLE for Booking");
                        statusLabel.setBackground(new Color(0x1C3A27)); // Dark green
                        statusLabel.setForeground(new Color(0xFF00BFA5)); // Mint Green
                        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0x2E5A3E)),
                            BorderFactory.createEmptyBorder(9, 15, 9, 15)
                        ));
                    } else {
                        statusLabel.setText("Slot is ALREADY BOOKED");
                        statusLabel.setBackground(new Color(0x3A1C1C)); // Dark red
                        statusLabel.setForeground(new Color(0xFFFF6B6B)); // Crimson Red
                        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0x5A2A2A)),
                            BorderFactory.createEmptyBorder(9, 15, 9, 15)
                        ));
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    checkBtn.setEnabled(true);
                    checkBtn.setText("Check Slot Availability");
                    statusLabel.setText("Check failed: " + ex.getMessage());
                    statusLabel.setForeground(new Color(0xFFFF6B6B));
                });
            }
        }).start();
    }

    private void performCreateBooking() {
        String room = (String) roomComboBox.getSelectedItem();
        String slot = (String) slotComboBox.getSelectedItem();
        String date = dateField.getText().trim();
        String purpose = purposeField.getText().trim();

        String studentId;
        String studentName;

        if (session.isAdmin()) {
            studentId = studentIdField.getText().trim();
            studentName = studentNameField.getText().trim();
        } else {
            studentId = session.getUserId();
            studentName = session.getFullName();
        }

        if (date.isEmpty() || purpose.isEmpty() || studentId.isEmpty() || studentName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all booking fields.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        bookBtn.setEnabled(false);
        bookBtn.setText("CONFIRMING BOOKING...");

        new Thread(() -> {
            try {
                String ref = soapService.bookRoom(studentId, studentName, room, slot, date, purpose);
                SwingUtilities.invokeLater(() -> {
                    bookBtn.setEnabled(true);
                    bookBtn.setText("CONFIRM BOOKING");
                    purposeField.setText("");
                    if (session.isAdmin()) {
                        studentIdField.setText("");
                        studentNameField.setText("");
                    }
                    resetStatusLabel();

                    // Confirmation Dialog with Ref
                    JPanel content = new JPanel(new GridLayout(0, 1, 5, 5));
                    content.add(new JLabel("Your room slot has been successfully registered."));
                    content.add(new JLabel("Booking Reference Code:"));
                    JTextField refText = new JTextField(ref);
                    refText.setEditable(false);
                    refText.setFont(new Font("Monospaced", Font.BOLD, 14));
                    refText.setHorizontalAlignment(JTextField.CENTER);
                    refText.setForeground(new Color(0xFF00BFA5));
                    content.add(refText);

                    JOptionPane.showMessageDialog(this, content, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

                    refreshBookings();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    bookBtn.setEnabled(true);
                    bookBtn.setText("CONFIRM BOOKING");
                    JOptionPane.showMessageDialog(this, "Booking Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public void refreshBookings() {
        new Thread(() -> {
            try {
                String dashboardJson = restService.getDashboardData(session.getToken());
                String arrayKey = session.isAdmin() ? "allRoomBookings" : "roomBookings";
                String bookingsJson = getNestedJsonList(dashboardJson, arrayKey);
                List<Map<String, String>> bookingsList = JsonParser.parseList(bookingsJson);

                SwingUtilities.invokeLater(() -> {
                    bookingsModel.setRowCount(0);
                    for (Map<String, String> bk : bookingsList) {
                        String ref = bk.get("bookingReference");
                        String room = bk.get("roomName");
                        String slot = bk.get("slot");
                        String date = bk.get("bookingDate");
                        String status = bk.get("status");

                        if (session.isAdmin()) {
                            String sid = bk.get("studentId");
                            bookingsModel.addRow(new Object[]{
                                ref, sid, room, slot, date, status, "Cancel"
                            });
                        } else {
                            bookingsModel.addRow(new Object[]{
                                ref, room, slot, date, status, "Cancel"
                            });
                        }
                    }
                });
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

    // Button cell renderer
    private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(0xFFE53935)); // Red
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String status = table.getValueAt(row, session.isAdmin() ? 5 : 4).toString();
            if ("CANCELLED".equalsIgnoreCase(status)) {
                setEnabled(false);
                setText("Cancelled");
            } else {
                setEnabled(true);
                setText("Cancel");
            }
            return this;
        }
    }

    // Button cell editor
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean clicked;
        private int selectedRow;

        public ButtonEditor(JTextField txt) {
            super(txt);
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(new Color(0xFFE53935));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "Cancel" : value.toString();
            button.setText(label);
            selectedRow = row;
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (clicked) {
                clicked = false;
                String ref = bookingsTable.getValueAt(selectedRow, 0).toString();
                String status = bookingsTable.getValueAt(selectedRow, session.isAdmin() ? 5 : 4).toString();

                if ("CANCELLED".equalsIgnoreCase(status)) {
                    return label;
                }

                int confirm = JOptionPane.showConfirmDialog(button, "Are you sure you want to cancel room booking reference:\n" + ref + "?", "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    new Thread(() -> {
                        try {
                            boolean success = soapService.cancelBooking(ref);
                            SwingUtilities.invokeLater(() -> {
                                if (success) {
                                    JOptionPane.showMessageDialog(button, "Booking cancelled successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(button, "Booking cancellation failed.", "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                refreshBookings();
                            });
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(button, "Failed to cancel: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                }
            }
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }
}
