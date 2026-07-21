package xin.micro.kp.moduleloader.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

    private void refreshStatus(View view) {
        LinearLayout parentLayout = view.findViewById(R.id.modules_card_container);
        parentLayout.removeAllViews();

        for (InstantKernAPI.IKA_KPMItem item : InstantKernAPI.getLoadedKPM()) {
            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.btn_card_item, parentLayout, false);

            TextView titleText = cardView.findViewById(R.id.card_title);
            titleText.setText(item.name());

            TextView contentText = cardView.findViewById(R.id.card_description);
            contentText.setText("协议版本：" + item.version() + "\n作者：" + item.author()
                    + "\n许可证：" + item.license() + "\n描述：" + item.description());

            Button detailButton = cardView.findViewById(R.id.btn_card_detail);
            detailButton.setText("操作");
            detailButton.setOnClickListener(v1 -> showOperationDialog(item, view));

            parentLayout.addView(cardView);
        }
    }

    private void showOperationDialog(InstantKernAPI.IKA_KPMItem item, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("操作模块: " + item.name());

        // 创建输入框
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("请输入字符串");
        builder.setView(input);

        builder.setPositiveButton("调参", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                showResultDialog("错误", "参数不能为空");
                return;
            }

            // 执行 controlKPM
            String result = InstantKernAPI.controlKPM(item.name(), value);

            // 显示结果
            showResultDialog("调参结果", result);
            refreshStatus(view);
        });

        builder.setNeutralButton("卸载模块", (dialog, which) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("确认卸载")
                    .setMessage("确定要卸载 " + item.name() + " 吗？")
                    .setPositiveButton("确定", (dialog1, which1) -> {
                        InstantKernAPI.unloadKPM(item.name());
                        showResultDialog("卸载完成", "模块 " + item.name() + " 已卸载");
                        refreshStatus(view);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    //一个简单的仅包含确定的弹窗
    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
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