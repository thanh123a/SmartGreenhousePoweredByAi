package com.example.moblieapplication.controller;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button resetPasswordButton;
    private TextView back;

    private String generatedPassword;
    private String tempEmail;
    private static final String FROM_EMAIL = "thanhnguyen2804t@gmail.com";
    private static final String FROM_PASSWORD = "bfrm ddwr bgvj sbkd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        emailInput = findViewById(R.id.emailInput);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        back = findViewById(R.id.backToLoginText);

        back.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        resetPasswordButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (validateEmail(email)) {
                tempEmail = email;

                // Generate OTP and send to the user's email
                generatedPassword = generateNewPassword();
                sendPasswordToEmail(email, generatedPassword);

                new ResetPasswordTask().execute(tempEmail, generatedPassword);
            }
        });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String generateNewPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Pass length
        int length = 8;

        StringBuilder password = new StringBuilder();

        // Use random to generate password
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }

        return password.toString();
    }

    private void sendPasswordToEmail(String email, String pass) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Create Authenticator to authentic email
        Authenticator auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        };

        // Tạo phiên gửi email
        Session session = Session.getInstance(props, auth);

        try {
            // Tạo một đối tượng MimeMessage
            final Message message = new MimeMessage(session);

            // Set up email
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Reset Password Email");
            message.setText("Your new password is: " + pass);

            // Send email in new thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Send email
                        Transport.send(message);

                        // Notice for user
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ResetPasswordActivity.this, "New Password sent! Check your email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MessagingException e) {
                        // Xử lý ngoại lệ nếu gửi email thất bại
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ResetPasswordActivity.this, "Failed to send reset password email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private class ResetPasswordTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String email = params[0];
            String newPassword = params[1];

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Check if email exists
                    String checkEmailQuery = "SELECT COUNT(*) FROM [User] WHERE Email = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkEmailQuery);
                    checkStmt.setString(1, email);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        return false; // Email does not exist
                    }

                    // Update password
                    String updatePasswordQuery = "UPDATE [User] SET Password = ? WHERE Email = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updatePasswordQuery);
                    updateStmt.setString(1, newPassword);
                    updateStmt.setString(2, email);

                    int rowsUpdated = updateStmt.executeUpdate();
                    return rowsUpdated > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ResetPasswordActivity.this, "Password reset successful! Check Your Email.", Toast.LENGTH_LONG).show();
                finish(); // Close activity
            } else {
                Toast.makeText(ResetPasswordActivity.this, "Password reset failed. Email not found.", Toast.LENGTH_LONG).show();
            }
        }
    }
}



