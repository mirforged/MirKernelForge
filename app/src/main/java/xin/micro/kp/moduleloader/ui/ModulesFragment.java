package xin.micro.kp.moduleloader.ui;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
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
import androidx.fragment.app.Fragment;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class ModulesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modules, container, false);
    }

    private Button btnRefreshBootFull;
    private Button btnGetPatchStatus;
    private TextView modulesLogs;
    SharedPreferences sp;

    @SuppressLint({"CommitPrefEdits", "SetTextI18n"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sp = requireContext().getSharedPreferences("my_config", MODE_PRIVATE);
        String currBootSha = MagicUtil.getCurrBootSha256();
        String recordedBootSha = sp.getString("recorded_boot_sha256","NULL");
        if (recordedBootSha.equals("NULL")){
            sp.edit().putString("recorded_boot_sha256",MagicUtil.getCurrBootSha256()).apply();
            KernelPatch.getInstance().refreshStatusFull(requireContext());
            Toast.makeText(requireContext(),"初次使用？ 已自动拉取boot分区",Toast.LENGTH_SHORT).show();
        }
        if (!currBootSha.equals(recordedBootSha)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("工作空间boot.img与实际boot分区不匹配")
                    .setMessage("如果你不清楚你在做什么 请参考 \n如果你在软件外曾更新过boot分区*请重新拉取* \n如果你清楚自己在做什么 可以取消\n\n选择错误将会导致boot回退*")
                    .setPositiveButton("重新拉取", (dialog, which) -> {
                        // 用户点击确定

                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 用户点击取消
                        dialog.dismiss();
                    })
                    .setCancelable(true) // 点击外部可取消
                    .show();
        }


        modulesLogs = view.findViewById(R.id.modules_logs);
        btnRefreshBootFull = view.findViewById(R.id.btn_refresh_boot_full);
        btnRefreshBootFull.setOnClickListener(v -> {
            if (KernelPatch.getInstance().refreshStatusFull(requireContext())) {
                modulesLogs.setText("Successful");
            }
        });
        btnGetPatchStatus = view.findViewById(R.id.btn_get_patch_status);
        btnGetPatchStatus.setOnClickListener(v -> {
            KernelPatch.getInstance().doGetPatchInformation();
            MagicUtil.PatchInfo info = KernelPatch.getInstance().getPatchInformation();
            if (info.isPatched()) {
                modulesLogs.setText("banner: " + info.banner() +
                        "\nisPatched: " + info.isPatched() +
                        "\n\n" + sp.getString("boot_sha256", "NULL")

                );
                LinearLayout parentLayout = view.findViewById(R.id.modules_card_container);
                parentLayout.removeAllViews();

                // Inflate 布局
                View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_item, parentLayout, false);

                TextView titleText = cardView.findViewById(R.id.txt_title);
                titleText.setText("动态标题");

                TextView contentText = cardView.findViewById(R.id.txt_content);
                contentText.setText("动态内容");

                parentLayout.addView(cardView);
            } else {
                modulesLogs.setText("banner: " + info.banner() + "\nisPatched: " + info.isPatched());
            }
        });
    }
}