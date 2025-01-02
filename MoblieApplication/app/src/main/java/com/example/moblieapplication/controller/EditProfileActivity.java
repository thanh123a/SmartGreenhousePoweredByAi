package com.example.moblieapplication.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
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
import java.sql.SQLException;

public class EditProfileActivity extends AppCompatActivity {
    private EditText editTextEditEmail, editTextEditPhone;
    private Button buttonSaveProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("Edit your Profile");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        editTextEditEmail = findViewById(R.id.etEmail);
        editTextEditPhone = findViewById(R.id.etPhone);
        buttonSaveProfile = findViewById(R.id.btnSaveChanges);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUsername = sharedPreferences.getString("username", null);

        if (currentUsername != null) {
            new LoadProfileDataTask().execute(currentUsername);
        }

        buttonSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newEmail = editTextEditEmail.getText().toString().trim();
                String newPhone = editTextEditPhone.getText().toString().trim();

                if (validateInputs(newEmail)) {
                    new UpdateProfileTask().execute(currentUsername, newEmail, newPhone);
                }
            }
        });
    }

    private boolean validateInputs(String email) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email cannot be empty.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private class LoadProfileDataTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            String username = params[0];
            String[] userData = new String[2];

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT Email, Phone FROM [User] WHERE UserName = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, username);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        userData[0] = rs.getString("Email");
                        userData[1] = rs.getString("Phone");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return userData;
        }

        @Override
        protected void onPostExecute(String[] userData) {
            if (userData[0] != null) {
                editTextEditEmail.setText(userData[0]);
                editTextEditPhone.setText(userData[1]);
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateProfileTask extends AsyncTask<String, Void, Boolean> {
        private String newEmail, newPhone;

        @Override
        protected Boolean doInBackground(String... params) {
            String currentUsername = params[0];
            newEmail = params[1];
            newPhone = params[2];

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "UPDATE [User] SET Email = ?, Phone = ? WHERE UserName = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, newEmail);
                    stmt.setString(2, newPhone);
                    stmt.setString(3, currentUsername);

                    int rowsAffected = stmt.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();

                // Navigate back to the profile view
                Intent intent = new Intent(EditProfileActivity.this, ViewProfileActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile. Try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}