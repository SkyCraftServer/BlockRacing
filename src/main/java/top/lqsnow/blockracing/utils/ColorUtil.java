package top.lqsnow.blockracing.utils;

import java.util.regex.Pattern;

public class ColorUtil {
    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)&([0-9A-FK-ORX])");

    public static String t(String str) {
        if (str == null || str.isEmpty()) return str == null ? "" : str;
        // If string already contains legacy section char, assume it's already converted and return as-is
        if (str.indexOf('\u00A7') >= 0) return str;
        // Otherwise convert MiniMessage or legacy & codes to legacy-formatted string
        return MiniMessageUtil.toLegacyString(str);
    }
}
