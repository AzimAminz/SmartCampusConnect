
// import javax.swing.SwingUtilities;
import javax.swing.*;

import view.LoginView;

public class Main {
    public static void main(String[] args) {
        // Initialize FlatLaf allow Dark Theme (Default is Light Theme)
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatDarkLaf theme, using default Look and Feel.");
        }

        // Why use SwingUtilities.invokeLater()?
        // Normally when we run java swing, we should add it
        /*
         * If we didnt add it, then we run swing UI in main thread instead of swing
         * thread
         */
        /*
         * If we run in swing thread, then all the swing element can be run safety and
         * properly
         */
        // Cuz if run in main thread, sometimes button might not functioning
        // So using SwingUtilities is safe
        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });
    }
}
