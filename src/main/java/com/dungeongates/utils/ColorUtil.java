package com.dungeongates.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class ColorUtil {
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    
    private ColorUtil() {}
    
    public static String colorize(String message) {
        if (message == null) return "";
        return LEGACY_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(message));
    }
    
    public static Component parseComponent(String message) {
        if (message == null) return Component.empty();
        return MINI_MESSAGE.deserialize(message.replace("&", "§"));
    }
    
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        sender.sendMessage(parseComponent(message));
    }
    
    public static void sendMessages(CommandSender sender, Iterable<String> messages) {
        if (sender == null || messages == null) return;
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }
}