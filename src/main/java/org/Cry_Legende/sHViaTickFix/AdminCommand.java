package org.Cry_Legende.sHViaTickFix;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shviatickfix: status and reload.
 */
final class AdminCommand implements CommandExecutor, TabCompleter {

    private final SHViaTickFix plugin;

    AdminCommand(SHViaTickFix plugin) {
        this.plugin = plugin;
    }

    /** Subcommands the sender may use. */
    private List<String> allowed(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        if (sender.hasPermission("shviatickfix.admin.status")) {
            allowed.add("status");
        }
        if (sender.hasPermission("shviatickfix.admin.reload")) {
            allowed.add("reload");
        }
        return allowed;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> allowed = allowed(sender);
        if (allowed.isEmpty()) {
            plugin.send(sender, plugin.message("no-permission"));
            return true;
        }
        if (args.length == 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload") && allowed.contains(sub)) {
                plugin.reloadFor(sender);
                return true;
            }
            if (sub.equals("status") && allowed.contains(sub)) {
                status(sender);
                return true;
            }
        }
        plugin.send(sender, plugin.message("usage-admin").replace("%subs%", String.join("&7|&e", allowed)));
        return true;
    }

    private void status(CommandSender sender) {
        TickEndHook hook = plugin.hook();
        if (hook == null) {
            plugin.send(sender, plugin.message("not-installed"));
            return;
        }
        plugin.send(sender, plugin.message("status")
            .replace("%state%", plugin.message(plugin.fixEnabled() ? "state-on" : "state-off"))
            .replace("%count%", String.valueOf(hook.added()))
            .replace("%connections%", String.valueOf(hook.connections())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String start = args[0].toLowerCase(Locale.ROOT);
        return allowed(sender).stream().filter(sub -> sub.startsWith(start)).toList();
    }
}
