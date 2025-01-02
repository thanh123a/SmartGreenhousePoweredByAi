package com.example.moblieapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.entity.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_list_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textUserName.setText("Username: " + user.getUsername());
        holder.textEmail.setText("Email: " + user.getEmail());
        holder.textPhoneNumber.setText("Phone: " + user.getPhone());
        holder.textNumberOfDevices.setText("Devices: " + user.getDeviceCount());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textEmail, textPhoneNumber, textNumberOfDevices;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textUserName);
            textEmail = itemView.findViewById(R.id.textEmail);
            textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
            textNumberOfDevices = itemView.findViewById(R.id.textNumberOfDevices);
        }
    }
}

