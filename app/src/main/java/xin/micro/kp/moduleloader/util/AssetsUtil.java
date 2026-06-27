package xin.micro.kp.moduleloader.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AssetsUtil {
//    releaseAssetToPrivateDir(context, "config.json")
    // 最终路径： /data/data/your.package/files/config.json
public static void releaseAsset(Context context, String assetPath) throws IOException {
    File filesDir = context.getFilesDir();
    File destFile = new File(filesDir, assetPath);

    // 1. 创建父目录
    File parentDir = destFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
        boolean created = parentDir.mkdirs();
        if (!created) {
            throw new IOException("无法创建目录: " + parentDir.getAbsolutePath()
                    + "，请检查磁盘空间或权限");
        }
    }

    // 2. 如果目标文件已存在，检查是否与 assets 文件相同
    if (destFile.exists()) {
        try (InputStream assetStream = context.getAssets().open(assetPath)) {
            if (isSameContent(assetStream, destFile)) {
                // 文件内容相同，无需操作
                return;
            }
        }
        // 内容不同，后续会覆盖写入
    }

    // 3. 执行复制（覆盖写入）
    try (InputStream is = context.getAssets().open(assetPath);
         FileOutputStream fos = new FileOutputStream(destFile, false)) { // false = 覆盖
        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }
        fos.flush();
    }
}

    /**
     * 比较两个文件内容是否相同（通过 MD5）
     */
    private static boolean isSameContent(InputStream inputStream, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            String md5Asset = getMD5(inputStream);
            String md5File = getMD5(fis);
            return md5Asset.equals(md5File);
        }
    }

    /**
     * 计算 InputStream 的 MD5 值
     */
    private static String getMD5(InputStream inputStream) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            byte[] digest = md.digest();

            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 算法不可用", e);
        }
    }
}
