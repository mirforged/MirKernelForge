package xin.micro.kp.moduleloader.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import xin.micro.kp.moduleloader.root.RootShellUtil;

public class MagicUtil {
    public static void releaseFile(Context context){
        // 释放文件
        try {
            File filesDir = context.getFilesDir();

            //testgetboot.sh
            AssetsUtil.releaseAsset(context, "scripts/testgetboot.sh");
            File destFile = new File(filesDir, "scripts/testgetboot.sh"); // 注意 释放的时候会保持目录
            RootShellUtil.execCommand("cp " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/testgetboot.sh");

            //util_functions.sh
            AssetsUtil.releaseAsset(context, "scripts/util_functions.sh");
            destFile = new File(filesDir, "scripts/util_functions.sh");
            RootShellUtil.execCommand("cp " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/util_functions.sh");

            //kptools-android
            AssetsUtil.releaseAsset(context, "kptools-android");
            destFile = new File(filesDir, "kptools-android");
            RootShellUtil.execCommand("cp " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
            RootShellUtil.execCommand("chmod +x /data/adb/mirkforged/kptools-android");

            //kpimg
            AssetsUtil.releaseAsset(context, "kpimg");
            destFile = new File(filesDir, "kpimg");
            RootShellUtil.execCommand("cp " + destFile.getAbsolutePath() + " /data/adb/mirkforged/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static record PatchInfo(String raw,String banner,boolean isPatched){

    }
    public static PatchInfo getPatchInformation(){
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools-android --image kernel --list");

        Log.d("MagicUtil", "getPatchInformation: " + result.output);
        INIUtil.INIResult ini = INIUtil.parse(result.output);
        return new PatchInfo(result.output ,
                ini.get("kernel","banner"),
                ini.get("kernel","patched").equals("true")
        );

    }
    public static String patchKernel(String[] additionArgs){
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools-android --image kernel --patch -k kpimg -o kernel");
        return result.output+"\nError(if so, it will show here):\n"+result.error;
    }
    public static String packBootImg(){
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./kptools repack \"boot.img\"");
        return result.output+"\nError(if so, it will show here):\n"+result.error;
    }
    public static String installPatchedKernel(){
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && dd if=/data/adb/mirkforged/kernel.img of=/dev/block/by-name/boot");
        return "Please reboot";
    }
}
