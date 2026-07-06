package xin.micro.kp.moduleloader.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    private ActivityResultLauncher<Intent> launcher;
    private OnFileResultListener resultListener;

    public interface OnFileResultListener {
        void onResult(String absolutePath);
    }

    /**
     * 在Activity中初始化
     */
    public void init(AppCompatActivity activity) {
        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleResult(result.getResultCode(), result.getData(), activity)
        );
    }

    /**
     * 在Fragment中初始化
     */
    public void init(Fragment fragment) {
        launcher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (fragment.getActivity() != null) {
                        handleResult(result.getResultCode(), result.getData(), fragment.getActivity());
                    }
                }
        );
    }

    /**
     * 设置回调
     */
    public void setListener(OnFileResultListener listener) {
        this.resultListener = listener;
    }

    /**
     * 打开文件管理器选择文件
     */
    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        launcher.launch(intent);
    }

    /**
     * 处理选择结果
     */
    private void handleResult(int resultCode, Intent data, Activity activity) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = copyToTemp(activity, uri);
                if (resultListener != null) {
                    resultListener.onResult(path);
                }
            }
        }
    }

    /**
     * 复制到应用临时目录并返回绝对路径
     */
    private String copyToTemp(Activity activity, Uri uri) {
        try {
            File tempDir = new File(activity.getCacheDir(), "temp");
            if (!tempDir.exists()) tempDir.mkdirs();

            String fileName = getFileName(uri);
            File tempFile = new File(tempDir, fileName);

            InputStream is = activity.getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            is.close();
            os.close();

            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取文件名
     */
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int last = path.lastIndexOf('/');
            if (last != -1) {
                return path.substring(last + 1);
            }
        }
        return "temp_" + System.currentTimeMillis();
    }
}