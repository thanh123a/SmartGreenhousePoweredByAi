package com.example.moblieapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.moblieapplication.R;
import com.example.moblieapplication.controller.ChangePasswordActivity;
import com.example.moblieapplication.controller.LoginActivity;
import com.example.moblieapplication.controller.ViewProfileActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OptionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OptionFragment extends Fragment {

    private CardView cardViewProfileDetail;
    private CardView cardViewChangePassword;
    private CardView cardViewAppInfor;
    private CardView cardViewLogOut;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OptionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OptionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OptionFragment newInstance(String param1, String param2) {
        OptionFragment fragment = new OptionFragment();
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
        View view = inflater.inflate(R.layout.fragment_option, container, false);

        cardViewProfileDetail = view.findViewById(R.id.card_view_profile_detail);
        cardViewChangePassword = view.findViewById(R.id.card_view_change_pass);
        cardViewAppInfor = view.findViewById(R.id.card_view_app_infor);
        cardViewLogOut = view.findViewById(R.id.card_view_logout);

        // View Profile
        cardViewProfileDetail.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewProfileActivity.class);
            startActivity(intent);
        });

        //LogOut
        cardViewLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        //change pass
        cardViewChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            }
        });


        return view;
    }

    private void logout() {
        // Clear SharedPreferences to log the user out
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Show a logout confirmation message
        Toast.makeText(getActivity(), "Logged out successfully!", Toast.LENGTH_LONG).show();

        // Navigate back to LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);

        // Optionally finish the current activity to prevent going back to it
        getActivity().finish();
    }
}