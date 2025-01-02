package com.example.moblieapplication.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moblieapplication.R;
import com.example.moblieapplication.controller.ManageDeviceOwnerActivity;
import com.example.moblieapplication.database.DatabaseHelper;
import com.example.moblieapplication.entity.DeviceOwner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DeviceOwnerAdapter extends RecyclerView.Adapter<DeviceOwnerAdapter.ViewHolder> {

    private final List<DeviceOwner> deviceOwners;
    private final int deviceCode; // Pass deviceCode for deletion
    private final Context context;

    public DeviceOwnerAdapter(List<DeviceOwner> deviceOwners, int deviceCode, Context context) {
        this.deviceOwners = deviceOwners;
        this.deviceCode = deviceCode;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_owner_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceOwner owner = deviceOwners.get(position);


        holder.textEmail.setText(owner.getEmail());
        holder.textPhone.setText(owner.getPhone());
        holder.textPermission.setText(owner.getPermission());
        holder.textUserName.setText(owner.getUserName());

        holder.buttonRemoveUser.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove User")
                    .setMessage("Are you sure you want to remove this user's control permission?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        removeUser(owner.getUserName());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        if ("user_root".equals(owner.getPermission())) {
            holder.buttonRemoveUser.setVisibility(View.GONE);
            holder.textUserName.setTextColor(Color.rgb(200, 0, 0));
        }
    }

    private void removeUser(String userName) {
        new RemoveUserTask(userName).execute();
    }

    @Override
    public int getItemCount() {
        return deviceOwners.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName, textEmail, textPhone, textPermission;
        Button buttonRemoveUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.textViewUserName);
            textEmail = itemView.findViewById(R.id.textViewEmail);
            textPhone = itemView.findViewById(R.id.textViewPhone);
            textPermission = itemView.findViewById(R.id.textViewPermission);
            buttonRemoveUser = itemView.findViewById(R.id.removeUser);
        }
    }

    private class RemoveUserTask extends AsyncTask<Void, Void, String> {
        private final String userName;

        RemoveUserTask(String userName) {
            this.userName = userName;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (conn != null) {
                    // Get UserID from UserName
                    String userIdQuery = "SELECT ID FROM [User] WHERE UserName = ?";
                    PreparedStatement userIdStmt = conn.prepareStatement(userIdQuery);
                    userIdStmt.setString(1, userName);
                    ResultSet rs = userIdStmt.executeQuery();

                    if (rs.next()) {
                        int userId = rs.getInt("ID");

                        // Delete User from DeviceOwner
                        String deleteQuery = "DELETE FROM DeviceOwner WHERE UserID = ? AND DeviceID = (SELECT ID FROM Device WHERE DeviceCode = ?)";
                        PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                        deleteStmt.setInt(1, userId);
                        deleteStmt.setInt(2, deviceCode);
                        int rowsAffected = deleteStmt.executeUpdate();

                        if (rowsAffected > 0) {
                            return "User removed successfully!";
                        } else {
                            return "Failed to remove user!";
                        }
                    } else {
                        return "User not found!";
                    }
                } else {
                    return "Database connection failed!";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error removing user!";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
            if (result.equals("User removed successfully!")) {
                ((ManageDeviceOwnerActivity) context).refreshList(); // Refresh the list
            }
        }
    }
}


