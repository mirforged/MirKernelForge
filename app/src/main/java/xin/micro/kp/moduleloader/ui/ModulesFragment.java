package xin.micro.kp.moduleloader.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.root.RootShellUtil;

public class ModulesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modules, container, false);
    }

    private Button temp_buttom;
    private TextView txtv111;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtv111 = view.findViewById(R.id.txt_temp1);
        temp_buttom = view.findViewById(R.id.btn_temp1);
        temp_buttom.setOnClickListener(v -> {
            RootShellUtil.ShellResult result = RootShellUtil.execScriptFromAssets(requireContext(),"scripts/test.sh");
            txtv111.setText(result.message);
        });
    }
}