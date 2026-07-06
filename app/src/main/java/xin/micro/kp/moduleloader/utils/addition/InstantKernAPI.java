package xin.micro.kp.moduleloader.utils.addition;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xin.micro.kp.moduleloader.root.RootShellUtil;
import xin.micro.kp.moduleloader.utils.INIUtil;

//依旧硬编码
//
public class InstantKernAPI {
    public record IKA_KPMItem(String name, String version,
                              String license, String description,
                              String author, String args) {
    }

    public static List<IKA_KPMItem> getLoadedKPM() {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./usercall -l -f");

        Gson gson = new Gson();
        JsonArray jsonArrayObj = gson.fromJson(result.output, JsonArray.class);

        List<IKA_KPMItem> KPMs = new ArrayList<IKA_KPMItem>();
        for (int i = 0; i < jsonArrayObj.size(); i++) {
            String kpmName = jsonArrayObj.get(i).getAsString();
            RootShellUtil.ShellResult result2 = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./usercall -f -i " + kpmName);
            JsonObject kpmInfo = gson.fromJson(result2.output, JsonObject.class);
            if (kpmInfo.get("status").getAsString().equals("success")) {
                String kpmInfoX = kpmInfo.get("info").getAsString();
                INIUtil.INIResult kpmInfoXX = INIUtil.parse("[placeholder]\n" + kpmInfoX);
                Map<String, String> kpmInfoXXX = kpmInfoXX.getSection("placeholder");


                String kpmVersion = kpmInfoXXX.get("version");
                String kpmLicense = kpmInfoXXX.get("license");
                String kpmDescription = kpmInfoXXX.get("description");
                String kpmAuthor = kpmInfoXXX.get("author");
                //String kpmArgs = kpmInfo.get("args").getAsString();//useless
                KPMs.add(new IKA_KPMItem(kpmName, kpmVersion, kpmLicense, kpmDescription, kpmAuthor, null));
            }
        }
        return KPMs;
    }

    public static int loadKPM(String path) {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./usercall -f -L " + path);
        return 0;
    }

    public static int unloadKPM(String name) {
        RootShellUtil.ShellResult result = RootShellUtil.execCommand("cd /data/adb/mirkforged/ && ./usercall -f -U " + name);
        return 0;
    }
}
