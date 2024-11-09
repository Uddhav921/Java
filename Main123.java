package jdbc;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Class for user authentication
class Authentication {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/library_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "uddhav taur7777";

    public static boolean registerUser(String username, String password, String email) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkQuery = "SELECT * FROM user15 WHERE username = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, username);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                return false; // Username already exists
            }

            String insertQuery = "INSERT INTO user15 (username, password, email) VALUES (?, ?, ?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
            insertStatement.setString(1, username);
            insertStatement.setString(2, password);
            insertStatement.setString(3, email);
            insertStatement.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean authenticate(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM user15 WHERE username = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getUserId(String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT id FROM user15 WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getUserBorrowedCount(int userId) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT COUNT(*) AS borrowed_count FROM borrowings WHERE user_id = ? AND return_date IS NULL AND borrow_date >= CURRENT_DATE - INTERVAL '15 days'";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("borrowed_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

// Class for book operations
class Book {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/library_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "uddhav taur7777";

    public void displayBooks(JComboBox<String> bookComboBox) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT id, title FROM books15 WHERE quantity > 0";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String item = id + " - " + title;
                bookComboBox.addItem(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String borrowBook(int userId, int bookId, String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            int borrowedCount = Authentication.getUserBorrowedCount(userId);
            if (borrowedCount >= 3) {
                return "You have already borrowed 3 books within the last 15 days. Please return a book before borrowing another.";
            }

            String checkQuery = "SELECT quantity FROM books15 WHERE id = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setInt(1, bookId);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next() && resultSet.getInt("quantity") > 0) {
                // Update book quantity
                String updateQuery = "UPDATE books15 SET quantity = quantity - 1 WHERE id = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                updateStatement.setInt(1, bookId);
                updateStatement.executeUpdate();

                // Record the borrowing
                String borrowQuery = "INSERT INTO borrowings (user_id, book_id, borrow_date) VALUES (?, ?, CURRENT_DATE)";
                PreparedStatement borrowStatement = connection.prepareStatement(borrowQuery);
                borrowStatement.setInt(1, userId);
                borrowStatement.setInt(2, bookId);
                borrowStatement.executeUpdate();

                return username + " Borrowed The book Successfully on " + LocalDate.now();
            } else {
                return "Book is not available for borrowing.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "An error occurred while borrowing the book.";
        }
    }

    public String returnBook(int userId, int bookId, String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkQuery = "SELECT borrow_date FROM borrowings WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setInt(1, userId);
            checkStatement.setInt(2, bookId);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                LocalDate borrowDate = resultSet.getDate("borrow_date").toLocalDate();
                LocalDate returnDate = LocalDate.now();
                long daysOverdue = ChronoUnit.DAYS.between(borrowDate.plusDays(7), returnDate);
                int fine = daysOverdue > 0 ? (int) (daysOverdue * 5) : 0;

                // Update book quantity
                String updateQuery = "UPDATE books15 SET quantity = quantity + 1 WHERE id = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                updateStatement.setInt(1, bookId);
                updateStatement.executeUpdate();

                // Record the return
                String returnQuery = "UPDATE borrowings SET return_date = CURRENT_DATE, fine = ? WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
                PreparedStatement returnStatement = connection.prepareStatement(returnQuery);
                returnStatement.setInt(1, fine);
                returnStatement.setInt(2, userId);
                returnStatement.setInt(3, bookId);
                returnStatement.executeUpdate();

                return username + " Returned The Book Successfully on " + returnDate + ". Fine: " + fine +" Ruppes";
            } else {
                return "No record found of you borrowing this book.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "An error occurred while returning the book.";
        }
    }

    public void displayUserHistory(int userId, JTextArea historyArea) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT b.title, br.borrow_date, br.return_date, br.fine FROM borrowings br JOIN books15 b ON br.book_id = b.id WHERE br.user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            StringBuilder historyBuilder = new StringBuilder();
            while (resultSet.next()) {
                String title = resultSet.getString("title");
                LocalDate borrowDate = resultSet.getDate("borrow_date").toLocalDate();
                String returnDate = resultSet.getDate("return_date") != null ? resultSet.getDate("return_date").toString() : "Not Returned";
                int fine = resultSet.getInt("fine");

                historyBuilder.append("Title: ").append(title)
                        .append(", Borrowed: ").append(borrowDate)
                        .append(", Returned: ").append(returnDate)
                        .append(", Fine: Ruppes=").append(fine).append("\n");
            }
            historyArea.setText(historyBuilder.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// Class for the User Interface
class LibraryManagementSystemUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JComboBox<String> bookComboBox;
    private JTextArea historyArea;
    private JButton loginButton;
    private JButton registerButton;
    private JButton borrowButton;
    private JButton returnButton;
    private JButton logoutButton;

    private String loggedInUser;
    private int loggedInUserId;

    public LibraryManagementSystemUI() {
        setTitle("Library Book Issue Management System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Center Panel for login/registration
        JPanel centerPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);
        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(20);

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        centerPanel.add(usernameLabel);
        centerPanel.add(usernameField);
        centerPanel.add(passwordLabel);
        centerPanel.add(passwordField);
        centerPanel.add(emailLabel);
        centerPanel.add(emailField);
        centerPanel.add(loginButton);
        centerPanel.add(registerButton);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Right panel for books and operations
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel bookLabel = new JLabel("Available Books:");
        bookComboBox = new JComboBox<>();
        bookComboBox.setPreferredSize(new Dimension(250, 30));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        borrowButton = new JButton("Borrow Book");
        returnButton = new JButton("Return Book");
        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);

        rightPanel.add(bookLabel, BorderLayout.NORTH);
        rightPanel.add(bookComboBox, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(rightPanel, BorderLayout.EAST);

        // History area
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        JScrollPane historyScrollPane = new JScrollPane(historyArea);
        mainPanel.add(historyScrollPane, BorderLayout.SOUTH);

        // Adding main panel to frame
        add(mainPanel, BorderLayout.CENTER);

        // Logout button
        logoutButton = new JButton("Logout");
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Action Listeners
        loginButton.addActionListener(new LoginActionListener());
        registerButton.addActionListener(new RegisterActionListener());
        borrowButton.addActionListener(new BorrowActionListener());
        returnButton.addActionListener(new ReturnActionListener());
        logoutButton.addActionListener(new LogoutActionListener());

        setLocationRelativeTo(null);
    }

    private class LoginActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (Authentication.authenticate(username, password)) {
                loggedInUser = username;
                loggedInUserId = Authentication.getUserId(username);
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Login Successful!");

                // Load available books
                new Book().displayBooks(bookComboBox);

                // Load user history
                new Book().displayUserHistory(loggedInUserId, historyArea);

            } else {
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Invalid username or password!");
            }
        }
    }

    private class RegisterActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String email = emailField.getText();

            if (Authentication.registerUser(username, password, email)) {
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Registration Successful!");
            } else {
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Username already exists.");
            }
        }
    }

    private class BorrowActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (loggedInUserId > 0 && bookComboBox.getSelectedItem() != null) {
                String selectedItem = (String) bookComboBox.getSelectedItem();
                int bookId = Integer.parseInt(selectedItem.split(" - ")[0]);
                String result = new Book().borrowBook(loggedInUserId, bookId, loggedInUser);

                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "<html><h2 style='font-size:16px'>" + result + "</h2></html>");

                // Update user history
                new Book().displayUserHistory(loggedInUserId, historyArea);
            } else {
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Please login and select a book.");
            }
        }
    }

    private class ReturnActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (loggedInUserId > 0 && bookComboBox.getSelectedItem() != null) {
                String selectedItem = (String) bookComboBox.getSelectedItem();
                int bookId = Integer.parseInt(selectedItem.split(" - ")[0]);
                String result = new Book().returnBook(loggedInUserId, bookId, loggedInUser);

                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "<html><h2 style='font-size:16px'>" + result + "</h2></html>");

                // Update user history
                new Book().displayUserHistory(loggedInUserId, historyArea);
            } else {
                JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Please login and select a book.");
            }
        }
    }

    private class LogoutActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(LibraryManagementSystemUI.this, "Thanks for Visiting!");
            dispose();
        }
    }
}

// Main class to run the application
 class LibraryManagementSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LibraryManagementSystemUI libraryUI = new LibraryManagementSystemUI();
            libraryUI.setVisible(true);
        });
    }
}
