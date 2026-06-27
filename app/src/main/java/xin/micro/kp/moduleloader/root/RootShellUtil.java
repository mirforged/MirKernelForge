package xin.micro.kp.moduleloader.root;

import android.content.Context;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RootShellUtil {
    private static final String TAG = "RootShellUtil";
    private static boolean isRootAvailable = false;
    private static boolean isInitialized = false;

    /**
     * 初始化Root Shell环境
     * @param context 上下文
     * @return true表示root可用
     */
    public static boolean initRoot(Context context) {
        if (isInitialized) {
            return isRootAvailable;
        }

        try {
            // 设置默认的Shell Builder
            Shell.Builder builder = Shell.Builder.create();

            Shell.setDefaultBuilder(builder);

            // 执行简单的命令测试
            Shell.Result result = Shell.cmd("id").exec();
            String output = String.join("\n", result.getOut());
            if (result.isSuccess() && output.contains("uid=0")) {
                isRootAvailable = true;
                Log.d(TAG, "Root权限获取成功");
            } else {
                isRootAvailable = false;
                Log.e(TAG, "Root权限获取失败，输出: " + output);
            }
        } catch (Exception e) {
            isRootAvailable = false;
            Log.e(TAG, "初始化Root失败", e);
        }

        isInitialized = true;
        return isRootAvailable;
    }

    /**
     * 检查是否拥有root权限
     */
    public static boolean hasRoot() {
        if (!isInitialized) {
            Log.w(TAG, "Root未初始化，请先调用initRoot()");
            return false;
        }

        try {
            Shell.Result result = Shell.cmd("id").exec();
            String output = String.join("\n", result.getOut());
            return result.isSuccess() && output.contains("uid=0");
        } catch (Exception e) {
            Log.e(TAG, "检查root权限失败", e);
            return false;
        }
    }

    /**
     * 执行shell命令（同步）
     * @param command shell命令
     * @return 执行结果
     */
    public static ShellResult execCommand(String command) {
        ShellResult result = new ShellResult();
        List<String> output = new ArrayList<>();

        try {
            Shell.Result shellResult = Shell.cmd(command).to(output).exec();
            result.isSuccess = shellResult.isSuccess();
            result.output = String.join("\n", output);
            result.error = String.join("\n", shellResult.getErr());  // 直接从 Result 获取错误
            result.message = result.isSuccess ? "执行成功" : "执行失败";

        } catch (Exception e) {
            result.isSuccess = false;
            result.message = "执行异常: " + e.getMessage();
            result.error = e.getMessage();
        }

        Log.d(TAG, "执行命令: " +command + "\n输出: " + result.output + "\n错误: " + result.error);
        return result;
    }

    /**
     * 执行shell命令（同步或异步）
     */
    public static ShellResult execCommand(String command, boolean async) {
        if (async) {
            // 异步执行
            try {
                Shell.cmd(command).submit();
                ShellResult result = new ShellResult();
                result.isSuccess = true;
                result.message = "命令已提交执行";
                return result;
            } catch (Exception e) {
                Log.e(TAG, "异步执行失败", e);
                return new ShellResult(false, "异步执行异常: " + e.getMessage());
            }
        } else {
            return execCommand(command);
        }
    }

    /**
     * 执行脚本（从String内容）
     * @param scriptContent 脚本内容
     * @return 执行结果
     */
    public static ShellResult execScriptContent(String scriptContent) {
        try {
            Shell.Result result = Shell.cmd(scriptContent.split("\n")).exec();
            return new ShellResult(result.isSuccess(),
                    String.join("\n", result.getOut()),
                    String.join("\n", result.getErr()));
        } catch (Exception e) {
            Log.e(TAG, "执行脚本失败", e);
            return new ShellResult(false, "执行脚本异常: " + e.getMessage());
        }
    }

    /**
     * 执行脚本文件（从assets加载）
     */
    public static ShellResult execScriptFromAssets(Context context, String scriptPath) {
        try {
            InputStream inputStream = context.getAssets().open(scriptPath);
            Shell.Result result = Shell.cmd(inputStream).exec();

            return new ShellResult(result.isSuccess(),
                    String.join("\n", result.getOut()),
                    String.join("\n", result.getErr()));
        } catch (Exception e) {
            Log.e(TAG, "执行脚本失败", e);
            return new ShellResult(false, "执行脚本异常: " + e.getMessage());
        }
    }


    /**
     * 获取Root Shell实例（用于高级操作）
     */
    public static Shell getShell() {
        try {
            return Shell.getShell();
        } catch (Exception e) {
            Log.e(TAG, "获取Shell失败", e);
            return null;
        }
    }

    /**
     * 关闭Root Shell
     */
    public static void closeShell() {
        try {
            Shell shell = Shell.getCachedShell();
            if (shell != null) {
                shell.close();
                Log.d(TAG, "Shell已关闭");
            }
            isInitialized = false;
            isRootAvailable = false;
        } catch (Exception e) {
            Log.e(TAG, "关闭Shell失败", e);
        }
    }

    /**
     * 获取Root状态
     */
    public static boolean isRootAvailable() {
        return isRootAvailable;
    }

    /**
     * 执行结果封装类
     */
    public static class ShellResult {
        public boolean isSuccess;
        public String output = "";
        public String error = "";
        public String message = "";

        public ShellResult() {}

        public ShellResult(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }

        public ShellResult(boolean isSuccess, String output, String error) {
            this.isSuccess = isSuccess;
            this.output = output != null ? output : "";
            this.error = error != null ? error : "";
            this.message = isSuccess ? "执行成功" : "执行失败";
        }

        @Override
        public String toString() {
            return "ShellResult{" +
                    "isSuccess=" + isSuccess +
                    ", output='" + output + '\'' +
                    ", error='" + error + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}