package xin.micro.kp.moduleloader.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import xin.micro.kp.moduleloader.root.RootShellUtil;

public class MagicUtil {

    // /data/adb/mirkforged - Workspace
    // /app-data-files/ - Workspace2
    // ws2/kpm/ - kpm which will be add

    /**
     * 这tm就是一坨史
     */
    public static void releaseFile(Context context) {
        // 释放文件
        try {
            File filesDir = context.getFilesDir();

            //getboot.sh
            AssetsUtil.releaseAsset(context, "scripts/getboot.sh");
            File destFile = new File(filesDir, "scripts/getboot.sh"); // 注意 释放的时候会保持目录
            RootShellUtil.execCommand("cp -u " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/getboot.sh");

            //util_functions.sh
            AssetsUtil.releaseAsset(context, "scripts/util_functions.sh");
            destFile = new File(filesDir, "scripts/util_functions.sh");
            RootShellUtil.execCommand("cp -u " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/util_functions.sh");

            //kptools-android
            AssetsUtil.releaseAsset(context, "kptools-android");
            destFile = new File(filesDir, "kptools-android");
            RootShellUtil.execCommand("cp -u " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/kptools-android");

            //instant_kern_api usercall
            AssetsUtil.releaseAsset(context, "usercall");
            destFile = new File(filesDir, "usercall");
            RootShellUtil.execCommand("cp -u " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/usercall");

            //kpimg
            AssetsUtil.releaseAsset(context, "kpimg");
            destFile = new File(filesDir, "kpimg");
            RootShellUtil.execCommand("cp " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cleanWorkSpace() {
        RootShellUtil.execCommand("rm -rf /data/adb/mirkforged/*");
    }

    public static record PatchInfo(String raw, String banner, boolean isPatched) {
    }

    public static PatchInfo getPatchInformation() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools-android --image kernel --list");

        Log.d("MagicUtil", "getPatchInformation: " + result.output);
        INIUtil.INIResult ini = INIUtil.parse(result.output);
        if (ini.get("kernel", "banner") == null) {
            return null;
        }
        return new PatchInfo(result.output,
                ini.get("kernel", "banner"),
                ini.get("kernel", "patched").equals("true")
        );

    }

    public static String patchKernel(String additionArgs) {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools-android --image kernel --patch -k kpimg -o kernel" + additionArgs);
        return result.output + "\nError(if so, it will show here):\n" + result.error;
    }

    /**
     * 会将kernel写入到boot.img
     */
    public static String packBootImg() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools-android repack boot.img");
        return result.output + "\nError(if so, it will show here):\n" + result.error;
    }

    /**
     * 将/data/adb/mirkforged/boot.img写入到当前槽位
     */
    public static String installPatchedBoot() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("dd if=/data/adb/mirkforged/new-boot.img of=" + getCurrBootSlotPath());
        return "Please reboot";
    }


    public static String getCurrBootSha256() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("sha256sum -b " + getCurrBootSlotPath());
        return result.output;
    }

    public static String getCurrBootSlotPath() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("ls /dev/block/by-name/boot$(getprop ro.boot.slot_suffix) 2>/dev/null || echo /dev/block/by-name/boot");
        return result.output;
    }

}
