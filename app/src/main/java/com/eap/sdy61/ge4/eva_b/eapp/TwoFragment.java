package com.eap.sdy61.ge4.eva_b.eapp;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class TwoFragment extends Fragment {
    Button registerBtn;
    EditText username;
    EditText email;
    EditText password;

    public TwoFragment() {
        // Required empty public constructor
    }


    public static TwoFragment newInstance(String param1, String param2) {
        TwoFragment fragment = new TwoFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_two, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        registerBtn = getView().findViewById(R.id.btnRegister);
        username = getView().findViewById(R.id.userName);
        email = getView().findViewById(R.id.userEmail);
        password = getView().findViewById(R.id.userPassword);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernameStr = username.getText().toString();
                String emailStr = email.getText().toString();
                String passwordStr = password.getText().toString();

                if (TextUtils.isEmpty(usernameStr) || usernameStr.trim().isEmpty() ||
                        TextUtils.isEmpty(emailStr) || emailStr.trim().isEmpty()  ||
                        TextUtils.isEmpty(passwordStr) || passwordStr.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Παρακαλούμε συμπληρώστε όλα τα πεδία.", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(getContext(), "Επιτυχής εγγραφή!", Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
