package com.example.chatroomtest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chatroomtest.databinding.FragmentLoginBinding;

import java.util.regex.Pattern;


public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private EditText etUserName, etIpAddress;
    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        etUserName = binding.etUserName;
        btnLogin = binding.btnLogin;
        etIpAddress = binding.etIpAddress;

        btnLogin.setOnClickListener(v -> {
            String userName = etUserName.getText().toString().trim();
            String ip = etIpAddress.getText().toString().trim();
            if (validateInput(userName, ip)) {
                ChatFragment chatFragment = new ChatFragment();
                Bundle args = new Bundle();
                args.putString("username", userName);
                args.putString("ip", ip);
                chatFragment.setArguments(args);
                ((MainActivity) requireActivity()).showFragment(chatFragment);
            }

        });
        return binding.getRoot();
    }

    private boolean validateInput(String userName, String ip) {
        if (userName.isEmpty()) {
            etUserName.setError("用户名不能为空");
            return false;
        }
        if (ip.isEmpty() || !isValidIp(ip)) {
            etIpAddress.setError("请输入有效的IP地址");
            return false;
        }
        return true;
    }

    private boolean isValidIp(String ip) {
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(pattern, ip);
    }
}
