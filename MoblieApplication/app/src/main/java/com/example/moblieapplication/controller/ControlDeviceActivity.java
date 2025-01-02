package com.example.moblieapplication.controller;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moblieapplication.R;
import com.example.moblieapplication.database.DatabaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class ControlDeviceActivity extends AppCompatActivity {
    private TextView tv_temperatureValue, tv_humidityValue, tv_soilMoistureValue, tv_phValue,
            tv_ai_soilMoisture, tv_ai_Light_Duration;
    private int deviceCode;
    private Switch autoModeSwitch, pump1Switch, pump2Switch, fanSwitch, ledSwitch;
    private EditText setSoilMoisture, timeStartFan, timeFinishFan,
            timeStartLighting, timeFinishLighting,
            timeStartWatering,
            timeFinishWatering, timeStartWateringNutri,
            timeFinishWateringNutri;

    String timeStartFan_user = "00:00", timeFinishFan_user = "00:00",
            timeStartLighting_user = "00:00", timeFinishLighting_user = "00:00",
            timeStartWatering_user = "00:00", timeFinishWatering_user = "00:00",
            timeStartWateringNutri_user = "00:00", timeFinishWateringNutri_user = "00:00";
    private String username;
    Button button_manage, button_apply_set;
    DatabaseReference mdata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_device);

        //tạo action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Đặt tiêu đề cho ActionBar
            actionBar.setTitle("Device Information");//Title
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        button_apply_set = findViewById(R.id.apply_setting);
        button_manage = findViewById(R.id.manage_device_bt);

        Intent intent = getIntent();
        if (intent != null) {
            deviceCode = intent.getIntExtra("deviceCode", -1);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");


        new LoadPermissionTask().execute();


        tv_temperatureValue = findViewById(R.id.temperatureValue);
        tv_humidityValue = findViewById(R.id.humidityValue);
        tv_soilMoistureValue = findViewById(R.id.soilMoistureValue);
        tv_phValue = findViewById(R.id.phValue);
        tv_ai_soilMoisture = findViewById(R.id.ai_soilMoisture);
        tv_ai_Light_Duration = findViewById(R.id.ai_Light_Duration);

        autoModeSwitch = findViewById(R.id.autoModeSwitch);
        pump1Switch = findViewById(R.id.pump1Switch);
        pump2Switch = findViewById(R.id.pump2Switch);
        fanSwitch = findViewById(R.id.fanSwitch);
        ledSwitch = findViewById(R.id.ledSwitch);


        mdata = FirebaseDatabase.getInstance().getReference();
        autoModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/controller/isAuto").setValue(1);
                    //pump1Switch.setEnabled(false);
                    pump2Switch.setEnabled(false);
                    //fanSwitch.setEnabled(false);
                    ledSwitch.setEnabled(false);
                } else {
                    mdata.child("json/controller/isAuto").setValue(0);
                    //pump1Switch.setEnabled(true);
                    pump2Switch.setEnabled(true);
                    //fanSwitch.setEnabled(true);
                    ledSwitch.setEnabled(true);
                }
            }
        });

        pump1Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/controller/turnOnPump1").setValue(1);
                } else {
                    mdata.child("json/controller/turnOnPump1").setValue(0);
                }
            }
        });

        pump2Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/controller/turnOnPump2").setValue(1);
                } else {
                    mdata.child("json/controller/turnOnPump2").setValue(0);
                }
            }
        });

        fanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/controller/turnOnFan").setValue(1);
                } else {
                    mdata.child("json/controller/turnOnFan").setValue(0);
                }
            }
        });

        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/controller/turnOnLight").setValue(1);
                } else {
                    mdata.child("json/controller/turnOnLight").setValue(0);
                }
            }
        });

        listenToSensorData();
        button_apply_set.setOnClickListener(v -> showCustomDialog());
    }

    private void showCustomDialog() {

        // Inflate layout từ XML
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom_setting, null);

        // Tạo AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Ánh xạ các view bên trong dialog
        timeStartFan = dialogView.findViewById(R.id.timeStartFan);
        timeFinishFan = dialogView.findViewById(R.id.timeFinishFan);
        timeStartLighting = dialogView.findViewById(R.id.timeStartLighting);
        timeFinishLighting = dialogView.findViewById(R.id.timeFinishLighting);
        timeStartWatering = dialogView.findViewById(R.id.timeStartWatering);
        timeFinishWatering = dialogView.findViewById(R.id.timeFinishWatering);
        timeStartWateringNutri = dialogView.findViewById(R.id.timeStartWateringNutri);
        timeFinishWateringNutri = dialogView.findViewById(R.id.timeFinishWateringNutri);
        getUserSetting();

        Switch autoFan = dialogView.findViewById(R.id.auto_fan);
        Switch autoLight = dialogView.findViewById(R.id.auto_light);
        Switch autoWater = dialogView.findViewById(R.id.auto_watering);
        Switch autoWaterNutri = dialogView.findViewById(R.id.auto_watering_nutri);

        Button applyTimeFan = dialogView.findViewById(R.id.apply_time_fan);
        Button applyTimeLight = dialogView.findViewById(R.id.apply_time_light);
        Button applyTimeWater = dialogView.findViewById(R.id.apply_time_wattering);
        Button applyTimeWaterNutri = dialogView.findViewById(R.id.apply_time_wattering_nutri);

        autoFan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/timeSetting/autoFan").setValue(1);
                    applyTimeFan.setVisibility(View.VISIBLE);
                } else {
                    applyTimeFan.setVisibility(View.GONE);
                    mdata.child("json/timeSetting/autoFan").setValue(0);
                    mdata.child("json/timeSetting/timeStartFan").setValue(0);
                    mdata.child("json/timeSetting/timeFinishFan").setValue(0);
                }
            }
        });

        autoLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/timeSetting/autoLight").setValue(1);
                    applyTimeLight.setVisibility(View.VISIBLE);
                } else {
                    applyTimeLight.setVisibility(View.GONE);
                    mdata.child("json/timeSetting/autoLight").setValue(0);
                    mdata.child("json/timeSetting/timeStartLighting").setValue(0);
                    mdata.child("json/timeSetting/timeFinishLighting").setValue(0);
                }
            }
        });

        autoWater.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/timeSetting/autoWater").setValue(1);
                    applyTimeWater.setVisibility(View.VISIBLE);
                } else {
                    applyTimeWater.setVisibility(View.GONE);
                    mdata.child("json/timeSetting/autoWater").setValue(0);
                    mdata.child("json/timeSetting/timeStartWatering").setValue(0);
                    mdata.child("json/timeSetting/timeFinishWatering").setValue(0);
                }
            }
        });

        autoWaterNutri.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mdata.child("json/timeSetting/autoWaterNutri").setValue(1);
                    applyTimeWaterNutri.setVisibility(View.VISIBLE);
                } else {
                    applyTimeWaterNutri.setVisibility(View.GONE);
                    mdata.child("json/timeSetting/autoWaterNutri").setValue(0);
                    mdata.child("json/timeSetting/timeStartWateringNutri").setValue(0);
                    mdata.child("json/timeSetting/timeFinishWateringNutri").setValue(0);
                }
            }
        });

        mdata.child("json/timeSetting/autoFan").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        autoFan.setChecked(true);
                    } else {
                        autoFan.setChecked(false);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoLight").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        autoLight.setChecked(true);
                    } else {
                        autoLight.setChecked(false);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoWater").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        autoWater.setChecked(true);
                    } else {
                        autoWater.setChecked(false);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoWaterNutri").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        autoWaterNutri.setChecked(true);
                    } else {
                        autoWaterNutri.setChecked(false);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        SharedPreferences sharedPreferences_device = getSharedPreferences("Device", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences_device.edit();

        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        timeStartFan.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                // Format time as HH:mm
                timeStartFan.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeStartFan_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeStartFan", timeStartFan_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeFinishFan.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeFinishFan.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeFinishFan_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeFinishFan", timeFinishFan_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeStartLighting.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeStartLighting.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeStartLighting_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeStartLighting", timeStartLighting_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeFinishLighting.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeFinishLighting.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeFinishLighting_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeFinishLighting", timeFinishLighting_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeStartWatering.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeStartWatering.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeStartWatering_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeStartWatering", timeStartWatering_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeFinishWatering.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeFinishWatering.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeFinishWatering_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeFinishWatering", timeFinishWatering_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeStartWateringNutri.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeStartWateringNutri.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeStartWateringNutri_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeStartWateringNutri", timeStartWateringNutri_user);
                editor.apply();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        });

        timeFinishWateringNutri.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                timeFinishWateringNutri.setText(String.format("%02d:%02d", hourOfDay, minute));
                timeFinishWateringNutri_user = String.format("%02d:%02d", hourOfDay, minute);
                editor.putString("timeFinishWateringNutri", timeFinishWateringNutri_user);
                editor.apply();
            }, currentHour, currentMinute, true);

            timePickerDialog.show();
        });
        // Xử lý logic khi nhấn nút "Set" trong dialog
        applyTimeFan.setOnClickListener(v -> {
            String timeStartFan_string = convertString(timeStartFan.getText().toString().trim());
            String timeFinishFan_string = convertString(timeFinishFan.getText().toString().trim());
            int timeStartFan_num = Integer.parseInt(timeStartFan_string);
            int timeFinishFan_num = Integer.parseInt(timeFinishFan_string);
            mdata.child("json/timeSetting/timeStartFan").setValue(timeStartFan_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });

            mdata.child("json/timeSetting/timeFinishFan").setValue(timeFinishFan_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });
        });

        applyTimeLight.setOnClickListener(v -> {
            String timeStartLight_string = convertString(timeStartLighting.getText().toString().trim());
            String timeFinishLight_string = convertString(timeFinishLighting.getText().toString().trim());
            int timeStartLight_num = Integer.parseInt(timeStartLight_string);
            int timeFinishLight_num = Integer.parseInt(timeFinishLight_string);
            mdata.child("json/timeSetting/timeStartLighting").setValue(timeStartLight_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });

            mdata.child("json/timeSetting/timeFinishLighting").setValue(timeFinishLight_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });
        });

        applyTimeWater.setOnClickListener(v -> {
            String timeStartWatering_string = convertString(timeStartWatering.getText().toString().trim());
            String timeFinishWatering_string = convertString(timeFinishWatering.getText().toString().trim());
            int timeStartWatering_num = Integer.parseInt(timeStartWatering_string);
            int timeFinishWatering_num = Integer.parseInt(timeFinishWatering_string);
            mdata.child("json/timeSetting/timeStartWatering").setValue(timeStartWatering_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });

            mdata.child("json/timeSetting/timeFinishWatering").setValue(timeFinishWatering_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });
        });

        applyTimeWaterNutri.setOnClickListener(v -> {
            String timeStartWateringNutri_string = convertString(timeStartWateringNutri.getText().toString().trim());
            String timeFinishWateringNutri_string = convertString(timeFinishWateringNutri.getText().toString().trim());
            int timeStartWateringNutri_num = Integer.parseInt(timeStartWateringNutri_string);
            int timeFinishWateringNutri_num = Integer.parseInt(timeFinishWateringNutri_string);
            mdata.child("json/timeSetting/timeStartWateringNutri").setValue(timeStartWateringNutri_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });

            mdata.child("json/timeSetting/timeFinishWateringNutri").setValue(timeFinishWateringNutri_num).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ControlDeviceActivity.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ControlDeviceActivity.this, "Failed to send Data", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Hiển thị dialog
        dialog.show();
    }

    private String convertString(String input) {
        if (input == null || !input.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Input must be in the format HH:mm");
        }
        return input.replace(":", "");
    }


    private void getUserSetting() {
        //Time Start FAN
        SharedPreferences sharedPreferences = getSharedPreferences("Device", MODE_PRIVATE);

        timeStartFan.setText(sharedPreferences.getString("timeStartFan", "00:00"));
        timeFinishFan.setText(sharedPreferences.getString("timeFinishFan", "00:00"));
        timeStartLighting.setText(sharedPreferences.getString("timeStartLighting", "00:00"));
        timeFinishLighting.setText(sharedPreferences.getString("timeFinishLighting", "00:00"));
        timeStartWatering.setText(sharedPreferences.getString("timeStartWatering", "00:00"));
        timeFinishWatering.setText(sharedPreferences.getString("timeFinishWatering", "00:00"));
        timeStartWateringNutri.setText(sharedPreferences.getString("timeStartWateringNutri", "00:00"));
        timeFinishWateringNutri.setText(sharedPreferences.getString("timeFinishWateringNutri", "00:00"));

    }

    private void listenToSensorData() {
        // temperature
        mdata.child("json/dataFromSensor/temperature").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String temperature = String.valueOf(snapshot.getValue(Float.class));
                    tv_temperatureValue.setText(temperature != null ? temperature + "°C" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_temperatureValue.setText("Error");
            }
        });

        // humidity
        mdata.child("json/dataFromSensor/humidity").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String humidity = String.valueOf(snapshot.getValue(Float.class));
                    tv_humidityValue.setText(humidity != null ? humidity + "%" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_humidityValue.setText("Error");
            }
        });

        // soilMoisture
        mdata.child("json/dataFromSensor/soilMoisture").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String soilMoisture = String.valueOf(snapshot.getValue(Long.class));
                    tv_soilMoistureValue.setText(soilMoisture != null ? soilMoisture + "%" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_soilMoistureValue.setText("Error");
            }
        });

        // ph
        mdata.child("json/dataFromSensor/ph").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String ph = String.valueOf(snapshot.getValue(Float.class));
                    tv_phValue.setText(ph != null ? ph : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_phValue.setText("Error");
            }
        });

        //get soilMoisture set data from AI
        mdata.child("json/dataFromAI/soilMoisture").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long soilMoisture = Long.valueOf(snapshot.getValue(Long.class));
                    tv_ai_soilMoisture.setText(soilMoisture != null ? "Soil Moisture: " + soilMoisture + "%" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mdata.child("json/dataFromAI/lightingDuration").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long lightingDuration = Long.valueOf(snapshot.getValue(Long.class));
                    tv_ai_Light_Duration.setText(lightingDuration != null ? "Lighting Duration: " + lightingDuration + "h" : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mdata.child("json/controller/isAuto").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        autoModeSwitch.setChecked(true);
                    } else {
                        autoModeSwitch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/controller/turnOnFan").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        fanSwitch.setChecked(true);
                    } else {
                        fanSwitch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/controller/turnOnLight").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        ledSwitch.setChecked(true);
                    } else {
                        ledSwitch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/controller/turnOnPump1").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        pump1Switch.setChecked(true);
                    } else {
                        pump1Switch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/controller/turnOnPump2").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        pump2Switch.setChecked(true);
                    } else {
                        pump2Switch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoFan").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        fanSwitch.setChecked(false);
                        fanSwitch.setEnabled(false);
                    } else {
                        fanSwitch.setEnabled(true);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoLight").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        ledSwitch.setChecked(false);
                        ledSwitch.setEnabled(false);
                    } else {
                        ledSwitch.setEnabled(true);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoWater").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        pump2Switch.setChecked(false);
                        pump2Switch.setEnabled(false);
                    } else {
                        pump2Switch.setEnabled(true);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        mdata.child("json/timeSetting/autoWaterNutri").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long value = Long.valueOf(snapshot.getValue(Long.class));
                    if (value == 1) {
                        pump1Switch.setChecked(false);
                        pump1Switch.setEnabled(false);
                    } else {
                        pump1Switch.setEnabled(true);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private class LoadPermissionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String permission = null;
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    String query = "SELECT DO.Permission " +
                            "FROM DeviceOwner DO " +
                            "INNER JOIN [User] U ON DO.UserID = U.ID " +
                            "INNER JOIN Device D ON DO.DeviceID = D.ID " +
                            "WHERE U.UserName = ? AND D.DeviceCode = ?";
                    PreparedStatement stmt = conn.prepareStatement(query);
                    stmt.setString(1, username);
                    stmt.setInt(2, deviceCode);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        permission = rs.getString("Permission");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return permission;
        }

        @Override
        protected void onPostExecute(String permission) {
            if ("user_root".equals(permission)) {
                button_manage.setVisibility(View.VISIBLE);
                button_manage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ControlDeviceActivity.this, ManageDeviceOwnerActivity.class);
                        intent.putExtra("DeviceCode", deviceCode);
                        startActivity(intent);

                    }
                });
            } else {
                button_manage.setVisibility(View.GONE);
            }
        }
    }
}