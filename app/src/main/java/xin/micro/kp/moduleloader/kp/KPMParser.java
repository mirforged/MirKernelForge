package xin.micro.kp.moduleloader.kp;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class KPMParser {

    private static final String TAG = "KPMParser";

    private static final byte ELF_MAGIC0 = 0x7F;
    private static final byte ELF_MAGIC1 = 'E';
    private static final byte ELF_MAGIC2 = 'L';
    private static final byte ELF_MAGIC3 = 'F';

    public static Map<String, String> extractKpmInfo(String elfPath) {
        Log.d(TAG, "开始解析ELF文件: " + elfPath);
        Map<String, String> result = new HashMap<>();

        File elfFile = new File(elfPath);
        if (!elfFile.exists()) {
            Log.e(TAG, "ELF文件不存在: " + elfPath);
            return result;
        }
        Log.d(TAG, "ELF文件大小: " + elfFile.length() + " bytes");

        try (FileInputStream fis = new FileInputStream(elfPath)) {
            byte[] data = new byte[(int) elfFile.length()];
            fis.read(data);

            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // 检查文件是否足够大
            if (data.length < 64) {
                Log.e(TAG, "ELF文件太小");
                return result;
            }

            // 检查ELF magic
            if (!isElfFile(buffer)) {
                Log.e(TAG, "不是有效的ELF文件");
                return result;
            }

            // 验证是64位
            int ei_class = buffer.get(4) & 0xFF;
            if (ei_class != 2) {
                Log.e(TAG, "不是64位ELF文件，class=" + ei_class);
                return result;
            }
            Log.d(TAG, "64位ELF文件验证通过");

            // 解析64位ELF Section Headers
            // e_shoff: 偏移0x28 (40)
            long e_shoff = buffer.getLong(0x28);
            int e_shentsize = buffer.getShort(0x3A);
            int e_shnum = buffer.getShort(0x3C);
            int e_shstrndx = buffer.getShort(0x3E);

            Log.d(TAG, String.format("Section Header: offset=0x%x, entry_size=%d, count=%d, strtab_idx=%d",
                    e_shoff, e_shentsize, e_shnum, e_shstrndx));

            if (e_shoff == 0 || e_shnum == 0) {
                Log.e(TAG, "没有Section Header信息");
                return result;
            }

            // 检查偏移是否在文件范围内
            if (e_shoff > data.length) {
                Log.e(TAG, "Section Header偏移超出文件范围");
                return result;
            }

            // 定位到Section Header Table
            buffer.position((int) e_shoff);

            // 遍历所有Section
            for (int i = 0; i < e_shnum; i++) {
                // 检查是否还有足够数据
                if (buffer.position() + 64 > data.length) {
                    Log.e(TAG, "Section数据不完整");
                    break;
                }

                int sh_name = buffer.getInt();
                int sh_type = buffer.getInt();
                long sh_flags = buffer.getLong();
                long sh_addr = buffer.getLong();
                long sh_offset = buffer.getLong();
                long sh_size = buffer.getLong();
                int sh_link = buffer.getInt();
                int sh_info = buffer.getInt();
                long sh_addralign = buffer.getLong();
                long sh_entsize = buffer.getLong();

                // 获取Section名称
                String sectionName = getSectionName(buffer, data, e_shstrndx, sh_name);

                if (sectionName != null && sectionName.startsWith(".kpm.info")) {
                    Log.d(TAG, "找到KPM Section: " + sectionName);
                    Log.d(TAG, String.format("  offset=0x%x, size=%d", sh_offset, sh_size));

                    // 解析KPM信息
                    parseKpmData(data, (int) sh_offset, (int) sh_size, result);
                    break;
                }
            }

            if (result.isEmpty()) {
                Log.w(TAG, "未找到KPM信息，尝试搜索数据段");
                // 如果没找到，尝试直接搜索
                searchKpmInData(data, result);
            }

        } catch (Exception e) {
            Log.e(TAG, "解析ELF文件失败: " + e.getMessage(), e);
        }

        Log.d(TAG, "解析完成，提取到 " + result.size() + " 个KPM信息: " + result);
        return result;
    }

    /**
     * 检查是否是ELF文件
     */
    private static boolean isElfFile(ByteBuffer buffer) {
        return buffer.get(0) == ELF_MAGIC0 &&
                buffer.get(1) == ELF_MAGIC1 &&
                buffer.get(2) == ELF_MAGIC2 &&
                buffer.get(3) == ELF_MAGIC3;
    }

    /**
     * 获取Section名称
     */
    private static String getSectionName(ByteBuffer buffer, byte[] data, int strtabIndex, int nameOffset) {
        try {
            // 获取Section Header Table信息
            long e_shoff = buffer.getLong(0x28);
            int e_shentsize = buffer.getShort(0x3A);

            // 定位到String Table Section Header
            long strtabPos = e_shoff + strtabIndex * e_shentsize;
            if (strtabPos + 64 > data.length) {
                return null;
            }

            buffer.position((int) strtabPos);

            // 读取String Table的offset和size
            buffer.position(buffer.position() + 4);  // sh_name
            buffer.position(buffer.position() + 4);  // sh_type
            buffer.position(buffer.position() + 8);  // sh_flags
            buffer.position(buffer.position() + 8);  // sh_addr
            long strtabOffset = buffer.getLong();    // sh_offset
            long strtabSize = buffer.getLong();      // sh_size

            // 从String Table中读取名称
            if (nameOffset >= 0 && nameOffset < strtabSize) {
                int offset = (int) (strtabOffset + nameOffset);
                if (offset >= data.length) {
                    return null;
                }
                int end = offset;
                while (end < data.length && data[end] != 0) {
                    end++;
                }
                if (end > offset) {
                    return new String(data, offset, end - offset, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取Section名称失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 解析KPM数据
     */
    private static void parseKpmData(byte[] data, int offset, int size, Map<String, String> result) {
        Log.d(TAG, "解析KPM数据: offset=0x" + Integer.toHexString(offset) + ", size=" + size);

        if (offset >= data.length) {
            Log.e(TAG, "偏移越界");
            return;
        }

        int actualSize = Math.min(size, data.length - offset);
        if (actualSize <= 0) {
            Log.e(TAG, "数据大小为0");
            return;
        }

        // 提取数据
        byte[] sectionData = new byte[actualSize];
        System.arraycopy(data, offset, sectionData, 0, actualSize);

        String content = new String(sectionData, StandardCharsets.UTF_8);
        Log.d(TAG, "KPM Section内容:\n" + content);

        // 按行解析
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int eqIndex = line.indexOf('=');
            if (eqIndex > 0) {
                String key = line.substring(0, eqIndex).trim();
                String value = line.substring(eqIndex + 1).trim();

                // 移除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                    result.put(key, value);
                    Log.d(TAG, "解析到: " + key + "=" + value);
            }
        }
    }

    /**
     * 在数据中直接搜索KPM信息 (不推荐)
     */
    private static void searchKpmInData(byte[] data, Map<String, String> result) {
        Log.d(TAG, "在数据中搜索KPM信息");

        // 搜索 "name=", "version=" 等字符串
        String[] keys = {"name=", "version=", "license=", "author=", "description=", "args="};

        for (String key : keys) {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            int start = findBytes(data, keyBytes);
            if (start != -1) {
                Log.d(TAG, "找到key: " + key + " at offset 0x" + Integer.toHexString(start));
                // 提取值（到下一个换行或null）
                int valueStart = start + keyBytes.length;
                int end = valueStart;
                while (end < data.length && data[end] != '\n' && data[end] != 0) {
                    end++;
                }
                if (end > valueStart) {
                    String value = new String(data, valueStart, end - valueStart, StandardCharsets.UTF_8).trim();
                    String standardKey = key.replace("=", "");
                    result.put(standardKey, value);
                    Log.d(TAG, "搜索到: " + standardKey + "=" + value);
                }
            }
        }
    }

    /**
     * 查找字节数组
     */
    private static int findBytes(byte[] data, byte[] pattern) {
        for (int i = 0; i < data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

}