package com.dungeongates.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class MessageUtil {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    private MessageUtil() {}
    
    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
    
    public static List<String> replacePlaceholders(List<String> messages, Map<String, String> placeholders) {
        if (messages == null) return List.of();
        return messages.stream()
                .map(msg -> replacePlaceholders(msg, placeholders))
                .toList();
    }
    
    public static Component parse(String message, Map<String, String> placeholders) {
        return ColorUtil.parseComponent(replacePlaceholders(message, placeholders));
    }
    
    public static void send(Player player, String message, Map<String, String> placeholders) {
        if (player == null || message == null) return;
        player.sendMessage(parse(message, placeholders));
    }
    
    public static void send(Player player, List<String> messages, Map<String, String> placeholders) {
        if (player == null || messages == null) return;
        for (String msg : messages) {
            send(player, msg, placeholders);
        }
    }
    
    public static void sendPrefixed(Player player, String prefix, String message, Map<String, String> placeholders) {
        if (player == null) return;
        String fullMessage = (prefix != null ? prefix : "") + (message != null ? message : "");
        send(player, fullMessage, placeholders);
    }
    
    public static void sendPrefixed(Player player, String prefix, List<String> messages, Map<String, String> placeholders) {
        if (player == null || messages == null) return;
        for (String msg : messages) {
            sendPrefixed(player, prefix, msg, placeholders);
        }
    }
}