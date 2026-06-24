package ru.rusplugins.bargrounds;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;

public class BarGroundsPlugin extends PluginBase implements Listener {

    private PlayerDataStore dataStore;
    private PluginHooks hooks;
    private SidebarService sidebarService;
    private String panelLanguage = "en";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataStore = new PlayerDataStore(getDataFolder());
        hooks = new PluginHooks(this);
        sidebarService = new SidebarService(this);
        panelLanguage = resolvePanelLanguage();

        getServer().getPluginManager().registerEvents(this, this);
        startTask();
        logIntegrationState();
        getLogger().info("Panel language: " + panelLanguage);
        getLogger().info("BarGrounds enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers().values()) {
            sidebarService.hide(player);
        }
        dataStore.save();
        getLogger().info("BarGrounds disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        getServer().getScheduler().scheduleDelayedTask(this, new Task() {
            @Override
            public void onRun(int currentTick) {
                if (player.isOnline()) {
                    sidebarService.update(player);
                }
            }
        }, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sidebarService.hide(event.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Player victim = (Player) entity;
        dataStore.addDeath(victim);

        Entity killerEntity = victim.getKiller();
        if (killerEntity instanceof Player) {
            Player killer = (Player) killerEntity;
            dataStore.addKill(killer);
        }

        dataStore.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("bargrounds")) {
            return false;
        }

        if (!sender.hasPermission("bargrounds.command")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            panelLanguage = resolvePanelLanguage();
            logIntegrationState();
            sender.sendMessage("BarGrounds config reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /bargrounds reload");
        return true;
    }

    public PlayerDataStore getDataStore() {
        return dataStore;
    }

    public PluginHooks getHooks() {
        return hooks;
    }

    public String getPanelLanguage() {
        return panelLanguage;
    }

    private void startTask() {
        final int updateTicks = Math.max(20, getConfig().getInt("update-ticks", 20));
        getServer().getScheduler().scheduleRepeatingTask(this, new Task() {
            @Override
            public void onRun(int currentTick) {
                for (Player player : getServer().getOnlinePlayers().values()) {
                    sidebarService.update(player);
                }
            }
        }, updateTicks);
    }

    private void logIntegrationState() {
        for (String line : hooks.buildStartupReport()) {
            getLogger().info(line);
        }
    }

    private String resolvePanelLanguage() {
        String mode = getConfig().getString("language-mode", "auto").toLowerCase();
        if (!"auto".equals(mode)) {
            return normalizeLanguage(mode);
        }

        String serverLanguage = getServer().getPropertyString("language", "eng");
        return normalizeLanguage(serverLanguage);
    }

    private String normalizeLanguage(String source) {
        String value = source == null ? "eng" : source.toLowerCase();

        if (value.startsWith("ru") || value.startsWith("rus")) {
            return "ru";
        }
        if (value.startsWith("uk") || value.startsWith("ua")) {
            return "uk";
        }
        if (value.startsWith("fr")) {
            return "fr";
        }
        if (value.startsWith("de") || value.startsWith("ger")) {
            return "de";
        }
        return "en";
    }
}