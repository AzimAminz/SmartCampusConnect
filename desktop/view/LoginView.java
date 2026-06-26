package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import service.RestService;
import util.JsonParser;
import util.SessionManager;


public class LoginView {

    private final RestService restService = new RestService();

    private JFrame frameLogin;
    private JPanel mainPanel;

    private JTextField idTextField;
    private JButton loginButton;

    private JLabel errorLabel;
    private JPanel loginErrorPanel;

    public LoginView() {

        // ---------------------------------------------------
        // Header for Logo and title
        // ---------------------------------------------------
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(0x121212));

        // Setup header icon
        JLabel headerIcon = new JLabel("🎓");
        headerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        headerIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerIcon.setForeground(new Color(0xFF3F51B5)); // Deep Indigo

        // Setup header main title "SmartCampus"
        JLabel headerTitle1 = new JLabel("SmartCampus");
        headerTitle1.setFont(new Font("Segoe UI", Font.BOLD, 28));
        headerTitle1.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerTitle1.setForeground(Color.WHITE);

        // Setup header 2nd title "Connect"
        JLabel headerTitle2 = new JLabel("Connect");
        headerTitle2.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        headerTitle2.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerTitle2.setForeground(new Color(0xFF00BFA5));

        // Add header elements into header panel
        headerPanel.add(headerIcon);
        headerPanel.add(headerTitle1);
        headerPanel.add(headerTitle2);

        // ---------------------------------------------------
        // Login Details (Card)
        // ---------------------------------------------------
        JPanel loginPanel = new JPanel();
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2D2D2D), 1, true),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        loginPanel.setBackground(new Color(0x1E1E1E));
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));

        // Main title in Login Panel "Secure Login
        JLabel loginTitle = new JLabel("Secure Login");
        loginTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        loginTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginTitle.setForeground(Color.WHITE);

        // Description in Login Panel
        JLabel loginDescription = new JLabel("<html><div style='text-align:center'; >Enter your Matric ID or ADMIN code to access your campus dashboard.</div></html>");
        loginDescription.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        loginDescription.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginDescription.setForeground(Color.GRAY);

        // Now setup id text field
        idTextField = new JTextField();
        idTextField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        idTextField.setBackground(new Color(0x2A2A2A));
        idTextField.setForeground(Color.WHITE);
        idTextField.setCaretColor(Color.WHITE);
        // Use .createCompoundBorder if you wanna setup more than 1 border
        // Like in this case, you wanna setup border style & margin (empty border = margin)
        idTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFF3F51B5), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        idTextField.putClientProperty("JTextField.placeholderText", "Matric No / Admin ID"); // Add placeholder

        // Panel for error text
        loginErrorPanel = new JPanel();
        loginErrorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x5A2A2A), 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        loginErrorPanel.setBackground(new Color(0x3A1C1C));

        errorLabel = new JLabel("test");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        errorLabel.setForeground(new Color(0xFF6B6B));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginErrorPanel.add(errorLabel);

        loginErrorPanel.setVisible(false);

        // Button login
        loginButton = new JButton("LOGIN");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(new Color(0xFF3F51B5));
        loginButton.setBorder(new EmptyBorder(10, 10, 10, 10));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add login elements inside loginPanel
        loginPanel.add(loginTitle);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(loginDescription);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(idTextField);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(loginErrorPanel);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(loginButton);

        // ---------------------------------------------------
        // Footer Details
        // ---------------------------------------------------
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBackground(new Color(0x121212));

        JLabel footerText1 = new JLabel("Demo Accounts:");
        footerText1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerText1.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerText1.setForeground(Color.GRAY);

        JLabel footerText2 = new JLabel("- Student: B032310001 to B032310005");
        footerText2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerText2.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerText2.setForeground(Color.GRAY);

        JLabel footerText3 = new JLabel("- Admin: ADMIN");
        footerText3.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerText3.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerText3.setForeground(Color.GRAY);

        // Add footer elements into footerPanel
        footerPanel.add(footerText1);
        footerPanel.add(footerText2);
        footerPanel.add(footerText3);

        footerPanel.add(Box.createVerticalStrut(15));
        try {
            ImageIcon icon = new ImageIcon("omgosh-final.png");
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(144, 50, Image.SCALE_SMOOTH);
            JLabel orgLogo = new JLabel(new ImageIcon(scaledImg));
            orgLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            footerPanel.add(orgLogo);
        } catch (Exception ex) {
            // silent catch
        }

        // ---------------------------------------------------
        // Add elements inside Main Panel
        // ---------------------------------------------------
        mainPanel = new JPanel();

        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(0, 20, 0, 20)); // Setup margin
        mainPanel.setSize(500, 1000);
        mainPanel.setBackground(new Color(0x121212));

        // GBC auto put things in the center
        // So you might see why all them in the center/ middle
        GridBagConstraints mainPanelGBC = new GridBagConstraints();
        mainPanelGBC.gridx = 0;
        mainPanelGBC.gridy = 0;
        mainPanelGBC.fill = GridBagConstraints.HORIZONTAL;
        mainPanelGBC.weightx = 1.0;

        mainPanel.add(headerPanel, mainPanelGBC);

        // Put the element next line
        mainPanelGBC.gridy++;
        mainPanelGBC.insets = new Insets(20, 0, 0, 0); // Setup margin
        mainPanel.add(loginPanel, mainPanelGBC);

        mainPanelGBC.gridy++;
        mainPanelGBC.insets = new Insets(20, 0, 0, 0); // Setup margin
        mainPanel.add(footerPanel, mainPanelGBC);


        // ---------------------------------------------------
        // Setup Frame
        // ---------------------------------------------------
        frameLogin = new JFrame("Desktop Application - Login View");

        // Get the screen size using Toolkit
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frameLogin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Special abit for frame
        // Need to add .getContentPane()
        frameLogin.add(mainPanel);
        frameLogin.pack();
        frameLogin.setSize(500, 680);
        frameLogin.setLocationRelativeTo(null); // Located at the middle of the screen

        // ---------------------------------------------------
        // Setup Action Listener
        // ---------------------------------------------------
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

        idTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });

    }

    private void performLogin() {
        String userId = idTextField.getText().trim();
        if (userId.isEmpty()) {
            showError("Please enter your Matric No or Admin ID");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("LOGGING IN...");
        loginErrorPanel.setVisible(false);

        // Run network request on background thread
        new Thread(() -> {
            try {
                String responseJson = restService.login(userId);
                Map<String, String> sessionData = JsonParser.parseObject(responseJson);

                String token = sessionData.get("token");
                String role = sessionData.get("role");
                String fullName = sessionData.get("fullName");

                if (token == null || token.isEmpty()) {
                    throw new Exception("Invalid response from server: missing token");
                }

                // Start Session
                SessionManager.getInstance().startSession(token, userId, role, fullName);

                // Switch to Dashboard (on EDT)
                SwingUtilities.invokeLater(() -> {
                    DashboardView dashboard = new DashboardView();
                    dashboard.setVisible(true);
                    // unlike EXIT_ON_CLOSE, dispose() doesnt close whole JVM
                    // Only the window is close but the resources allocated still there
                    frameLogin.dispose(); // Close login window
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("LOGIN");
                    showError(ex.getMessage());
                });
            }
        }).start();
    }

    private void showError(String message) {
        if (message.contains("Authentication failed")) {
            errorLabel.setText("<html>Authentication failed. Invalid Matric No or Admin ID.</html>");
        } else {
            errorLabel.setText("<html>" + message + "</html>");
        }
        loginErrorPanel.setVisible(true);
        frameLogin.revalidate();
        frameLogin.repaint();
    }

    public void setVisible(boolean visible) {
        if (visible) {
            frameLogin.setVisible(true);
        }
    }

}
