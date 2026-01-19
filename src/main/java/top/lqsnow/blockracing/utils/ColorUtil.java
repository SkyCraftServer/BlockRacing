package top.lqsnow.blockracing.utils;

public class ColorUtil {
    public static String t(String str) {
        if (str == null || str.isEmpty()) return str == null ? "" : str;
        // If string already contains legacy section char, assume it's already converted and return as-is
        if (str.indexOf('\u00A7') >= 0) return str;
        // Otherwise convert MiniMessage or legacy & codes to legacy-formatted string
        return MiniMessageUtil.toLegacyString(str);
    }
}
