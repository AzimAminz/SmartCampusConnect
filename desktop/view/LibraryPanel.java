package view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import model.Book;
import model.BookLoan;
import service.SoapService;
import util.SessionManager;

public class LibraryPanel extends JPanel {
    private final SoapService soapService = new SoapService();
    private final SessionManager session = SessionManager.getInstance();

    private JTextField searchField;
    private JTable catalogTable;
    private DefaultTableModel catalogModel;

    // Admin Components
    private JPanel adminSection;
    private JTable loansTable;
    private DefaultTableModel loansModel;
    
    // History Components
    private JTextField historySearchField;
    private JTable historyTable;
    private DefaultTableModel historyModel;

    public LibraryPanel() {
        setLayout(new BorderLayout(15, 15));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Create main split or tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Book Catalog Search
        tabs.addTab("📖 Book Catalog", createCatalogTab());

        // Tab 2: Personal / Student Loan History (or Admin Loan Management)
        if (session.isAdmin()) {
            tabs.addTab("⚙️ Manage Library & Loans", createAdminManagementTab());
            tabs.addTab("📜 Global Loan History", createHistoryTab(true));
        } else {
            tabs.addTab("📜 My Borrow History", createHistoryTab(false));
        }

        add(tabs, BorderLayout.CENTER);
        
        // Initial load of catalog
        refreshCatalog("");
    }

    private JPanel createCatalogTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Search Bar Panel
        JPanel searchBar = new JPanel(new BorderLayout(10, 10));
        searchBar.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.putClientProperty("JTextField.placeholderText", "Search books by title, author, category or ISBN...");
        searchField.addActionListener(e -> refreshCatalog(searchField.getText().trim()));
        searchBar.add(searchField, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastPanel.setOpaque(false);

        JButton searchBtn = new JButton("Search Catalog");
        searchBtn.setBackground(new Color(0xFF3F51B5));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchBtn.addActionListener(e -> refreshCatalog(searchField.getText().trim()));
        eastPanel.add(searchBtn);

        if (session.isAdmin()) {
            JButton addBtn = new JButton("Add New Book");
            addBtn.setBackground(new Color(0xFF00BFA5));
            addBtn.setForeground(Color.BLACK);
            addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            addBtn.addActionListener(e -> showAddBookDialog());
            eastPanel.add(addBtn);
        }

        searchBar.add(eastPanel, BorderLayout.EAST);

        panel.add(searchBar, BorderLayout.NORTH);

        // Catalog Table
        String[] cols;
        if (session.isAdmin()) {
            cols = new String[]{"ID", "ISBN", "Title", "Author", "Category", "Status", "Actions"};
        } else {
            cols = new String[]{"ID", "ISBN", "Title", "Author", "Category", "Status"};
        }

        catalogModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return session.isAdmin() && col == 6;
            }
        };
        catalogTable = new JTable(catalogModel);
        catalogTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        catalogTable.setRowHeight(session.isAdmin() ? 28 : 25);
        catalogTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        if (session.isAdmin()) {
            catalogTable.getColumnModel().getColumn(6).setCellRenderer(new CatalogButtonRenderer());
            catalogTable.getColumnModel().getColumn(6).setCellEditor(new CatalogButtonEditor(new JTextField()));
        }
        
        JScrollPane scroll = new JScrollPane(catalogTable);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAdminManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] loanCols = {"Reference", "Student ID", "Book ISBN", "Due Date", "Status", "Actions"};
        loansModel = new DefaultTableModel(loanCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5; // Allow button column to be editable/clickable
            }
        };
        
        loansTable = new JTable(loansModel);
        loansTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loansTable.setRowHeight(30);

        // Custom action for "Return Book" button column
        loansTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        loansTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JTextField()));

        JScrollPane loanScroll = new JScrollPane(loansTable);
        panel.add(loanScroll, BorderLayout.CENTER);

        JButton refreshLoansBtn = new JButton("🔄 Refresh Active Loans");
        refreshLoansBtn.setBackground(new Color(0x2A2A2A));
        refreshLoansBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshLoansBtn.addActionListener(e -> refreshActiveLoans());
        panel.add(refreshLoansBtn, BorderLayout.SOUTH);

        // Initial load of active loans
        SwingUtilities.invokeLater(this::refreshActiveLoans);

        return panel;
    }

    private JPanel createHistoryTab(boolean isAdmin) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Search Bar Panel
        JPanel searchBar = new JPanel(new BorderLayout(10, 10));
        searchBar.setOpaque(false);

        historySearchField = new JTextField();
        historySearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (isAdmin) {
            historySearchField.putClientProperty("JTextField.placeholderText", "Enter Student Matric ID or Book ISBN...");
        } else {
            historySearchField.setText(session.getUserId());
            historySearchField.setEditable(false);
        }
        
        historySearchField.addActionListener(e -> refreshHistory(isAdmin));
        searchBar.add(historySearchField, BorderLayout.CENTER);

        JButton searchBtn = new JButton(isAdmin ? "Search History" : "Reload My History");
        searchBtn.setBackground(new Color(0xFF3F51B5));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchBtn.addActionListener(e -> refreshHistory(isAdmin));
        searchBar.add(searchBtn, BorderLayout.EAST);

        panel.add(searchBar, BorderLayout.NORTH);

        // History Table
        String[] cols = {"Reference", "Student Name", "Book Title", "Borrowed Date", "Due Date", "Returned Date", "Status", "Fine (RM)"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        historyTable = new JTable(historyModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.setRowHeight(25);
        
        // Color coder for status and fines
        historyTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    String status = value.toString();
                    if ("RETURNED".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFF00BFA5)); // Mint Green
                    } else if ("OVERDUE".equalsIgnoreCase(status) || "LOST".equalsIgnoreCase(status)) {
                        label.setForeground(new Color(0xFFFF6B6B)); // Crimson Red
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else {
                        label.setForeground(Color.WHITE);
                    }
                }
                return label;
            }
        });
        
        JScrollPane scroll = new JScrollPane(historyTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshHistoryBtn = new JButton(isAdmin ? "🔄 Refresh Global History Log" : "🔄 Refresh My Borrow History");
        refreshHistoryBtn.setBackground(new Color(0x2A2A2A));
        refreshHistoryBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshHistoryBtn.addActionListener(e -> refreshHistory(isAdmin));
        panel.add(refreshHistoryBtn, BorderLayout.SOUTH);

        // Preload history
        SwingUtilities.invokeLater(() -> refreshHistory(isAdmin));

        return panel;
    }

    private void refreshCatalog(String query) {
        new Thread(() -> {
            try {
                List<Book> books = soapService.searchBooks(query);
                SwingUtilities.invokeLater(() -> {
                    catalogModel.setRowCount(0);
                    for (Book b : books) {
                        if (session.isAdmin()) {
                            catalogModel.addRow(new Object[]{
                                b.getId(),
                                b.getIsbn(),
                                b.getTitle(),
                                b.getAuthor(),
                                b.getCategory(),
                                b.getStatus(),
                                "AVAILABLE".equalsIgnoreCase(b.getStatus()) ? "Issue" : "Borrowed"
                            });
                        } else {
                            catalogModel.addRow(new Object[]{
                                b.getId(),
                                b.getIsbn(),
                                b.getTitle(),
                                b.getAuthor(),
                                b.getCategory(),
                                b.getStatus()
                            });
                        }
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Failed to load book catalog: " + e.getMessage(), "SOAP Web Service Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void refreshActiveLoans() {
        if (!session.isAdmin()) return;
        new Thread(() -> {
            try {
                // To list active loans, we can search book loans.
                // We'll search by empty query to get catalog list and then search student history.
                // However, SoapService has getStudentLoanHistory or getBookLoanHistory.
                // Let's call searchBooks("") to see what is BORROWED and search student history or get student history.
                // Wait! To list all loans, the admin can call searchBooks("") then look for status BORROWED.
                // Alternatively, let's search all student loan histories.
                // Wait, does the soap service have a "get all book loans" or can we fetch them?
                // Let's check: CampusBookingSoapService exposes getStudentLoanHistory(token, studentId).
                // Wait, does the SOAP service have another operation to list all loans?
                // Let's check CampusBookingSoapService lines 265-291:
                // Operation 8: getBookLoanHistory(token, isbn)
                // Operation 9: getStudentLoanHistory(token, studentId)
                // Wait! How does the Admin dashboard fetch all book loans?
                // In DashboardController (REST API):
                // adminDash.put("allBookLoans", bookLoanRepository.findAll());
                // Ah! The REST dashboard endpoint returns all book loans for Admin!
                // Let's see: `REST /api/dashboard` returns `"allBookLoans"` as a JSON array of book loans!
                // Oh! This is amazing! We can query the REST Dashboard API to get all book loans, OR we can also search student loans.
                // Since REST Dashboard API gives us allBookLoans, we can use that to render the admin active loans!
                // Let's check RestService to see if we fetch dashboard data. Yes, `getDashboardData(token)` returns the complete dashboard JSON!
                // Let's use RestService to fetch all active book loans for active loans list!
                // This is a beautiful composition of REST and SOAP.
                // Wait, let's verify if that's correct. Yes, REST dashboard returns `allBookLoans`.
                // Let's write the active loans fetcher:
                service.RestService restService = new service.RestService();
                String dashboardJson = restService.getDashboardData(session.getToken());
                String loansJson = getNestedJsonList(dashboardJson, "allBookLoans");
                List<java.util.Map<String, String>> allLoans = util.JsonParser.parseList(loansJson);
                
                SwingUtilities.invokeLater(() -> {
                    loansModel.setRowCount(0);
                    for (java.util.Map<String, String> loan : allLoans) {
                        String ref = loan.get("loanReference");
                        // Wait, make sure this is actually a book loan (some fields in REST dashboard could be room bookings).
                        // Let's check if the map has "bookIsbn" or "loanReference".
                        if (ref != null && !ref.isEmpty()) {
                            String status = loan.get("status");
                            if ("BORROWED".equals(status) || "OVERDUE".equals(status)) {
                                loansModel.addRow(new Object[]{
                                    ref,
                                    loan.get("studentId"),
                                    loan.get("bookIsbn"),
                                    loan.get("dueDate"),
                                    status,
                                    "Return"
                                });
                            }
                        }
                    }
                });
            } catch (Exception e) {
                // ignore or print
            }
        }).start();
    }

    private void refreshHistory(boolean isAdmin) {
        String input = historySearchField.getText().trim();

        new Thread(() -> {
            try {
                List<BookLoan> historyList = new java.util.ArrayList<>();
                if (input.isEmpty()) {
                    if (isAdmin) {
                        // Fetch all book loans from REST Dashboard
                        service.RestService restService = new service.RestService();
                        String dashboardJson = restService.getDashboardData(session.getToken());
                        String loansJson = getNestedJsonList(dashboardJson, "allBookLoans");
                        List<java.util.Map<String, String>> parsedLoans = util.JsonParser.parseList(loansJson);
                        
                        for (java.util.Map<String, String> m : parsedLoans) {
                            BookLoan bl = new BookLoan();
                            bl.setLoanReference(m.get("loanReference"));
                            bl.setStudentId(m.get("studentId"));
                            bl.setStudentName(m.get("studentName"));
                            bl.setBookIsbn(m.get("bookIsbn"));
                            bl.setBookTitle(m.get("bookTitle"));
                            bl.setLoanDate(m.get("loanDate"));
                            bl.setDueDate(m.get("dueDate"));
                            bl.setReturnDate(m.get("returnDate"));
                            bl.setStatus(m.get("status"));
                            try {
                                bl.setFineAmount(Double.parseDouble(m.get("fineAmount")));
                            } catch (Exception ex) {
                                bl.setFineAmount(0.0);
                            }
                            historyList.add(bl);
                        }
                    } else {
                        // Student defaults to their own history
                        historyList = soapService.getStudentLoanHistory(session.getToken(), session.getUserId());
                    }
                } else {
                    // Search by query
                    if (input.matches("\\d+")) {
                        historyList = soapService.getBookLoanHistory(session.getToken(), input);
                    } else {
                        historyList = soapService.getStudentLoanHistory(session.getToken(), input);
                    }
                }

                List<BookLoan> finalHistoryList = historyList;
                SwingUtilities.invokeLater(() -> {
                    historyModel.setRowCount(0);
                    for (BookLoan loan : finalHistoryList) {
                        historyModel.addRow(new Object[]{
                            loan.getLoanReference(),
                            loan.getStudentName() != null ? loan.getStudentName() : loan.getStudentId(),
                            loan.getBookTitle() != null ? loan.getBookTitle() : loan.getBookIsbn(),
                            loan.getLoanDate(),
                            loan.getDueDate(),
                            loan.getReturnDate() == null || loan.getReturnDate().isEmpty() ? "-" : loan.getReturnDate(),
                            loan.getStatus(),
                            String.format("%.2f", loan.getFineAmount())
                        });
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    historyModel.setRowCount(0);
                });
            }
        }).start();
    }

    // Custom Cell Renderer for Status badge
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (value != null) {
                String status = value.toString();
                if ("AVAILABLE".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0xFF00BFA5)); // Mint Green
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else if ("BORROWED".equalsIgnoreCase(status)) {
                    label.setForeground(new Color(0xFFFF6B6B)); // Crimson Red
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setForeground(Color.WHITE);
                }
            }
            return label;
        }
    }

    // Table Button Renderer and Editor for Return Action
    private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(0xFFE53935)); // Red Accent
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Return" : value.toString());
            return this;
        }
    }

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
            label = (value == null) ? "Return" : value.toString();
            button.setText(label);
            selectedRow = row;
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (clicked) {
                clicked = false;
                String ref = loansTable.getValueAt(selectedRow, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(button, "Are you sure you want to return book for loan reference:\n" + ref + "?", "Confirm Return", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    new Thread(() -> {
                        try {
                            soapService.returnBook(session.getToken(), ref);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(button, "Book returned successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                refreshCatalog("");
                                refreshActiveLoans();
                                refreshHistory(session.isAdmin());
                            });
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(button, "Failed to return book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    public void refreshAll() {
        refreshCatalog(searchField != null ? searchField.getText().trim() : "");
        if (session.isAdmin()) {
            refreshActiveLoans();
        }
        refreshHistory(session.isAdmin());
    }

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

    private void showIssueDialog(String isbn, String title, String author) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Issue Book to Student", true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0x1E1E1E));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dialog.setContentPane(panel);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(6, 6, 6, 6);

        // Selected Book Card/Banner
        JPanel bookCard = new JPanel(new GridLayout(0, 1, 3, 3));
        bookCard.setBackground(new Color(0x2D2D2D));
        bookCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel bTitle = new JLabel("Book: " + title);
        bTitle.setForeground(Color.WHITE);
        bTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel bIsbn = new JLabel("ISBN: " + isbn);
        bIsbn.setForeground(Color.LIGHT_GRAY);
        bIsbn.setFont(new Font("Monospaced", Font.PLAIN, 11));
        bookCard.add(bTitle);
        bookCard.add(bIsbn);
        panel.add(bookCard, c);
        c.gridy++;

        // Matric ID Input
        JLabel matricLabel = new JLabel("Student Matric ID *");
        matricLabel.setForeground(Color.LIGHT_GRAY);
        matricLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        JTextField matricField = new JTextField();
        matricField.setBackground(new Color(0x2A2A2A));
        matricField.setForeground(Color.WHITE);
        matricField.setCaretColor(Color.WHITE);
        matricField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        matricField.putClientProperty("JTextField.placeholderText", "e.g. B032310001");
        
        JPanel matricPanel = new JPanel(new BorderLayout(0, 4));
        matricPanel.setOpaque(false);
        matricPanel.add(matricLabel, BorderLayout.NORTH);
        matricPanel.add(matricField, BorderLayout.CENTER);
        panel.add(matricPanel, c);
        c.gridy++;

        // Due Date Input
        JLabel dueLabel = new JLabel("Return Due Date (YYYY-MM-DD) *");
        dueLabel.setForeground(Color.LIGHT_GRAY);
        dueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        JTextField dueField = new JTextField(java.time.LocalDate.now().plusDays(14).toString());
        dueField.setBackground(new Color(0x2A2A2A));
        dueField.setForeground(Color.WHITE);
        dueField.setCaretColor(Color.WHITE);
        dueField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        
        JPanel duePanel = new JPanel(new BorderLayout(0, 4));
        duePanel.setOpaque(false);
        duePanel.add(dueLabel, BorderLayout.NORTH);
        duePanel.add(dueField, BorderLayout.CENTER);
        panel.add(duePanel, c);
        c.gridy++;

        // Status / Message Label
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        panel.add(statusLabel, c);
        c.gridy++;

        // Issue Confirm Button
        JButton confirmBtn = new JButton("Confirm Issue");
        confirmBtn.setBackground(new Color(0xFF00BFA5));
        confirmBtn.setForeground(Color.BLACK);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confirmBtn.addActionListener(e -> {
            String matric = matricField.getText().trim();
            String due = dueField.getText().trim();

            if (matric.isEmpty() || due.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter Student Matric ID and Due Date.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            confirmBtn.setEnabled(false);
            statusLabel.setText("Verifying student and issuing book...");
            statusLabel.setForeground(Color.CYAN);

            new Thread(() -> {
                try {
                    // 1. Fetch student profile to verify and get name
                    service.RestService restService = new service.RestService();
                    String profileJson = restService.getStudentProfile(session.getToken(), matric);
                    String studentJson = getNestedJson(profileJson, "student");
                    java.util.Map<String, String> studentMap = util.JsonParser.parseObject(studentJson);
                    String studentName = studentMap.get("name");

                    if (studentName == null || studentName.isEmpty()) {
                        throw new Exception("Student name was empty in database profile.");
                    }

                    // 2. Call SOAP service to borrow book
                    String ref = soapService.borrowBook(session.getToken(), matric, studentName, isbn, due);

                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, "Book successfully issued!\nLoan Reference: " + ref, "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshCatalog("");
                        refreshActiveLoans();
                        refreshHistory(session.isAdmin());
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        confirmBtn.setEnabled(true);
                        statusLabel.setText("Failed: " + ex.getMessage());
                        statusLabel.setForeground(new Color(0xFFFF6B6B));
                        JOptionPane.showMessageDialog(dialog, "Transaction failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        panel.add(confirmBtn, c);

        dialog.setVisible(true);
    }

    private void showAddBookDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Book to Catalog", true);
        dialog.setSize(400, 360);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0x1E1E1E));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dialog.setContentPane(panel);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(8, 8, 8, 8);

        JLabel headerLabel = new JLabel("Add Book to SOAP Master Catalog");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(headerLabel, c);
        c.gridy++;

        JTextField isbnField = new JTextField();
        isbnField.setBackground(new Color(0x2A2A2A));
        isbnField.setForeground(Color.WHITE);
        isbnField.setCaretColor(Color.WHITE);
        isbnField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        isbnField.putClientProperty("JTextField.placeholderText", "ISBN Code (e.g. 9780134685991)");
        panel.add(isbnField, c);
        c.gridy++;

        JTextField titleField = new JTextField();
        titleField.setBackground(new Color(0x2A2A2A));
        titleField.setForeground(Color.WHITE);
        titleField.setCaretColor(Color.WHITE);
        titleField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        titleField.putClientProperty("JTextField.placeholderText", "Book Title");
        panel.add(titleField, c);
        c.gridy++;

        JTextField authorField = new JTextField();
        authorField.setBackground(new Color(0x2A2A2A));
        authorField.setForeground(Color.WHITE);
        authorField.setCaretColor(Color.WHITE);
        authorField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        authorField.putClientProperty("JTextField.placeholderText", "Author Name");
        panel.add(authorField, c);
        c.gridy++;

        JTextField categoryField = new JTextField();
        categoryField.setBackground(new Color(0x2A2A2A));
        categoryField.setForeground(Color.WHITE);
        categoryField.setCaretColor(Color.WHITE);
        categoryField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F51B5), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        categoryField.putClientProperty("JTextField.placeholderText", "Category / Genre");
        panel.add(categoryField, c);
        c.gridy++;

        JButton addBtn = new JButton("Add Book");
        addBtn.setBackground(new Color(0xFF00BFA5));
        addBtn.setForeground(Color.BLACK);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBtn.addActionListener(e -> {
            String isbn = isbnField.getText().trim();
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String cat = categoryField.getText().trim();

            if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || cat.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all book fields.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new Thread(() -> {
                try {
                    soapService.addBook(session.getToken(), isbn, title, author, cat);
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, "Book successfully added to SOAP master catalog!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshCatalog("");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Failed to add book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
        panel.add(addBtn, c);

        dialog.setVisible(true);
    }

    private class CatalogButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public CatalogButtonRenderer() {
            setOpaque(true);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String status = table.getValueAt(row, 5).toString();
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                setEnabled(true);
                setBackground(new Color(0xFF3F51B5)); // Indigo
                setText("Issue");
            } else {
                setEnabled(false);
                setBackground(new Color(0x333333)); // Dark Gray
                setText("Borrowed");
            }
            return this;
        }
    }

    private class CatalogButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean clicked;
        private int selectedRow;

        public CatalogButtonEditor(JTextField txt) {
            super(txt);
            button = new JButton();
            button.setOpaque(true);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String status = table.getValueAt(row, 5).toString();
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                button.setEnabled(true);
                button.setBackground(new Color(0xFF3F51B5));
                button.setText("Issue");
                label = "Issue";
            } else {
                button.setEnabled(false);
                button.setBackground(new Color(0x333333));
                button.setText("Borrowed");
                label = "Borrowed";
            }
            selectedRow = row;
            clicked = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (clicked && "Issue".equals(label)) {
                clicked = false;
                String isbn = catalogTable.getValueAt(selectedRow, 1).toString();
                String title = catalogTable.getValueAt(selectedRow, 2).toString();
                String author = catalogTable.getValueAt(selectedRow, 3).toString();
                
                SwingUtilities.invokeLater(() -> showIssueDialog(isbn, title, author));
            }
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
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
