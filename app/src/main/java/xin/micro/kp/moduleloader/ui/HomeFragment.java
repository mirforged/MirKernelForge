package xin.micro.kp.moduleloader.ui;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.root.RootShellUtil;

public class HomeFragment extends MyFragment {
    private TextView rootStatusText;
    private CardView systemInfoCard;
    private TextView deviceModel;
    private TextView androidVersion;
    private TextView kernelVersion;
    private TextView systemVersion;
    private TextView additionalInformation;
    private Button btnRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        checkRootStatus();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void initViews(View view) {
        rootStatusText = view.findViewById(R.id.root_status_text);
        systemInfoCard = view.findViewById(R.id.system_info_card);
        deviceModel = view.findViewById(R.id.device_model);
        androidVersion = view.findViewById(R.id.android_version);
        kernelVersion = view.findViewById(R.id.kernel_version);
        systemVersion = view.findViewById(R.id.system_version);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        additionalInformation= view.findViewById(R.id.additional_information);
    }

    private void setupListeners() {
        btnRefresh.setOnClickListener(v -> {
            if (RootShellUtil.isRootAvailable()) {
                loadSystemInfo();
            } else {
                checkRootStatus();
            }
        });
    }

    private void checkRootStatus() {
        updateRootStatus(STATUS_CHECKING, "检测中...");

        new Thread(() -> {
            boolean hasRoot = RootShellUtil.hasRoot();

            requireActivity().runOnUiThread(() -> {
                if (hasRoot) {
                    updateRootStatus(STATUS_SUCCESS, "Root 已获取 ✓");
                    systemInfoCard.setVisibility(View.VISIBLE);
                    loadSystemInfo();
                } else {
                    updateRootStatus(STATUS_ERROR, "未获取 Root 权限 ✗");
                    systemInfoCard.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private static final int STATUS_CHECKING = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_ERROR = 2;

    private void updateRootStatus(int status, String text) {
        rootStatusText.setText(text);

        int colorRes;
        switch (status) {
            case STATUS_SUCCESS:
                colorRes = R.attr.colorSuccess;
                break;
            case STATUS_ERROR:
                colorRes = R.attr.colorError;
                break;
            default:
                colorRes = R.attr.colorWarning;
                break;
        }
    }

    private int getColorFromAttr(int attrRes) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attrRes, typedValue, true);
        return ContextCompat.getColor(requireContext(), typedValue.resourceId);
    }

    private void loadSystemInfo() {
        deviceModel.setText(Build.MODEL);
        androidVersion.setText(Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");

        String systemVersionStr = Build.DISPLAY;
        if (systemVersionStr == null || systemVersionStr.isEmpty()) {
            systemVersionStr = Build.VERSION.RELEASE;
        }
        systemVersion.setText(systemVersionStr);

        loadKernelVersion();

        //这是我写过第二烂的代码
        StringBuilder builder = new StringBuilder();
        builder.append("\n[其他]");
        RootShellUtil.ShellResult result;
        result = RootShellUtil.execCommand("id -Z");
        builder.append("\nContext: ");
        builder.append(result.output);
        result = RootShellUtil.execCommand("uname -m");
        builder.append("\nArch: ");
        builder.append(result.output);
        additionalInformation.setText(builder);
    }

    private void loadKernelVersion() {
        new Thread(() -> {
            String kernelVer = getKernelVersion();
            requireActivity().runOnUiThread(() -> kernelVersion.setText(kernelVer));
        }).start();
    }

    private static final String TAG = "HomeFragment";

    private String getKernelVersion() {

        String[] commands = {
                "uname -r",
                "cat /proc/version | awk '{print $3}'",
                "getprop ro.kernel.version"
        };

        for (int i = 0; i < commands.length; i++) {
            String cmd = commands[i];
            try {
                RootShellUtil.ShellResult result = RootShellUtil.execCommand(cmd);

                if (result.isSuccess && result.output != null && !result.output.isEmpty()) {
                    String version = result.output.trim();
                    if (!version.isEmpty() && !version.contains("error")) {
                        return version;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "执行命令异常: " + cmd, e);
            }
        }

        return "未知";
    }
}