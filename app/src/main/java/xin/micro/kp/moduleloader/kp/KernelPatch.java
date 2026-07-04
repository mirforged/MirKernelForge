package xin.micro.kp.moduleloader.kp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import xin.micro.kp.moduleloader.root.RootShellUtil;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class KernelPatch {
    private static final KernelPatch instance = new KernelPatch();

    KernelPatch() {

    }

    public static KernelPatch getInstance() {
        return instance;
    }

    // =============================================================================================
    // info
    // =============================================================================================
    private MagicUtil.PatchInfo info;

    /**
     * include doGetPatchInformation
     * @return bool
     */
    public boolean refreshStatusFull(Context context) {
        //1.get boot
        RootShellUtil.ShellResult result = RootShellUtil.execScriptFromAssets(context, "scripts/test.sh");
        //result.output

        //2.get info from kernel
        doGetPatchInformation();
        return true;
    }

    public boolean doGetPatchInformation() {
        info = MagicUtil.getPatchInformation();
        return isNormal();
    }

    public MagicUtil.PatchInfo getPatchInformation() {
        return info;
    }

    public boolean isPatched() {
        return info.isPatched();
    }

    public boolean isNormal() {
        return (info != null);
    }

    // =============================================================================================
    // kpm
    // =============================================================================================
    private final List<KPMItem> moduleList = new ArrayList<>(); //将被加入修补的模块

    public List<KPMItem> getModuleList() {
        return moduleList;
    }

    public int refreshKpmList(Context context) {
        //files/kpm/xxx.kpm
        File kpmDir = new File(context.getFilesDir(), "kpm");
        moduleList.clear();
        int count = 0;
        if (kpmDir.exists() && kpmDir.isDirectory()) {
            File[] files = kpmDir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".kpm")) {
                        KPMItem kpmItem = new KPMItem(file.getAbsolutePath());
                        kpmItem.complete();
                        moduleList.add(kpmItem);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean preAddKpm(Context context, File kpm) {
        File filesDir = context.getFilesDir();
        File kpmDir = new File(filesDir, "kpm");
        if (!kpmDir.exists()) {
            kpmDir.mkdirs();
        }
        if (!kpmDir.isDirectory()) {
            kpmDir.delete();
            kpmDir.mkdirs();
        }
        String name;
        {
            KPMItem kpmItem = new KPMItem(kpm.toPath().toString());
            kpmItem.complete();
            name = kpmItem.name + ".kpm";
        }

        File targetFile = new File(kpmDir, name);
        try {
            Files.copy(
                    kpm.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removePreAddKpm(Context context, File kpm) {
        File filesDir = context.getFilesDir();
        File kpmDir = new File(filesDir, "kpm");
        try {
            Files.copy(
                    kpm.toPath(),
                    kpmDir.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // =============================================================================================
    // kernel patch
    // =============================================================================================

    /**
     *
     * @return output msg
     */
    public String doPatchAndPackBootImg() {
        StringBuilder msg = new StringBuilder();
        StringBuilder kpmArg = new StringBuilder();
        for (KPMItem item : moduleList) {
            kpmArg.append(" -M " + item.path + " -V pre-kernel-init -T kpm");
        }
        msg.append(MagicUtil.patchKernel(kpmArg.toString()));
        if (msg.toString().contains("patch done")) {
            msg.append("\nPatch Boot\n");
            msg.append(MagicUtil.packBootImg());
            return msg.toString();
        } else {//成功了才写boot.img
            return msg.toString();
        }
    }

    //将文件刷入到对应槽位
    public String flashBootSlot() {
        MagicUtil.installPatchedBoot();
        return null;
    }
}
