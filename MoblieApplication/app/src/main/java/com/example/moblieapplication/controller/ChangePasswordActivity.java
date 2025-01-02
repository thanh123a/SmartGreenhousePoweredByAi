package com.example.moblieapplication.controller;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextCurrentPassword, editTextNewPassword, editTextConfirmPassword;
    private Button buttonSave;
    private String generatedOTP;
    private String userEmail;

    private static final String FROM_EMAIL = "thanhnguyen2804t@gmail.com";
    private static final String FROM_PASSWORD = "bfrm ddwr bgvj sbkd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSave = findViewById(R.id.buttonSendOTP); // Reuse this button for "Save"

        buttonSave.setText("Save");
        buttonSave.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String currentPassword = editTextCurrentPassword.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            new CheckUserTask(username, currentPassword, newPassword).execute();
        });
    }

    private class CheckUserTask extends AsyncTask<Void, Void, String> {
        private String username;
        private String currentPassword;
        private String newPassword;

        public CheckUserTask(String username, String currentPassword, String newPassword) {
            this.username = username;
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT Email, Password FROM [User] WHERE UserName = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, username);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            String storedPassword = rs.getString("Password");
                            userEmail = rs.getString("Email");
                            if (!storedPassword.equals(currentPassword)) {
                                return "Incorrect current password.";
                            }
                            return "User valid.";
                        } else {
                            return "User does not exist.";
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
            return "Unknown error.";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("User valid.")) {
                generatedOTP = generateOTP();
                sendOTPToEmail(userEmail, generatedOTP);
                showOTPDialog();
            } else {
                Toast.makeText(ChangePasswordActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        }
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
                    new ChangePasswordTask(editTextUsername.getText().toString().trim(), editTextNewPassword.getText().toString().trim()).execute();
                } else {
                    Toast.makeText(this, "Invalid OTP.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private class ChangePasswordTask extends AsyncTask<Void, Void, String> {
        private String username;
        private String newPassword;

        public ChangePasswordTask(String username, String newPassword) {
            this.username = username;
            this.newPassword = newPassword;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String updatePasswordQuery = "UPDATE [User] SET Password = ? WHERE UserName = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(updatePasswordQuery)) {
                        stmt.setString(1, newPassword);
                        stmt.setString(2, username);
                        int rowsUpdated = stmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            return "Password changed successfully.";
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
            return "Failed to change password.";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(ChangePasswordActivity.this, result, Toast.LENGTH_SHORT).show();
            if (result.equals("Password changed successfully.")) {
                finish();
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
                                Toast.makeText(ChangePasswordActivity.this, "OTP sent! Check your email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MessagingException e) {
                        // Xử lý ngoại lệ nếu gửi email thất bại
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChangePasswordActivity.this, "Failed to send reset password email.", Toast.LENGTH_SHORT).show();
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

