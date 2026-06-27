package xin.micro.kp.moduleloader.util;

import android.content.Context;

import xin.micro.kp.moduleloader.root.RootShellUtil;

public class KernelPatch {
    private static final KernelPatch instance = new KernelPatch();
    KernelPatch() {

    }
    public static KernelPatch getInstance() {
        return instance;
    }

    private MagicUtil.PatchInfo info;
    public boolean refreshStatusFull(Context context){
        //1.get boot
        RootShellUtil.ShellResult result = RootShellUtil.execScriptFromAssets(context,"scripts/test.sh");
        //result.output

        //2.get info from kernel
        doGetPatchInformation();
        return true;
    }
    public boolean doGetPatchInformation(){
        info = MagicUtil.getPatchInformation();
        return true;
    }
    public MagicUtil.PatchInfo getPatchInformation(){
        return info;
    }
    public boolean isPatched(){
        return info.isPatched();
    }
    public boolean isNormal(){
        return (info != null);
    }
}
