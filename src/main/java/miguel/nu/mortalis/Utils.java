package miguel.nu.mortalis;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String formatDuration(int seconds) {
        // Forever
        if (seconds == -1) {
            return "Forever";
        }

        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        List<String> parts = new ArrayList<>();

        if (days > 0) {
            parts.add(days + " Day" + (days != 1 ? "s" : ""));
        }

        if (hours > 0) {
            parts.add(hours + " Hour" + (hours != 1 ? "s" : ""));
        }

        if (minutes > 0) {
            parts.add(minutes + " Minute" + (minutes != 1 ? "s" : ""));
        }

        if (secs > 0) {
            parts.add(secs + " Second" + (secs != 1 ? "s" : ""));
        }

        // Zero duration
        if (parts.isEmpty()) {
            return "0 Seconds";
        }

        return String.join(", ", parts);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    // convert &#RRGGBB + & codes to §-based colors
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) return "";
        // Hex colors: &#RRGGBB -> §x§R§R§G§G§B§B
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // & -> §
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
