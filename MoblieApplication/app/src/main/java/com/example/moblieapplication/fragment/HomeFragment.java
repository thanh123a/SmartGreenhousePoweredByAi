package com.example.moblieapplication.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.adapter.DeviceAdapter;
import com.example.moblieapplication.controller.ControlDeviceActivity;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.Device;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    String temperature, humi, soilMoisture, ph;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList = new ArrayList<>();
    private String userId;

    private CardView cardDeviceData;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", getContext().MODE_PRIVATE);
        userId = sharedPreferences.getString("username", null); // Assuming username is used to identify the user

        recyclerView = view.findViewById(R.id.recyclerViewDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Device device) {
                // Navigate to ControlDeviceActivity with the selected device's DeviceCode
                Intent intent = new Intent(getContext(), ControlDeviceActivity.class);
                Toast.makeText(getActivity(), "Device: " + device.getDeviceCode(), Toast.LENGTH_SHORT).show();
                intent.putExtra("deviceCode", device.getDeviceCode());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(deviceAdapter);


        // Load devices
        new LoadDevicesTask().execute();

        return view;
    }


    private class LoadDevicesTask extends AsyncTask<Void, Void, List<Device>> {
        @Override
        protected List<Device> doInBackground(Void... voids) {
            List<Device> devices = new ArrayList<>();
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", getContext().MODE_PRIVATE);
            String currentUsername = sharedPreferences.getString("username", null);

            if (currentUsername != null) {
                try (Connection conn = DatabaseHelper.getConnection()) {
                    if (conn != null) {
                        String query = "SELECT D.ID, D.DeviceCode, D.Description " +
                                "FROM Device D " +
                                "INNER JOIN DeviceOwner DO ON D.ID = DO.DeviceID " +
                                "INNER JOIN [User] U ON DO.UserID = U.ID " +
                                "WHERE U.UserName = ?";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setString(1, currentUsername);

                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            int id = rs.getInt("ID");
                            int deviceCode = rs.getInt("DeviceCode");
                            String description = rs.getString("Description");

                            devices.add(new Device(deviceCode, description, null, null, null, null));
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return devices;
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            if (devices.isEmpty()) {
                Toast.makeText(getContext(), "No devices found.", Toast.LENGTH_SHORT).show();
            } else {
                fetchDataFromFirebase(() -> {
                    for (Device device : devices) {
                        device.setTemperature(temperature);
                        device.setHumidity(humi);
                        device.setSoilMoisture(soilMoisture);
                        device.setPhLevel(ph);
                    }
                    deviceList.clear();
                    deviceList.addAll(devices);
                    deviceAdapter.notifyDataSetChanged();
                });
            }
        }
    }

    private void fetchDataFromFirebase(OnDataLoadedCallback callback) {
        DatabaseReference mData = FirebaseDatabase.getInstance().getReference();

        // Reset lại dữ liệu trước khi lắng nghe
        temperature = humi = soilMoisture = ph = null;

        mData.child("json/dataFromSensor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Đọc các giá trị từ Firebase
                if (snapshot.child("temperature").exists()) {
                    temperature = String.valueOf(snapshot.child("temperature").getValue(Float.class));
                } else {
                    temperature = "N/A";
                }

                if (snapshot.child("humidity").exists()) {
                    humi = String.valueOf(snapshot.child("humidity").getValue(Float.class));
                } else {
                    humi = "N/A";
                }

                if (snapshot.child("soilMoisture").exists()) {
                    soilMoisture = String.valueOf(snapshot.child("soilMoisture").getValue(Long.class));
                } else {
                    soilMoisture = "N/A";
                }

                if (snapshot.child("ph").exists()) {
                    ph = String.valueOf(snapshot.child("ph").getValue(Float.class));
                } else {
                    ph = "N/A";
                }

                // Dữ liệu đã tải xong, thực hiện callback
                callback.onDataLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load data from Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private int dataLoadedCount = 0;

    private void checkIfAllDataLoaded(OnDataLoadedCallback callback) {
        dataLoadedCount++;
        if (dataLoadedCount == 4) { // Khi tất cả 4 giá trị đã tải xong
            callback.onDataLoaded();
        }
    }

    interface OnDataLoadedCallback {
        void onDataLoaded();
    }

}