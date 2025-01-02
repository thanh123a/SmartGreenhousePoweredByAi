package com.example.moblieapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.entity.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<Device> deviceList;
    private final OnItemClickListener listener;


    public interface OnItemClickListener {
        void onItemClick(Device device);
    }

    public DeviceAdapter(List<Device> deviceList, OnItemClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewDeviceCode;
        private final TextView textViewTemperature;
        private final TextView textViewHumidity;
        private final TextView textViewSoilMoisture;
        private final TextView textViewPhLevel;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDeviceCode = itemView.findViewById(R.id.textViewDeviceCode);
            textViewTemperature = itemView.findViewById(R.id.textViewTemperature);
            textViewHumidity = itemView.findViewById(R.id.textViewHumidity);
            textViewSoilMoisture = itemView.findViewById(R.id.textViewSoilMoisture);
            textViewPhLevel = itemView.findViewById(R.id.textViewPhLevel);
        }

        public void bind(final Device device, final OnItemClickListener listener) {
            textViewDeviceCode.setText("Device: " + device.getDeviceCode());
            textViewTemperature.setText(device.getTemperature() + "°C");
            textViewHumidity.setText(device.getHumidity() + "%");
            textViewSoilMoisture.setText(device.getSoilMoisture() + "%");
            textViewPhLevel.setText(device.getPhLevel() + "");
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(device);
                }
            });

        }
    }
}

