package com.example.moblieapplication.controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUserName;
    private EditText editTextPassword;
    private Button btLogin;
    private TextView tvForgetPassword;
    private Button btSignUp;
    private CheckBox checkBoxRememberMe;
    private String userRole;

    // SharedPreferences file name and keys
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";

    private static final String KEY_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUserName = findViewById(R.id.etUsername);
        editTextPassword = findViewById(R.id.etPassword);
        tvForgetPassword = findViewById(R.id.forgotPasswordText);
        btLogin = findViewById(R.id.loginButton);
        btSignUp = findViewById(R.id.SignupButton);

        //checkBoxRememberMe = findViewById(R.id.checkBoxRememberMe);
        tvForgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });

        btSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
        String role = getUserRole();
        if (loadUserPreferences()) {
            Toast.makeText(LoginActivity.this, "Logged in automatically!", Toast.LENGTH_LONG).show();
            if ("user".equals(role)) {
                startActivity(new Intent(LoginActivity.this, Dashboard.class));
            } else {
                startActivity(new Intent(LoginActivity.this, AdminScreenActivity.class));
            }
            finish();  // Close the LoginActivity
        }

        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LoginTask().execute(
                        editTextUserName.getText().toString(),
                        editTextPassword.getText().toString()
                );
            }
        });
    }

    private class LoginTask extends AsyncTask<String, Void, Boolean> {
        private String userRole;

        @Override
        protected Boolean doInBackground(String... params) {
            String userName = params[0];
            String password = params[1];

            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT Role FROM [User] WHERE UserName = ? AND Password = ?";
                    PreparedStatement statement = conn.prepareStatement(query);
                    statement.setString(1, userName);
                    statement.setString(2, password);

                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        userRole = resultSet.getString("Role");  // Retrieve the role from the query
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                rememberUser(editTextUserName.getText().toString(), editTextPassword.getText().toString(), userRole);
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_LONG).show();
                if ("user".equals(userRole)) {
                    startActivity(new Intent(LoginActivity.this, Dashboard.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, AdminScreenActivity.class));
                }
            } else {
                Toast.makeText(LoginActivity.this, "Login failed. Invalid credentials.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Save user credentials to SharedPreferences if "Remember Me" is checked
    // Save user credentials and role to SharedPreferences
    private void rememberUser(String userName, String password, String role) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, userName);
        editor.putString(KEY_PASSWORD, password);
        editor.putString(KEY_ROLE, role);  // Save the user role
        editor.putBoolean(KEY_REMEMBER, true);
        editor.apply();
    }

    // Load saved user credentials and role from SharedPreferences
    // Load saved user credentials and navigate directly to Dashboard if Remember Me was checked
    private boolean loadUserPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(KEY_REMEMBER, false)) {
            // Retrieve and use saved credentials if needed
            editTextUserName.setText(sharedPreferences.getString(KEY_USERNAME, ""));
            editTextPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
            //checkBoxRememberMe.setChecked(true);
            return true;
        }
        return false;
    }

    private String getUserRole() {
        String role = "";
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        role = sharedPreferences.getString(KEY_ROLE, "");
        return role;
    }


    // Clear saved user credentials from SharedPreferences
    // Clear saved user credentials and role from SharedPreferences
    private void clearUserPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}


