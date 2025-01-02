package com.example.moblieapplication.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ViewProfileActivity extends AppCompatActivity {
    private TextView textViewUsername, textViewEmail, textViewPhone, textViewDeviceCount;
    private Button buttonEditProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("User Information");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        textViewUsername = findViewById(R.id.usernameText);
        textViewEmail = findViewById(R.id.emailText);
        textViewPhone = findViewById(R.id.phoneText);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);
        buttonEditProfile = findViewById(R.id.editProfileButton);
        buttonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        if (username != null) {
            new LoadProfileTask().execute(username);
        }

    }

    private class LoadProfileTask extends AsyncTask<String, Void, User> {

        @Override
        protected User doInBackground(String... params) {
            String username = params[0];
            User userProfile = new User();

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Query user profile details
                    String query = "SELECT " +
                            "u.UserName, u.Email, u.Phone, " +
                            "COALESCE(COUNT(d.UserID), 0) AS DeviceCount " +
                            "FROM [User] u " +
                            "LEFT JOIN DeviceOwner d ON u.ID = d.UserID " +
                            "WHERE u.UserName = ? " +
                            "GROUP BY u.UserName, u.Email, u.Phone";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        userProfile.setUsername(rs.getString("UserName"));
                        userProfile.setEmail(rs.getString("Email"));
                        userProfile.setPhone(rs.getString("Phone"));
                        userProfile.setDeviceCount(rs.getInt("DeviceCount"));
                        userProfile.setRole("user");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return userProfile;
        }

        @Override
        protected void onPostExecute(User userProfile) {
            if (userProfile != null) {
                // Update UI with profile details
                textViewUsername.setText("Username: " + userProfile.getUsername());
                textViewEmail.setText("Email: " + userProfile.getEmail());
                textViewPhone.setText("Phone: " + (userProfile.getPhone() != null ? userProfile.getPhone() : "N/A"));
                textViewDeviceCount.setText("Devices Controlled: " + userProfile.getDeviceCount());
            } else {
                Toast.makeText(ViewProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}