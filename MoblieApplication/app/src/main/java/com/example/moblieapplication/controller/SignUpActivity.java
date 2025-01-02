package com.example.moblieapplication.controller;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword, editTextConfirmPassword, editTextEmail;
    private Button buttonSignUp;
    private TextView loginTextView;
    private String generatedOTP;
    private String tempUsername, tempPassword, tempEmail;
    private static final String FROM_EMAIL = "thanhnguyen2804t@gmail.com";
    private static final String FROM_PASSWORD = "bfrm ddwr bgvj sbkd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        loginTextView = findViewById(R.id.loginTextView);

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        buttonSignUp.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString();
            String confirmPassword = editTextConfirmPassword.getText().toString();
            String email = editTextEmail.getText().toString().trim();

            if (validateInput(username, password, confirmPassword, email)) {
                tempUsername = username;
                tempPassword = password;
                tempEmail = email;

                // Generate OTP and send it to the user's email
                generatedOTP = generateOTP();
                sendOTPToEmail(email, generatedOTP);

                // Show OTP dialog
                showOTPDialog();
            }
        });
    }

    private boolean validateInput(String username, String password, String confirmPassword, String email) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showOTPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp_input, null);
        builder.setView(dialogView);

        EditText editTextOTPInput = dialogView.findViewById(R.id.editTextDialogOTP);

        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button buttonVerify = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonVerify.setOnClickListener(v -> {
                String enteredOTP = editTextOTPInput.getText().toString().trim();
                if (TextUtils.isEmpty(enteredOTP)) {
                    Toast.makeText(this, "OTP is required.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (enteredOTP.equals(generatedOTP)) {
                    dialog.dismiss();
                    // Proceed with user registration
                    new SignUpTask().execute(tempUsername, tempPassword, tempEmail);
                } else {
                    Toast.makeText(this, "Invalid OTP.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private class SignUpTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            String email = params[2];

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Check if username is unique
                    String checkUserQuery = "SELECT COUNT(*) FROM [User] WHERE UserName = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // Username already exists
                    }

                    // Insert new user with phone set to null
                    String insertQuery = "INSERT INTO [User] (UserName, Password, Phone, Email, Role) VALUES (?, ?, NULL, ?, 'user')";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, password);
                    insertStmt.setString(3, email);

                    int rowsInserted = insertStmt.executeUpdate();
                    return rowsInserted > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(SignUpActivity.this, "Sign-up successful!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(SignUpActivity.this, "Username already exists! Sign-up failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String generateOTP() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void sendOTPToEmail(String email, String otp) {
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
            message.setSubject("Change Password OTP");
            message.setText("Your OTP is: " + otp);

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
                                Toast.makeText(SignUpActivity.this, "OTP sent! Check your email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MessagingException e) {
                        // Xử lý ngoại lệ nếu gửi email thất bại
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignUpActivity.this, "Failed to send reset password email.", Toast.LENGTH_SHORT).show();
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
}
