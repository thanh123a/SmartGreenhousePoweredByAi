package com.example.moblieapplication.controller;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.adapter.DeviceOwnerAdapter;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.DeviceOwner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ManageDeviceOwnerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DeviceOwnerAdapter adapter;
    private List<DeviceOwner> deviceOwners = new ArrayList<>();
    private int deviceCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_device_owner);

        deviceCode = getIntent().getIntExtra("DeviceCode", -1);

        recyclerView = findViewById(R.id.recyclerViewDeviceOwner);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceOwnerAdapter(deviceOwners, deviceCode, this);
        recyclerView.setAdapter(adapter);

        loadDeviceOwners();

        Button buttonAddUser = findViewById(R.id.buttonAddUser);
        buttonAddUser.setOnClickListener(view -> {
            Intent intent = new Intent(ManageDeviceOwnerActivity.this, AddUserControlDeviceActivity.class);
            intent.putExtra("DeviceCode", deviceCode);
            startActivityForResult(intent, 1);
        });
    }

    private void loadDeviceOwners() {
        new LoadDeviceOwnersTask().execute();
    }

    public void refreshList() {
        loadDeviceOwners(); // Refresh the list
    }

    private class LoadDeviceOwnersTask extends AsyncTask<Void, Void, List<DeviceOwner>> {

        @Override
        protected List<DeviceOwner> doInBackground(Void... voids) {
            List<DeviceOwner> owners = new ArrayList<>();
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT u.UserName, u.Email, u.Phone, d.Permission " +
                            "FROM DeviceOwner d " +
                            "INNER JOIN [User] u ON d.UserID = u.ID " +
                            "INNER JOIN Device dev ON d.DeviceID = dev.ID " +
                            "WHERE dev.DeviceCode = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setInt(1, deviceCode);

                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        DeviceOwner owner = new DeviceOwner(
                                rs.getString("UserName"),
                                rs.getString("Email"),
                                rs.getString("Phone"),
                                rs.getString("Permission")
                        );
                        owners.add(owner);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return owners;
        }

        @Override
        protected void onPostExecute(List<DeviceOwner> owners) {
            deviceOwners.clear();
            deviceOwners.addAll(owners);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            refreshList(); // Refresh the list after adding a user
        }
    }
}


