package xin.micro.kp.moduleloader.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.util.KernelPatch;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class ModulesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modules, container, false);
    }

    private Button temp_buttom;
    private Button temp_buttom2;
    private Button temp_buttom3;
    private TextView modulesLogs;
    static boolean isGUI = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        modulesLogs = view.findViewById(R.id.modules_logs);
        temp_buttom = view.findViewById(R.id.btn_temp1);
        temp_buttom.setOnClickListener(v -> {
            if(KernelPatch.getInstance().refreshStatusFull(requireContext())){
                modulesLogs.setText("Successful");
            }
        });
        temp_buttom2 = view.findViewById(R.id.btn_temp2);
        temp_buttom2.setOnClickListener(v -> {
            KernelPatch.getInstance().doGetPatchInformation();
            MagicUtil.PatchInfo info = KernelPatch.getInstance().getPatchInformation();
            if(info.isPatched()){
                modulesLogs.setText("banner: " + info.banner() + "\nisPatched: " + info.isPatched());
                LinearLayout parentLayout = view.findViewById(R.id.modules_card_container);
                parentLayout.removeAllViews();

                // Inflate 布局
                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_item, parentLayout, false);

                TextView titleText = cardView.findViewById(R.id.txt_title);
                titleText.setText("动态标题");

                TextView contentText = cardView.findViewById(R.id.txt_content);
                contentText.setText("动态内容");

                parentLayout.addView(cardView);
            }else{
                modulesLogs.setText("banner: " + info.banner() + "\nisPatched: " + info.isPatched());
            }
        });
        temp_buttom3 = view.findViewById(R.id.btn_temp3);
        temp_buttom3.setOnClickListener(v -> {
            if (isGUI){
                modulesLogs.setVisibility(View.GONE);
            }else{
                modulesLogs.setVisibility(View.VISIBLE);
            }
            isGUI = !isGUI;
        });
    }
}