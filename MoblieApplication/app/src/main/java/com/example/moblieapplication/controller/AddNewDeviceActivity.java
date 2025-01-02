package com.example.moblieapplication.controller;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddNewDeviceActivity extends AppCompatActivity {

    private EditText editTextDeviceCode, editTextDeviceDescription, editTextUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_device);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("Create New Device");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        editTextDeviceCode = findViewById(R.id.editDeviceCode);
        editTextDeviceDescription = findViewById(R.id.editDeviceDescription);
        editTextUsername = findViewById(R.id.editUsername);
        Button buttonAddDevice = findViewById(R.id.buttonAddDevice);

        buttonAddDevice.setOnClickListener(v -> {
            String deviceCode = editTextDeviceCode.getText().toString().trim();
            String description = editTextDeviceDescription.getText().toString().trim();
            String username = editTextUsername.getText().toString().trim();

            if (deviceCode.isEmpty() || description.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!deviceCode.matches("\\d+")) {
                Toast.makeText(this, "Device Code must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AddDeviceTask(Integer.parseInt(deviceCode), description, username).execute();
        });
    }

    private class AddDeviceTask extends AsyncTask<Void, Void, String> {
        private int deviceCode;
        private String description, username;

        public AddDeviceTask(int deviceCode, String description, String username) {
            this.deviceCode = deviceCode;
            this.description = description;
            this.username = username;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Check if username exists
                    String checkUserQuery = "SELECT ID FROM [User] WHERE UserName = ?";
                    try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery)) {
                        checkUserStmt.setString(1, username);
                        ResultSet rs = checkUserStmt.executeQuery();
                        if (!rs.next()) {
                            return "Username does not exist.";
                        }
                        int userId = rs.getInt("ID");

                        // Insert into Device
                        String insertDeviceQuery = "INSERT INTO Device (DeviceCode, Description) VALUES (?, ?)";
                        try (PreparedStatement insertDeviceStmt = conn.prepareStatement(insertDeviceQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                            insertDeviceStmt.setInt(1, deviceCode);
                            insertDeviceStmt.setString(2, description);
                            insertDeviceStmt.executeUpdate();
                            ResultSet deviceRs = insertDeviceStmt.getGeneratedKeys();
                            if (deviceRs.next()) {
                                int deviceId = deviceRs.getInt(1);

                                // Insert into DeviceOwner
                                String insertOwnerQuery = "INSERT INTO DeviceOwner (DeviceID, UserID, Permission) VALUES (?, ?, 'user_root')";
                                try (PreparedStatement insertOwnerStmt = conn.prepareStatement(insertOwnerQuery)) {
                                    insertOwnerStmt.setInt(1, deviceId);
                                    insertOwnerStmt.setInt(2, userId);
                                    insertOwnerStmt.executeUpdate();
                                    return "Device added successfully.";
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
            return "Failed to add device.";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AddNewDeviceActivity.this, result, Toast.LENGTH_SHORT).show();
            if (result.equals("Device added successfully.")) {
                Intent intent = new Intent();
                intent.putExtra("deviceAdded", true);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}
