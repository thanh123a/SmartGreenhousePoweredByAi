package com.example.moblieapplication.controller;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.adapter.DeviceListAdapter;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.DeviceList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ListDevicesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDevices;
    private DeviceListAdapter adapter;
    private List<DeviceList> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_devices);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("List Devices");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        Button buttonAddNewDevice = findViewById(R.id.button_add_new_device);

        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceList = new ArrayList<>();
        adapter = new DeviceListAdapter(deviceList);
        recyclerViewDevices.setAdapter(adapter);

        // Load devices
        new LoadDevicesTask().execute();

        // Navigate to AddNewDeviceActivity
        buttonAddNewDevice.setOnClickListener(v -> {
            Intent intent = new Intent(ListDevicesActivity.this, AddNewDeviceActivity.class);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            boolean deviceAdded = data.getBooleanExtra("deviceAdded", false);
            if (deviceAdded) {
                Toast.makeText(this, "Device added successfully!", Toast.LENGTH_SHORT).show();
                new LoadDevicesTask().execute();
            }
        }
    }

    private class LoadDevicesTask extends AsyncTask<Void, Void, List<DeviceList>> {
        @Override
        protected List<DeviceList> doInBackground(Void... voids) {
            List<DeviceList> devices = new ArrayList<>();
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT d.DeviceCode, d.Description, u.UserName AS Owner " +
                            "FROM Device d " +
                            "JOIN DeviceOwner do ON d.ID = do.DeviceID " +
                            "JOIN [User] u ON do.UserID = u.ID " +
                            "WHERE do.Permission = 'user_root'";
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        while (rs.next()) {
                            devices.add(new DeviceList(
                                    rs.getInt("DeviceCode"),
                                    rs.getString("Description"),
                                    rs.getString("Owner")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return devices;
        }

        @Override
        protected void onPostExecute(List<DeviceList> devices) {
            deviceList.clear();
            deviceList.addAll(devices);
            adapter.notifyDataSetChanged();
        }
    }
}
