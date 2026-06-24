package ru.rusplugins.bargrounds;

import cn.nukkit.Player;
import cn.nukkit.network.protocol.RemoveObjectivePacket;
import cn.nukkit.network.protocol.SetDisplayObjectivePacket;
import cn.nukkit.network.protocol.SetScorePacket;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SidebarService {

    private static final String OBJECTIVE_NAME = "bargrounds";

    private final Map<UUID, List<Long>> activeIds = new HashMap<UUID, List<Long>>();
    private final BarGroundsPlugin plugin;

    public SidebarService(BarGroundsPlugin plugin) {
        this.plugin = plugin;
    }

    public void show(Player player) {
        SetDisplayObjectivePacket packet = new SetDisplayObjectivePacket();
        setEnumField(packet, "displaySlot", "SIDEBAR");
        setStringField(packet, new String[]{"objectiveName", "objectiveId"}, OBJECTIVE_NAME);
        setStringField(packet, new String[]{"displayName"}, formatTitle(player));
        setStringField(packet, new String[]{"criteriaName", "criteria"}, "dummy");
        setEnumField(packet, "sortOrder", "DESCENDING");
        player.dataPacket(packet);
    }

    public void hide(Player player) {
        RemoveObjectivePacket packet = new RemoveObjectivePacket();
        setStringField(packet, new String[]{"objectiveName", "objectiveId"}, OBJECTIVE_NAME);
        player.dataPacket(packet);
        activeIds.remove(player.getUniqueId());
    }

    public void update(Player player) {
        show(player);
        clearLines(player);

        List<String> lines = buildLines(player);
        List<SetScorePacket.ScoreInfo> infos = new ArrayList<SetScorePacket.ScoreInfo>();
        List<Long> ids = new ArrayList<Long>();

        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            long id = 1000L + i;
            ids.add(id);
            infos.add(new SetScorePacket.ScoreInfo(id, OBJECTIVE_NAME, score--, lines.get(i)));
        }

        SetScorePacket setPacket = new SetScorePacket();
        setPacket.action = SetScorePacket.Action.SET;
        setPacket.infos.clear();
        setPacket.infos.addAll(infos);
        player.dataPacket(setPacket);

        activeIds.put(player.getUniqueId(), ids);
    }

    private void clearLines(Player player) {
        List<Long> ids = activeIds.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SetScorePacket.ScoreInfo> infos = new ArrayList<SetScorePacket.ScoreInfo>();
        for (Long id : ids) {
            infos.add(new SetScorePacket.ScoreInfo(id, OBJECTIVE_NAME, 0, ""));
        }

        SetScorePacket removePacket = new SetScorePacket();
        removePacket.action = SetScorePacket.Action.REMOVE;
        removePacket.infos.clear();
        removePacket.infos.addAll(infos);
        player.dataPacket(removePacket);
    }

    private List<String> buildLines(Player player) {
        PluginHooks hooks = plugin.getHooks();
        PlayerDataStore store = plugin.getDataStore();
        PanelTexts texts = getTexts(plugin.getPanelLanguage());
        PluginHooks.HookResult rank = hooks.readRank(player);
        PluginHooks.HookResult clan = hooks.readClan(player);
        PluginHooks.HookResult coins = hooks.readCoins(player);
        PluginHooks.HookResult rubies = hooks.readRubies();
        PluginHooks.HookResult donate = hooks.readDonateCoins();

        String dateLine = plugin.getConfig().getBoolean("center-date", true) ? centerText(now(), getMaxLineLength()) : now();

        List<String> lines = new ArrayList<String>();
        lines.add("&7" + dateLine);
        lines.add(uniqueSpacer(1));
        lines.add(texts.nick + ": &a" + player.getName());
        lines.add(texts.rank + ": " + formatHookValue(rank, texts));
        lines.add(texts.clan + ": " + formatHookValue(clan, texts));
        lines.add(uniqueSpacer(2));
        lines.add(texts.coins + ": " + formatHookValue(coins, texts));
        lines.add(texts.rubies + ": " + formatHookValue(rubies, texts));
        lines.add(texts.donate + ": " + formatHookValue(donate, texts));
        lines.add(uniqueSpacer(3));
        lines.add(texts.kills + ": &c" + store.getKills(player));
        lines.add(texts.deaths + ": &c" + store.getDeaths(player));
        lines.add("&b" + plugin.getConfig().getString("footer-link", "github.com/RUSPlugins-Team"));

        List<String> out = new ArrayList<String>();
        for (String line : lines) {
            out.add(trim(colorize(line)));
        }

        return out;
    }

    private String now() {
        String pattern = plugin.getConfig().getString("date-format", "dd.MM.yyyy HH:mm");
        return new SimpleDateFormat(pattern, Locale.US).format(new Date());
    }

    private String trim(String value) {
        int maxLength = getMaxLineLength();
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        String cut = value.substring(0, maxLength);
        if (cut.endsWith("§")) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut;
    }

    private String formatTitle(Player player) {
        String template = getTexts(plugin.getPanelLanguage()).title;
        String title = template.replace("{PING}", String.valueOf(player.getPing()));
        return trim(colorize(title));
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }

    private String uniqueSpacer(int index) {
        String[] spacers = new String[]{"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
        return spacers[index % spacers.length];
    }

    private String formatHookValue(PluginHooks.HookResult result, PanelTexts texts) {
        if (result == null || !result.isAvailable()) {
            return "&c" + texts.missing;
        }
        if (!result.isSafe()) {
            return "&e" + texts.available;
        }

        String value = result.getValue();
        if (value == null || value.trim().isEmpty()) {
            return "&a" + texts.available;
        }

        if ("Available".equalsIgnoreCase(value)) {
            return "&a" + texts.available;
        }

        return "&a" + value;
    }

    private int getMaxLineLength() {
        int value = plugin.getConfig().getInt("max-line-length", 28);
        if (value < 16) {
            return 16;
        }
        if (value > 32) {
            return 32;
        }
        return value;
    }

    private String centerText(String text, int width) {
        String clear = stripColors(text);
        if (clear.length() >= width) {
            return text;
        }
        int spaces = (width - clear.length()) / 2;
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            prefix.append(' ');
        }
        return prefix.toString() + text;
    }

    private String stripColors(String text) {
        return text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    private PanelTexts getTexts(String language) {
        if ("ru".equals(language)) {
            return new PanelTexts("&aINFOSERVER &7{PING} ms", "Ник", "Ранг", "Клан", "Монеты", "Рубины", "Донат", "Убийства", "Смерти", "Плагин не найден", "Доступно");
        }
        if ("uk".equals(language)) {
            return new PanelTexts("&aINFOSERVER &7{PING} ms", "Нік", "Ранг", "Клан", "Монети", "Рубіни", "Донат", "Вбивства", "Смерті", "Плагін не знайдено", "Доступно");
        }
        if ("fr".equals(language)) {
            return new PanelTexts("&aINFOSERVER &7{PING} ms", "Pseudo", "Rang", "Clan", "Pieces", "Rubis", "Donat", "Kills", "Morts", "Plugin manquant", "Disponible");
        }
        if ("de".equals(language)) {
            return new PanelTexts("&aINFOSERVER &7{PING} ms", "Name", "Rang", "Clan", "Muenzen", "Rubine", "Spenden", "Kills", "Tode", "Plugin fehlt", "Verfuegbar");
        }
        return new PanelTexts("&aINFOSERVER &7{PING} ms", "Nick", "Rank", "Clan", "Coins", "Rubies", "Donate", "Kills", "Deaths", "Plugin Missing", "Available");
    }

    private static class PanelTexts {
        private final String title;
        private final String nick;
        private final String rank;
        private final String clan;
        private final String coins;
        private final String rubies;
        private final String donate;
        private final String kills;
        private final String deaths;
        private final String missing;
        private final String available;

        private PanelTexts(String title, String nick, String rank, String clan, String coins, String rubies, String donate, String kills, String deaths, String missing, String available) {
            this.title = title;
            this.nick = nick;
            this.rank = rank;
            this.clan = clan;
            this.coins = coins;
            this.rubies = rubies;
            this.donate = donate;
            this.kills = kills;
            this.deaths = deaths;
            this.missing = missing;
            this.available = available;
        }
    }

    private void setStringField(Object target, String[] names, String value) {
        for (String name : names) {
            try {
                Field field = target.getClass().getField(name);
                field.set(target, value);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setEnumField(Object target, String fieldName, String enumValue) {
        try {
            Field field = target.getClass().getField(fieldName);
            Class<?> type = field.getType();
            if (type.isEnum()) {
                field.set(target, Enum.valueOf((Class<? extends Enum>) type, enumValue));
            }
        } catch (Exception ignored) {
        }
    }
}