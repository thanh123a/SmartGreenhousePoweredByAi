package com.example.moblieapplication.controller;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddUserControlDeviceActivity extends AppCompatActivity {

    private EditText editTextUserName;
    private Button buttonAddUser;
    private int deviceCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user_control_device);

        editTextUserName = findViewById(R.id.editTextUsername);
        buttonAddUser = findViewById(R.id.buttonSubmit);

        deviceCode = getIntent().getIntExtra("DeviceCode", -1);

        buttonAddUser.setOnClickListener(v -> {
            String userName = editTextUserName.getText().toString().trim();

            if (userName.isEmpty()) {
                Toast.makeText(this, "Please enter a username!", Toast.LENGTH_SHORT).show();
            } else {
                new CheckAndAddUserTask(userName).execute();
            }
        });
    }

    private class CheckAndAddUserTask extends AsyncTask<Void, Void, String> {
        private final String userName;

        CheckAndAddUserTask(String userName) {
            this.userName = userName;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Check if username exists in the User table
                    String userCheckQuery = "SELECT ID FROM [User] WHERE UserName = ?";
                    PreparedStatement userCheckStmt = conn.prepareStatement(userCheckQuery);
                    userCheckStmt.setString(1, userName);
                    ResultSet rsUser = userCheckStmt.executeQuery();

                    if (!rsUser.next()) {
                        return "Username not found!";
                    }

                    int userId = rsUser.getInt("ID");

                    // Check if username is already added to DeviceOwner for the device
                    String duplicateCheckQuery = "SELECT 1 FROM DeviceOwner " +
                            "INNER JOIN Device ON DeviceOwner.DeviceID = Device.ID " +
                            "WHERE DeviceCode = ? AND UserID = ?";
                    PreparedStatement duplicateCheckStmt = conn.prepareStatement(duplicateCheckQuery);
                    duplicateCheckStmt.setInt(1, deviceCode);
                    duplicateCheckStmt.setInt(2, userId);
                    ResultSet rsDuplicate = duplicateCheckStmt.executeQuery();

                    if (rsDuplicate.next()) {
                        return "Username already added to this device!";
                    }

                    // Add username to DeviceOwner
                    String addUserQuery = "INSERT INTO DeviceOwner (UserID, DeviceID, Permission) " +
                            "SELECT ?, ID, 'user_regular' FROM Device WHERE DeviceCode = ?";
                    PreparedStatement addUserStmt = conn.prepareStatement(addUserQuery);
                    addUserStmt.setInt(1, userId);
                    addUserStmt.setInt(2, deviceCode);
                    int rowsInserted = addUserStmt.executeUpdate();

                    if (rowsInserted > 0) {
                        return "User added successfully!";
                    } else {
                        return "Failed to add user!";
                    }
                } else {
                    return "Database connection failed!";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error occurred while adding user!";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(AddUserControlDeviceActivity.this, result, Toast.LENGTH_SHORT).show();
            if (result.equals("User added successfully!")) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}

