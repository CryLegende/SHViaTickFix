package org.Cry_Legende.sHViaTickFix;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class SHViaTickFix extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private YamlConfiguration messages;
    private TickEndHook hook;
    private volatile boolean fixEnabled;
    private volatile boolean logFixes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        applyConfig();

        try {
            TickEndHook created = new TickEndHook(this);
            created.install();
            hook = created;
            getLogger().info("Hooked into ViaBackwards for 1.21.2-1.21.3 clients.");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Could not hook into ViaBackwards, 1.21.2-1.21.3 clients are not covered", t);
        }

        AdminCommand admin = new AdminCommand(this);
        PluginCommand command = getCommand("shviatickfix");
        if (command != null) {
            command.setExecutor(admin);
            command.setTabCompleter(admin);
        }
    }

    @Override
    public void onDisable() {
        // Via has no way to remove a handler: an unloaded plugin's handlers stay registered but do nothing
        if (hook != null) {
            hook.deactivate();
        }
    }

    boolean fixEnabled() {
        return fixEnabled;
    }

    TickEndHook hook() {
        return hook;
    }

    void logFix(String username) {
        if (logFixes) {
            getLogger().info("Added a missing client tick end for " + (username == null ? "?" : username)
                + " (1.21.2-1.21.3 client, two positions in one tick).");
        }
    }

    void reloadFor(CommandSender sender) {
        if (!sender.hasPermission("shviatickfix.admin.reload")) {
            send(sender, message("no-permission"));
            return;
        }
        File configFile = new File(getDataFolder(), "config.yml");
        try {
            new YamlConfiguration().load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            send(sender, message("reload-failed").replace("%file%", configFile.getName()).replace("%error%", firstLine(e)));
            return;
        }
        YamlConfiguration newMessages;
        try {
            newMessages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            send(sender, message("reload-failed").replace("%file%", "messages.yml").replace("%error%", firstLine(e)));
            return;
        }
        reloadConfig();
        messages = newMessages;
        applyConfig();
        send(sender, message("reloaded"));
    }

    private void applyConfig() {
        fixEnabled = getConfig().getBoolean("enabled", true);
        logFixes = getConfig().getBoolean("log", true);
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        try {
            messages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("messages.yml is unreadable, using the built-in texts: " + firstLine(e));
            messages = defaultMessages();
        }
    }

    private YamlConfiguration defaultMessages() {
        try (InputStream in = getResource("messages.yml")) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            getLogger().warning("Could not read the default messages.yml: " + e.getMessage());
        }
        return new YamlConfiguration();
    }

    // Keys added in a later version fall back to the jar's text until they are copied into the file
    private YamlConfiguration readMessages() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File(getDataFolder(), "messages.yml"));
        yaml.setDefaults(defaultMessages());
        return yaml;
    }

    private static String firstLine(Exception e) {
        String text = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int end = text.indexOf('\n');
        return (end < 0 ? text : text.substring(0, end)).trim();
    }

    String message(String path) {
        // The one-argument getString also reads the defaults taken from the jar
        String text = messages.getString(path);
        return text != null ? text : "&cMessage manquant dans messages.yml : " + path;
    }

    // An empty message in messages.yml sends nothing
    void send(CommandSender sender, String text) {
        if (text != null && !text.isBlank()) {
            sender.sendMessage(LEGACY.deserialize(text));
        }
    }
}
