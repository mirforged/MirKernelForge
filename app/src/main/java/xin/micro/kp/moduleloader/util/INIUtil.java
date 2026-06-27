package xin.micro.kp.moduleloader.util;

import java.util.*;
import java.util.regex.Pattern;

public class INIUtil {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^=]+)=(.*)$");

    public static class INIResult {
        private final Map<String, Map<String, String>> sections = new HashMap<>();

        public void put(String section, String key, String value) {
            sections.computeIfAbsent(section, k -> new HashMap<>())
                    .put(key, value);
        }

        public String get(String section, String key) {
            Map<String, String> map = sections.get(section);
            return map != null ? map.get(key) : null;
        }

        public Map<String, String> getSection(String section) {
            return sections.get(section);
        }

        public Set<String> getSections() {
            return sections.keySet();
        }

        public Map<String, Map<String, String>> getAll() {
            return sections;
        }
    }

    public static INIResult parse(String iniString) {
        INIResult result = new INIResult();
        String currentSection = "";

        for (String line : iniString.split("\\r?\\n")) {
            line = line.trim();

            // 跳过空行、注释
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                continue;
            }

            // 检查是否 section
            var sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                currentSection = sectionMatcher.group(1).trim();
                continue;
            }

            // 检查是否 key=value
            var kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches()) {
                String key = kvMatcher.group(1).trim();
                String value = kvMatcher.group(2).trim();
                result.put(currentSection, key, value);
            }
        }

        return result;
    }

    // 使用示例
//    public static void main(String[] args) {
//        String iniString = """
//            [server]
//            host=127.0.0.1
//            port=8080
//
//            [database]
//            url=jdbc:mysql://localhost:3306/test
//            username=root
//            password=123456
//            max_connections=100
//            """;
//
//        INIResult result = INIUtils.parse(iniString);
//
//        // 获取单个值
//        String host = result.get("server", "host");
//        String port = result.get("server", "port");
//
//        // 获取整个 section
//        Map<String, String> dbConfig = result.getSection("database");
//
//        System.out.println("Server: " + host + ":" + port);
//        System.out.println("Database URL: " + dbConfig.get("url"));
//    }
}