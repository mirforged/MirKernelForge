package xin.micro.kp.moduleloader.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.kp.KPMItem;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.utils.FileUtil;
import xin.micro.kp.moduleloader.utils.addition.InstantKernAPI;

public class ModulesFragment extends MyFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modules, container, false);
    }

    private Button btnRefreshBootFull;
    private Button btnGetPatchStatus;
    private Button btnLoadKpm;
    private TextView modulesLogs;
    private FileUtil fileUtil;

    @Override
    public void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileUtil = new FileUtil();
        fileUtil.init(this);
    }
    @Override
    public void onShow(View view) {
        refreshStatus(view);
    }

    private void refreshStatus(View view){

            LinearLayout parentLayout = view.findViewById(R.id.modules_card_container);
            parentLayout.removeAllViews();

            // Inflate 布局

            // TODO: instant kern api switch
            for (InstantKernAPI.IKA_KPMItem item : InstantKernAPI.getLoadedKPM()) {
                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.btn_card_item, parentLayout, false);

                TextView titleText = cardView.findViewById(R.id.card_title);
                titleText.setText(item.name());

                TextView contentText = cardView.findViewById(R.id.card_description);
                contentText.setText("协议版本：" + item.version() + "\n作者：" + item.author()
                        + "\n许可证：" + item.license() + "\n描述：" + item.description());

                Button detailButton = cardView.findViewById(R.id.btn_card_detail);
                detailButton.setOnClickListener(v1 -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("你要卸载他吗？" + item.name())
                            .setMessage("描述: " + item.description())
                            .setPositiveButton("确定", (dialog, which) -> {
                                InstantKernAPI.unloadKPM(item.name());
                                this.refreshStatus(view);
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .show();
                });

                parentLayout.addView(cardView);
            }
    }

    @SuppressLint({"CommitPrefEdits", "SetTextI18n"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //==========================================================================================


        modulesLogs = view.findViewById(R.id.modules_logs);
        btnGetPatchStatus = view.findViewById(R.id.btn_refresh_status);
        btnGetPatchStatus.setOnClickListener(v -> {
            refreshStatus(view);
        });
        //btn_load_kpm
        btnLoadKpm = view.findViewById(R.id.btn_load_kpm);
        btnLoadKpm.setOnClickListener(v -> {
            fileUtil.pickFile();
            fileUtil.setListener(path -> {
                if (path != null) {
                    KPMItem kpmItem = new KPMItem(path);
                    kpmItem.complete();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("即将加载：" + kpmItem.name)
                            .setMessage("描述: " + kpmItem.description)
                            .setPositiveButton("确定", (dialog, which) -> {
                                InstantKernAPI.loadKPM(path);
                                this.refreshStatus(view);
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .setCancelable(true)
                            .show();
                } else {
                    Toast.makeText(getContext(), "FilePath为Null", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}