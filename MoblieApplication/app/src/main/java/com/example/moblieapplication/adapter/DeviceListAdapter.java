package com.example.moblieapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.entity.DeviceList;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    private List<DeviceList> deviceList;

    public DeviceListAdapter(List<DeviceList> deviceList) {
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceList device = deviceList.get(position);
        holder.textDeviceCode.setText("Device Code: " + device.getDeviceCode());
        holder.textDescription.setText("Description: " + device.getDescription());
        holder.textOwner.setText("Owner: " + device.getOwner());
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView textDeviceCode, textDescription, textOwner;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceCode = itemView.findViewById(R.id.textDeviceCode);
            textDescription = itemView.findViewById(R.id.textDescription);
            textOwner = itemView.findViewById(R.id.textOwner);
        }
    }
}

