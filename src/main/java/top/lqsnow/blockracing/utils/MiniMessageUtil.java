package top.lqsnow.blockracing.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility helpers for converting legacy color codes into MiniMessage strings and back.
 */
public final class MiniMessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .strict(false)
            .build();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_SIMPLE_PATTERN = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    private static final Map<Character, String> LEGACY_TO_MINI = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private MiniMessageUtil() {
    }

    public static Component deserialize(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String prepared = convertLegacyCodes(input);
        return MINI_MESSAGE.deserialize(prepared);
    }

    public static String toLegacyString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(deserialize(input));
    }

    public static String toLegacyString(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(component);
    }

    private static String convertLegacyCodes(String input) {
        String withHex = replaceHexCodes(input);
        return replaceSimpleCodes(withHex);
    }

    private static String replaceHexCodes(String input) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hex + ">");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceSimpleCodes(String input) {
        Matcher matcher = LEGACY_SIMPLE_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char original = matcher.group(1).charAt(0);
            char code = Character.toLowerCase(original);
            String replacement = LEGACY_TO_MINI.get(code);
            if (replacement == null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement("&" + original));
                continue;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
