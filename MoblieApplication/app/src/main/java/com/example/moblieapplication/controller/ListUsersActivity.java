package com.example.moblieapplication.controller;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.adapter.UserAdapter;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ListUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_users);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("List Users");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        new LoadUsersTask().execute();
    }

    private class LoadUsersTask extends AsyncTask<Void, Void, List<User>> {

        @Override
        protected List<User> doInBackground(Void... voids) {
            List<User> users = new ArrayList<>();
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT u.UserName, u.Email, u.Phone, " +
                            "(SELECT COUNT(*) FROM DeviceOwner d WHERE d.UserID = u.ID) AS NumberOfDevices " +
                            "FROM [User] u";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);

                    while (rs.next()) {
                        users.add(new User(
                                rs.getString("UserName"),
                                rs.getString("Email"),
                                rs.getString("Phone"),
                                "user",
                                rs.getInt("NumberOfDevices")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return users;
        }

        @Override
        protected void onPostExecute(List<User> users) {
            userList.clear();
            userList.addAll(users);
            adapter.notifyDataSetChanged();
        }
    }
}
