package ArduinoSerialGUI;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;
import java.awt.Polygon;

public class ArduinoSerialGUI {

    // F1 theme colors
    private static final Color F1_RED = new Color(220, 0, 0);
    private static final Color F1_BLACK = new Color(15, 15, 15);
    private static final Color F1_SILVER = new Color(210, 210, 210);
    private static final Color F1_DARK_GRAY = new Color(40, 40, 40);
    private static final Color F1_YELLOW = new Color(255, 215, 0);
    
    private JFrame frame;
    private JLabel timerLabel;
    private JLabel attemptLabel;
    private boolean gameOver = false;
    private int elapsedSeconds = 0;
    private Timer gameTimer;
    private SerialPort comPort;
    private int attempts = 0;
    private JPanel mainPanel;
    private JPanel gamePanel;
    private JPanel leaderboardPanel;
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;
    private final String LEADERBOARD_FILE = "leaderboard.csv";
    private Font f1Font;
    private Font f1BoldFont;
    private Font f1TitleFont;

    public ArduinoSerialGUI() {
        // Load custom F1 fonts
        loadF1Fonts();
        
        frame = new JFrame("Buzz Wire Challenge");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(createF1Icon().getImage());

        // Create card layout for switching between game and leaderboard
        mainPanel = new JPanel(new CardLayout());
        createGamePanel();
        createLeaderboardPanel();

        // Add panels to main panel
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(leaderboardPanel, "LEADERBOARD");

        // Show game panel by default
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "GAME");

        frame.add(mainPanel);
        frame.setVisible(true);

        startTimer();
        new Thread(this::listenToSerial).start();
    }
    
    private void loadF1Fonts() {
        // Using system fonts that resemble F1 style
        f1Font = new Font("Arial", Font.PLAIN, 16);
        f1BoldFont = new Font("Arial", Font.BOLD, 20);
        f1TitleFont = new Font("Arial", Font.BOLD, 32);
        
        // Try to load Formula 1 font if available on the system
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Font[] fonts = ge.getAllFonts();
            for (Font font : fonts) {
                if (font.getFontName().contains("Formula") || 
                    font.getFontName().contains("Racing") || 
                    font.getFontName().contains("F1")) {
                    f1Font = font.deriveFont(Font.PLAIN, 16);
                    f1BoldFont = font.deriveFont(Font.BOLD, 20);
                    f1TitleFont = font.deriveFont(Font.BOLD, 32);
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback to default fonts if custom font loading fails
            System.out.println("Using default fonts: " + e.getMessage());
        }
    }
    
    private ImageIcon createF1Icon() {
        // Create a simple F1 icon
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw red background
        g2d.setColor(F1_RED);
        g2d.fillOval(0, 0, size, size);
        
        // Draw F1 text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "F1";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        g2d.drawString(text, (size - textWidth) / 2, size / 2 + textHeight / 4);
        
        g2d.dispose();
        return new ImageIcon(image);
    }

    private void createGamePanel() {
        // Create a panel with background image
        gamePanel = new JBackgroundPanel();
        gamePanel.setLayout(new BorderLayout());
        
        // Create a semi-transparent overlay panel for content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(30, 50, 30, 50));
        
        // Create title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("BUZZ WIRE CHALLENGE");
        titleLabel.setFont(f1TitleFont);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel);
        
        // Create timer and attempts display with F1-style
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
        
        // Timer panel with F1 styling
        JPanel timerPanel = createF1Panel("RACE TIME");
        timerLabel = new JLabel("0:00", SwingConstants.CENTER);
        timerLabel.setFont(f1TitleFont);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerPanel.add(timerLabel);
        
        // Attempts panel with F1 styling
        JPanel attemptsPanel = createF1Panel("REMAINING LIVES");
        attemptLabel = new JLabel("9", SwingConstants.CENTER);
        attemptLabel.setFont(f1TitleFont);
        attemptLabel.setForeground(Color.WHITE);
        attemptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        attemptsPanel.add(attemptLabel);
        
        // Add timer and attempts panels to info panel
        infoPanel.add(timerPanel);
        infoPanel.add(Box.createVerticalStrut(20));
        infoPanel.add(attemptsPanel);
        
        // Instructions panel
        JPanel instructionPanel = new JPanel();
        instructionPanel.setOpaque(false);
        instructionPanel.setLayout(new BoxLayout(instructionPanel, BoxLayout.Y_AXIS));
        JLabel instructionLabel = new JLabel("Complete the circuit without touching the wire!");
        instructionLabel.setFont(f1Font);
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionPanel.add(instructionLabel);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton startButton = createF1Button("START");
        startButton.addActionListener(e -> {
            attempts = 9; // Reset attempts to 9
            updateAttemptLabel("Attempt: " + attempts); // Update the display
            startTimer(); // Start the timer countdown
            gameOver = false; // Reset game over state if needed
        });

        JButton viewLeaderboardButton = createF1Button("VIEW LEADERBOARD");
        viewLeaderboardButton.addActionListener(e -> {
        loadLeaderboard();
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "LEADERBOARD");
        });
        JButton resetButton = createF1Button("RESET LEADERBOARD");
        resetButton.setBackground(Color.DARK_GRAY); // Differentiate it from other red buttons
        resetButton.addActionListener(e -> {
        int result = JOptionPane.showConfirmDialog(frame,
        "Are you sure you want to clear the leaderboard?",
        "Confirm Reset",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
 
        if (result == JOptionPane.YES_OPTION) {
        try {
            File file = new File(LEADERBOARD_FILE);
            if (file.exists()) {
                new PrintWriter(file).close(); // Clear file contents
            }
            loadLeaderboard(); // Reload the now-empty leaderboard
            JOptionPane.showMessageDialog(frame, "Leaderboard reset successfully!",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error resetting leaderboard: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        }
        });

         // Stop button - Pauses the timer without ending the game
        JButton stopButton = createF1Button("STOP");
        stopButton.addActionListener(e -> {
        if (gameTimer != null && gameTimer.isRunning()) {
        gameTimer.stop();
        }
        });
 
        buttonPanel.add(viewLeaderboardButton);
        JButton finishButton = createF1Button("FINISH");
        finishButton.addActionListener(e -> {
        if (!gameOver) {
        gameOver = true;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        showNicknameDialog(); // Prompt for nickname and save score
        }
        });
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(finishButton);
        buttonPanel.add(viewLeaderboardButton);
        buttonPanel.add(resetButton);
        
        // Add all components to content panel
        contentPanel.add(titlePanel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(infoPanel);
        contentPanel.add(instructionPanel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(buttonPanel);
        
        // Add content panel to game panel
        gamePanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createF1Panel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(f1BoldFont);
        titleLabel.setForeground(F1_YELLOW);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        
        return panel;
    }
    
    private JButton createF1Button(String text) {
        JButton button = new JButton(text);
        button.setFont(f1BoldFont);
        button.setForeground(Color.WHITE);
        button.setBackground(F1_RED);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        return button;
    }

    private void createLeaderboardPanel() {
        // Create a panel with background image
        leaderboardPanel = new JBackgroundPanel();
        leaderboardPanel.setLayout(new BorderLayout());
        
        // Create a semi-transparent overlay panel for content
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(30, 50, 30, 50));
        
        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("CHAMPIONSHIP STANDINGS");
        titleLabel.setFont(f1TitleFont);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel);
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        // Create table model with columns
        leaderboardModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        leaderboardModel.addColumn("POS");
        leaderboardModel.addColumn("DRIVER");
        leaderboardModel.addColumn("TIME");
        leaderboardModel.addColumn("LIVES");
        leaderboardModel.addColumn("POINTS");

        // Create table with F1 styling
        leaderboardTable = new JTable(leaderboardModel);
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setRowHeight(35);
        leaderboardTable.setShowGrid(true); // Show grid lines for better visibility
        leaderboardTable.setGridColor(new Color(50, 50, 50)); // Subtle grid color
        leaderboardTable.setIntercellSpacing(new Dimension(1, 1)); // Add some spacing between cells
        leaderboardTable.setBackground(new Color(15, 15, 15, 200));
        leaderboardTable.setForeground(Color.WHITE);
        leaderboardTable.setFont(f1Font);
        leaderboardTable.setSelectionBackground(F1_RED);
        leaderboardTable.setSelectionForeground(Color.BLACK);
        leaderboardTable.setAutoCreateRowSorter(false);
        leaderboardTable.setRowSelectionAllowed(false);
        leaderboardTable.getTableHeader().setReorderingAllowed(false);
        
        // Style the table header
        JTableHeader header = leaderboardTable.getTableHeader();
        header.setBackground(F1_RED);
        header.setForeground(Color.BLACK); // Changed from WHITE to BLACK
        header.setFont(f1BoldFont);
        header.setBorder(null);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < leaderboardTable.getColumnCount(); i++) {
            leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Custom renderer for the first column (position)
        DefaultTableCellRenderer positionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row == 0) {
                    c.setBackground(isSelected ? F1_RED.darker() : F1_YELLOW);
                    c.setForeground(F1_BLACK);
                } else if (row == 1) {
                    c.setBackground(isSelected ? F1_RED.darker() : F1_SILVER);
                    c.setForeground(F1_BLACK);
                } else if (row == 2) {
                    c.setBackground(isSelected ? F1_RED.darker() : new Color(205, 127, 50)); // Bronze
                    c.setForeground(F1_BLACK);
                } else {
                    c.setBackground(isSelected ? F1_RED : new Color(15, 15, 15, 200));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };
        positionRenderer.setHorizontalAlignment(JLabel.CENTER);
        leaderboardTable.getColumnModel().getColumn(0).setCellRenderer(positionRenderer);
        
        // Set column widths and properties
        TableColumnModel columnModel = leaderboardTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60); // Position
        columnModel.getColumn(0).setMinWidth(60); // Increased from 50 to 60
        columnModel.getColumn(0).setMaxWidth(80); // Increased from 70 to 80
        columnModel.getColumn(1).setPreferredWidth(200); // Driver
        columnModel.getColumn(1).setMinWidth(100);
        
        columnModel.getColumn(2).setPreferredWidth(100); // Time
        columnModel.getColumn(2).setMinWidth(80);
        columnModel.getColumn(2).setMaxWidth(120);
        
        columnModel.getColumn(3).setPreferredWidth(80); // Lives
        columnModel.getColumn(3).setMinWidth(60);
        columnModel.getColumn(3).setMaxWidth(100);
        
        columnModel.getColumn(4).setPreferredWidth(100); // Points
        columnModel.getColumn(4).setMinWidth(80);
        
        // Ensure the table uses the full width of the scroll pane
        leaderboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        
        // Create scroll pane with F1 styling
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(15, 15, 15, 200));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton backButton = createF1Button("BACK TO RACE");
        backButton.addActionListener(e -> {
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "GAME");
        });
        buttonPanel.add(backButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add content panel to leaderboard panel
        leaderboardPanel.add(contentPanel, BorderLayout.CENTER);

        // Load leaderboard data
        loadLeaderboard();
    }

    private void startTimer() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }

        elapsedSeconds = 0;
        updateTimerLabel(0);
        gameOver = false;

        SwingUtilities.invokeLater(() -> {
            attemptLabel.setVisible(true);
            attemptLabel.setText("Attempt: 9");
        });

        gameTimer = new Timer(1000, e -> {
            if (gameOver) {
                ((Timer) e.getSource()).stop();
                return;
            }
            elapsedSeconds++;
            updateTimerLabel(elapsedSeconds);

            if (elapsedSeconds >= 180) {
                triggerGameOver();
            }
        });
        gameTimer.start();
    }

    private void updateTimerLabel(int seconds) {
        int minutes = seconds / 60;
        int sec = seconds % 60;
        SwingUtilities.invokeLater(() ->
                timerLabel.setText(String.format("Time: %d:%02d", minutes, sec))
        );
    }

    private void triggerGameOver() {
        if (gameOver) return;
        gameOver = true;

        if (gameTimer != null) {
            gameTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            attemptLabel.setText("GAME OVER");
        });

        // Show nickname input dialog
        showNicknameDialog();
    }

    private void updateAttemptLabel(String text) {
        SwingUtilities.invokeLater(() -> attemptLabel.setText(text));
    }

    private void showNicknameDialog() {
        // Create custom F1-styled dialog
        JDialog dialog = new JDialog(frame, "RACE COMPLETE", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(F1_DARK_GRAY);
        
        JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
        panel.setBackground(F1_DARK_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel promptLabel = new JLabel("Enter your driver name:");
        promptLabel.setFont(f1BoldFont);
        promptLabel.setForeground(Color.WHITE);
        
        JTextField nicknameField = new JTextField(10);
        nicknameField.setFont(f1Font);
        nicknameField.setBackground(F1_BLACK);
        nicknameField.setForeground(Color.WHITE);
        nicknameField.setCaretColor(Color.WHITE);
        nicknameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(F1_RED, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        panel.add(promptLabel);
        panel.add(nicknameField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(F1_DARK_GRAY);
        
        JButton okButton = createF1Button("CONFIRM");
        JButton cancelButton = createF1Button("CANCEL");
        
        okButton.addActionListener(e -> {
            if (!nicknameField.getText().trim().isEmpty()) {
                dialog.dispose();
                String nickname = nicknameField.getText().trim();
                int livesLeft = attempts;
                saveScore(nickname, elapsedSeconds, livesLeft);
                loadLeaderboard();
                
                // Switch to leaderboard view
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "LEADERBOARD");
            }
        });
        
        cancelButton.addActionListener(e -> {
            dialog.dispose();
            // If user cancels, just restart the game
            Timer restartTimer = new Timer(1000, ev -> startTimer());
            restartTimer.setRepeats(false);
            restartTimer.start();
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set default button and focus
        dialog.getRootPane().setDefaultButton(okButton);
        SwingUtilities.invokeLater(() -> nicknameField.requestFocusInWindow());
        
        dialog.setVisible(true);
    }

    private void saveScore(String nickname, int time, int livesLeft) {
        try {
            File file = new File(LEADERBOARD_FILE);
            boolean fileExists = file.exists();
            
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            
            // If file doesn't exist, write header
            if (!fileExists) {
                bw.write("Player,Time,LivesLeft\n");
            }
            
            // Write the score
            bw.write(String.format("%s,%d,%d\n", nickname, time, livesLeft));
            bw.close();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error saving score: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLeaderboard() {
        // Clear existing data
        while (leaderboardModel.getRowCount() > 0) {
            leaderboardModel.removeRow(0);
        }
        
        try {
            File file = new File(LEADERBOARD_FILE);
            if (!file.exists()) {
                return; // No leaderboard file yet
            }
            
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            
            // Skip header
            br.readLine();
            
            List<PlayerScore> scores = new ArrayList<>();
            
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String player = parts[0];
                    int time = Integer.parseInt(parts[1]);
                    int livesLeft = Integer.parseInt(parts[2]);
                    scores.add(new PlayerScore(player, time, livesLeft));
                }
            }
            br.close();
            
            // Sort scores (faster time and more lives left is better)
            Collections.sort(scores, new Comparator<PlayerScore>() {
                @Override
                public int compare(PlayerScore p1, PlayerScore p2) {
                    // First compare by lives left (more is better)
                    int livesComparison = Integer.compare(p2.livesLeft, p1.livesLeft);
                    if (livesComparison != 0) {
                        return livesComparison;
                    }
                    // Then compare by time (less is better)
                    return Integer.compare(p1.time, p2.time);
                }
            });
            
            // Add to table model
            int rank = 1;
            for (PlayerScore score : scores) {
                int minutes = score.time / 60;
                int seconds = score.time % 60;
                String timeStr = String.format("%d:%02d", minutes, seconds);
                
                // Calculate score - higher is better
                // Formula: (lives_left * 1000) - time_in_seconds
                int scoreValue = (score.livesLeft * 1000) - score.time;
                
                leaderboardModel.addRow(new Object[] {
                    rank++,
                    score.player,
                    timeStr,
                    score.livesLeft,
                    scoreValue
                });
            }
            
            // Force table to refresh and update its display
            leaderboardTable.revalidate();
            leaderboardTable.repaint();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading leaderboard: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenToSerial() {
        comPort = SerialPort.getCommPort("COM6");
        comPort.setBaudRate(9600);

        if (!comPort.openPort()) {
            updateAttemptLabel("Failed to open port");
            return;
        }

        try {
            byte[] buffer = new byte[1024];
            while (true) {
                while (comPort.bytesAvailable() == 0) {
                    Thread.sleep(20);
                }
                int numRead = comPort.readBytes(buffer, buffer.length);
                if (numRead > 0) {
                    String data = new String(buffer, 0, numRead).trim();
                    String[] lines = data.split("\\r?\\n");
                    for (String line : lines) {
                        if (line.equalsIgnoreCase("Restart")) {
                            SwingUtilities.invokeLater(() -> {
                                attempts = 9;
                                startTimer();
                                updateAttemptLabel("Attempt: " + attempts);
                            });
                        } else if (line.equalsIgnoreCase("Reset Timer")) {
                            SwingUtilities.invokeLater(this::startTimer);
                        } else if (!gameOver && line.matches("\\d+")) {
                            attempts = Integer.parseInt(line);
                            updateAttemptLabel("Attempt: " + attempts);
                            
                            // Check if game over (no more attempts)
                            if (attempts <= 0) {
                                triggerGameOver();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            updateAttemptLabel("Error: " + e.getMessage());
        } finally {
            comPort.closePort();
        }
    }

    // Class to store player scores
    private static class PlayerScore {
        String player;
        int time;
        int livesLeft;
        
        public PlayerScore(String player, int time, int livesLeft) {
            this.player = player;
            this.time = time;
            this.livesLeft = livesLeft;
        }
    }

    // Class to represent a racing particle for animation
    private class RacingParticle {
        int x, y;
        int speed;
        int size;
        Color color;
        
        public RacingParticle(int x, int y, int speed, int size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            
            // Randomly choose between F1 red or white with transparency
            if (new Random().nextBoolean()) {
                this.color = new Color(F1_RED.getRed(), F1_RED.getGreen(), F1_RED.getBlue(), 100 + new Random().nextInt(100));
            } else {
                this.color = new Color(255, 255, 255, 50 + new Random().nextInt(100));
            }
        }
        
        public void update() {
            // Move particle from left to right
            x += speed;
        }
        
        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            // Draw a line instead of a circle for a racing feel
            g2d.setStroke(new BasicStroke(size));
            g2d.drawLine(x - 10, y, x + 10, y);
        }
    }

    // Background panel class for F1 themed background
    private class JBackgroundPanel extends JPanel {
        private Image backgroundImage;
        private Timer animationTimer;
        private List<RacingParticle> particles = new ArrayList<>();
        private int animationTick = 0;
        
        public JBackgroundPanel() {
            setOpaque(false);
            // Try to load background image
            try {
                // First try to load from resources directory
                // Fix the file paths to use absolute paths
                File imageFile = new File("c:\\buzzWireGUI\\src\\resources\\f1_background.jpg");
                File svgFile = new File("c:\\buzzWireGUI\\src\\resources\\f1_background.svg");
                
                if (imageFile.exists()) {
                    backgroundImage = new ImageIcon(imageFile.getAbsolutePath()).getImage();
                } else if (svgFile.exists()) {
                    // Load SVG file
                    try {
                        backgroundImage = loadSVG(svgFile);
                        if (backgroundImage == null) {
                            createGradientBackground();
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading SVG: " + e.getMessage());
                        createGradientBackground();
                    }
                } else {
                    // Fallback to creating a gradient background
                    createGradientBackground();
                }
            } catch (Exception e) {
                System.out.println("Error loading background image: " + e.getMessage());
                createGradientBackground();
            }
            
            // Initialize animation particles
            initializeAnimations();
        }
        
        private void initializeAnimations() {
            // Create racing particles
            Random random = new Random();
            for (int i = 0; i < 30; i++) {
                particles.add(new RacingParticle(
                    random.nextInt(getWidth() > 0 ? getWidth() : 1200),
                    random.nextInt(getHeight() > 0 ? getHeight() : 800),
                    random.nextInt(5) + 1,
                    random.nextInt(3) + 1
                ));
            }
            
            // Start animation timer (30 FPS)
            animationTimer = new Timer(33, e -> {
                animationTick++;
                updateAnimations();
                repaint();
            });
            animationTimer.start();
        }
        
        private void updateAnimations() {
            // Update particle positions
            for (RacingParticle particle : particles) {
                particle.update();
                
                // If particle goes off-screen, reset it
                if (particle.x > getWidth()) {
                    particle.x = -10;
                    particle.y = new Random().nextInt(getHeight());
                }
            }
        }
        
        private Image loadSVG(File svgFile) {
            try {
                // Create a simple renderer for the SVG
                int width = 1280;
                int height = 720;
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                
                // Set rendering hints for better quality
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Create a fallback background in case SVG can't be rendered properly
                g2d.setColor(F1_BLACK);
                g2d.fillRect(0, 0, width, height);
                
                // Create a gradient background with F1 colors
                GradientPaint gp = new GradientPaint(
                    0, 0, F1_BLACK,
                    0, height, F1_RED.darker().darker());
                
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
                
                // Add checkered pattern at bottom
                g2d.setColor(new Color(40, 40, 40));
                int squareSize = 20;
                for (int x = 0; x < width; x += squareSize * 2) {
                    for (int y = height - 100; y < height; y += squareSize * 2) {
                        g2d.fillRect(x, y, squareSize, squareSize);
                        g2d.fillRect(x + squareSize, y + squareSize, squareSize, squareSize);
                    }
                }
                
                // Add F1 car silhouette
                g2d.setColor(F1_RED);
                int carX = width - 400;
                int carY = height - 200;
                g2d.fillRoundRect(carX, carY, 200, 60, 20, 20);
                g2d.setColor(Color.BLACK);
                g2d.fillOval(carX + 20, carY + 40, 40, 40);
                g2d.fillOval(carX + 140, carY + 40, 40, 40);
                
                // Add some racing lines
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(5));
                g2d.drawLine(0, height - 50, width, height - 80);
                g2d.drawLine(0, height - 30, width, height - 40);
                
                // Add F1 text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("F1 BUZZ WIRE CHALLENGE", width/2 - 300, 100);
                
                g2d.dispose();
                return new ImageIcon(img).getImage();
                
            } catch (Exception e) {
                System.out.println("Error in SVG loading: " + e.getMessage());
                return null;
            }
        }
        
        private void createGradientBackground() {
            // Create a gradient background as fallback
            int width = 1200;
            int height = 800;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();
            
            // Create gradient from dark to light
            GradientPaint gp = new GradientPaint(
                0, 0, F1_BLACK,
                0, height, F1_RED.darker().darker());
            
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
            
            // Add some racing-inspired elements
            g2d.setColor(new Color(40, 40, 40));
            // Draw checkered pattern at bottom
            int squareSize = 20;
            for (int x = 0; x < width; x += squareSize * 2) {
                for (int y = height - 100; y < height; y += squareSize * 2) {
                    g2d.fillRect(x, y, squareSize, squareSize);
                    g2d.fillRect(x + squareSize, y + squareSize, squareSize, squareSize);
                }
            }
            
            g2d.dispose();
            backgroundImage = new ImageIcon(img).getImage();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                // Draw the background image stretched to fill the panel
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                
                // Add a semi-transparent overlay for better text readability
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw animated racing stripes overlay
                drawRacingStripesOverlay(g2d);
                
                // Draw all particles
                for (RacingParticle particle : particles) {
                    particle.draw(g2d);
                }
                
                g2d.dispose();
            }
        }
        
        private void drawRacingStripesOverlay(Graphics2D g2d) {
            // Draw animated racing stripes
            int stripeWidth = 40;
            int stripeSpacing = 100;
            int stripeOffset = animationTick % 100; // Moving effect
            
            // Set stripe color with transparency
            g2d.setColor(new Color(F1_RED.getRed(), F1_RED.getGreen(), F1_RED.getBlue(), 30));
            
            // Draw diagonal racing stripes
            for (int x = -getHeight(); x < getWidth() + getHeight(); x += stripeSpacing) {
                Polygon stripe = new Polygon();
                stripe.addPoint(x - stripeOffset, 0);
                stripe.addPoint(x + stripeWidth - stripeOffset, 0);
                stripe.addPoint(x + stripeWidth + getHeight() - stripeOffset, getHeight());
                stripe.addPoint(x + getHeight() - stripeOffset, getHeight());
                g2d.fillPolygon(stripe);
            }
            
            // Draw checkered pattern at bottom with animation
            int squareSize = 20;
            int checkerOffset = (animationTick / 5) % (squareSize * 2);
            g2d.setColor(new Color(255, 255, 255, 40));
            
            for (int x = -checkerOffset; x < getWidth(); x += squareSize * 2) {
                for (int y = getHeight() - 100; y < getHeight(); y += squareSize * 2) {
                    g2d.fillRect(x, y, squareSize, squareSize);
                    g2d.fillRect(x + squareSize, y + squareSize, squareSize, squareSize);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();

        }
        
        SwingUtilities.invokeLater(ArduinoSerialGUI::new);
    }}