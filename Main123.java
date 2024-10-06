package jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Authentication {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "uddhav taur7777";

    public static boolean checkUsername(String username) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean authenticate(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
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
            String query = "SELECT id FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if user ID not found
    }
}

class Book1 {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "uddhav taur7777";
    private int bookChoice;
    private int fine;
    private int fineDays;
    private int extra_fine;
    private int bookCondition;
    private int issueDate, issueMonth, issueYear;
    private int returnDate, returnMonth, returnYear;

    public void displayBooks(JComboBox<String> bookComboBox) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT id, name FROM book9";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String item = id + " " + name;
                bookComboBox.addItem(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void borrowBook(int userId, int bookChoice) {
        this.bookChoice = bookChoice;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT quantity FROM book9 WHERE id = ?";
            PreparedStatement checkStatement = connection.prepareStatement(query);
            checkStatement.setInt(1, bookChoice);
            ResultSet resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                int quantity = resultSet.getInt("quantity");
                if (quantity > 0) {
                    String updateQuery = "UPDATE book9 SET quantity = quantity - 1 WHERE id = ?";
                    PreparedStatement statement = connection.prepareStatement(updateQuery);
                    statement.setInt(1, bookChoice);
                    int rowsAffected = statement.executeUpdate();
                    if (rowsAffected == 0) {
                        JOptionPane.showMessageDialog(null, "Book borrowing failed. Book not found.");
                    } else {
                        JOptionPane.showMessageDialog(null, "You have borrowed the book.");
                        storeBorrowInfo(connection, userId);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Book borrowing failed. Book not available.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Book borrowing failed. Book not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void returnBook(int userId, int bookChoice) {
        this.bookChoice = bookChoice;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE book9 SET quantity = quantity + 1 WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, bookChoice);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                JOptionPane.showMessageDialog(null, "Book returning failed. Book not found.");
            } else {
                JOptionPane.showMessageDialog(null, "Book is successfully returned.");
                storeReturnInfo(connection, userId, fine);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkBook() {
        String input = JOptionPane.showInputDialog("Book is in good condition if less than 1 & bad if greater than 1:");
        bookCondition = Integer.parseInt(input);
        if (bookCondition <= 1) {
            JOptionPane.showMessageDialog(null, "Good Condition");
        } else {
            input = JOptionPane.showInputDialog("Book cannot be returned due to bad condition book will charge the extra fine:\nEnter the extra fine:");
            extra_fine = Integer.parseInt(input);
            fine = extra_fine + fineDays * 5;
            JOptionPane.showMessageDialog(null, "Your total fine including original fine + extra fine = " + fine);
        }
    }

    public void dateOfIssue() {
        String input = JOptionPane.showInputDialog("Enter issue date (dd mm yyyy):");
        String[] dateParts = input.split(" ");
        issueDate = Integer.parseInt(dateParts[0]);
        issueMonth = Integer.parseInt(dateParts[1]);
        issueYear = Integer.parseInt(dateParts[2]);
        if (issueDate > 31 || issueMonth > 12) {
            JOptionPane.showMessageDialog(null, "Please enter correct date");
        }
    }

    public void dateOfReturn() {
        String input = JOptionPane.showInputDialog("Enter return date (dd mm yyyy):");
        String[] dateParts = input.split(" ");
        returnDate = Integer.parseInt(dateParts[0]);
        returnMonth = Integer.parseInt(dateParts[1]);
        returnYear = Integer.parseInt(dateParts[2]);
        if (returnDate > 31 || returnMonth > 12) {
            JOptionPane.showMessageDialog(null, "Please enter correct date");
        }
    }

    public void checkFine() {
        if (returnDate - issueDate <= 7) {
            JOptionPane.showMessageDialog(null, "No fine.");
            fineDays = 0;
        } else {
            fineDays = returnDate - issueDate - 7;
            fine = fineDays * 5;
            JOptionPane.showMessageDialog(null, "Your fine is " + fine + " RS");
        }
    }

    private void storeBorrowInfo(Connection connection, int userId) {
        try {
            String query = "INSERT INTO borrow_history1 (user_id, book_id, borrow_date) VALUES (?, ?, CURRENT_DATE)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, bookChoice);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void storeReturnInfo(Connection connection, int userId, int fine) {
        try {
            String query = "UPDATE borrow_history1 SET return_date = CURRENT_DATE, fine = ? WHERE user_id = ? AND book_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, fine);
            statement.setInt(2, userId);
            statement.setInt(3, bookChoice);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

public class Main123 {
    public static void main(String[] args) {
        // Create JFrame for login
        JFrame loginFrame = new JFrame("Library Management System - Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(400, 300);
        loginFrame.setLayout(new GridBagLayout());

        // Create components for login
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(15);
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");

        // Add components to the frame
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginFrame.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        loginFrame.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginFrame.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        loginFrame.add(passwordField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        loginFrame.add(loginButton, gbc);

        // Set frame visibility
        loginFrame.setVisible(true);

        // Add ActionListener to login button
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                // Authentication logic
                if (Authentication.checkUsername(username) && Authentication.authenticate(username, password)) {
                    JOptionPane.showMessageDialog(loginFrame, "Login successful!");

                    // Proceed to the book library
                    loginFrame.dispose();  // Close the login frame
                    openLibraryInterface(username);  // Open library interface
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Invalid username or password.", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void openLibraryInterface(String username) {
        int userId = Authentication.getUserId(username);

        // Create a new frame for the library interface
        JFrame libraryFrame = new JFrame("Library Interface");
        libraryFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        libraryFrame.setSize(500, 400);
        libraryFrame.setLayout(new FlowLayout());

        // Create components for the library interface
        JLabel bookLabel = new JLabel("Select a book:");
        JComboBox<String> bookComboBox = new JComboBox<>();
        JButton borrowButton = new JButton("Borrow");
        JButton returnButton = new JButton("Return");

        // Add components to the library frame
        libraryFrame.add(bookLabel);
        libraryFrame.add(bookComboBox);
        libraryFrame.add(borrowButton);
        libraryFrame.add(returnButton);

        // Populate books into the JComboBox
        Book1 library = new Book1();
        library.displayBooks(bookComboBox);

        // Set frame visibility
        libraryFrame.setVisible(true);

        // Add ActionListeners for borrow and return buttons
        borrowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = bookComboBox.getSelectedIndex();
                if (selectedIndex >= 0) {
                    // Book IDs are 1-based, so add 1 to selected index
                    library.borrowBook(userId, selectedIndex + 1);
                } else {
                    JOptionPane.showMessageDialog(libraryFrame, "Please select a book.");
                }
            }
        });

        returnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                library.dateOfIssue();
                library.dateOfReturn();
                library.checkFine();
                library.checkBook();
                int selectedIndex = bookComboBox.getSelectedIndex();
                if (selectedIndex >= 0) {
                    // Book IDs are 1-based, so add 1 to selected index
                    library.returnBook(userId, selectedIndex + 1);
                } else {
                    JOptionPane.showMessageDialog(libraryFrame, "Please select a book.");
                }
            }
        });
    }
}

