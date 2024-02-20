package io.tebex.plugin.command.analytics.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.analytics.SubCommand;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.PlayerType;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatsCommand extends SubCommand {
    public StatsCommand(TebexPlugin platform) {
        super(platform, "stats", "analytics.stats");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Collection<AnalysePlayer> players = getPlatform().getPlayers().values();

        Stream<AnalysePlayer> javaStream = players.stream().filter(analysePlayer -> analysePlayer.getType() == PlayerType.JAVA);
        Stream<AnalysePlayer> bedrockStream = players.stream().filter(analysePlayer -> analysePlayer.getType() == PlayerType.BEDROCK);

        int javaCount = (int) javaStream.count();
        int bedrockCount = (int) bedrockStream.count();
        int totalCount = javaCount + bedrockCount;

        if (args.length == 0 || (args[0].equalsIgnoreCase("domain") || args[0].equalsIgnoreCase("domains"))) {
            long amountOfDomains = 5; // Range: 1, 25

            if (args.length >= 2)
                try { amountOfDomains = Math.max(1, Math.min(Long.parseLong(args[1]), 25));} catch (NumberFormatException ignored) {}

            // Show domain stats
            getPlatform().sendMessage(sender, "&7Top " + amountOfDomains + " Domains:");
            getPlatform().sendMessage(sender, "&7");

            // Group by domain and show counts
            Map<String, Map<PlayerType, Long>> domainCounts = players.stream()
                    .filter(p -> p.getDomain() != null)
                    .collect(Collectors.groupingBy(AnalysePlayer::getDomain, Collectors.groupingBy(AnalysePlayer::getType, Collectors.counting())));

            Map<String, Long> domainTotals = domainCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().values().stream().mapToLong(Long::longValue).sum()));

            Map<String, Map<PlayerType, Long>> topDomains = domainCounts.entrySet().stream()
                    .sorted(Comparator.comparingLong((Map.Entry<String, Map<PlayerType, Long>> e) -> e.getValue().getOrDefault(PlayerType.JAVA, 0L) + e.getValue().getOrDefault(PlayerType.BEDROCK, 0L)).reversed())
                    .limit(amountOfDomains)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

            if (topDomains.size() > 0) {
                for (Map.Entry<String, Map<PlayerType, Long>> entry : topDomains.entrySet()) {
                    String domain = entry.getKey();
                    Map<PlayerType, Long> counts = entry.getValue();
                    long domainTotal = domainTotals.get(domain);
                    long javaDomainCount = counts.getOrDefault(PlayerType.JAVA, 0L);
                    long bedrockDomainCount = counts.getOrDefault(PlayerType.BEDROCK, 0L);

                    // Create a tooltip with the domain stats using chatcomponents
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[]{
                            new TextComponent("§b" + domain + "\n"),

                            new TextComponent("\n§7⚡ §7Java: §f" + javaDomainCount),
                            new TextComponent("\n§7⚡ §7Bedrock: §f" + bedrockDomainCount),

                            new TextComponent("\n\n§7Click for more information"),
                    });

                    if (sender instanceof Player) {
                        TextComponent component = new TextComponent("§7⚡ §b" + domain + ": §f" + domainTotal + " §7online §8§o(Hover for details)");
                        component.setHoverEvent(hoverEvent);

                        ((Player) sender).spigot().sendMessage(component);
                    }
                }
            } else {
                getPlatform().sendMessage(sender, "&7⚡ &7No domains found.");
            }

            getPlatform().sendMessage(sender, "&7");
            getPlatform().sendMessage(sender, "&7A total of " + totalCount + " players online.");

        } else if (args[0].equalsIgnoreCase("country") || args[0].equalsIgnoreCase("countries")) {
            long amountOfCountries = 5; // Range: 1, 25

            if (args.length >= 2)
                try { amountOfCountries = Math.max(1, Math.min(Long.parseLong(args[1]), 25));} catch (NumberFormatException ignored) {}

            // Show country stats
            getPlatform().sendMessage(sender, "&7Top " + amountOfCountries + " Countries:");
            getPlatform().sendMessage(sender, "&7");

            // Group by country and show counts
            Map<String, Map<PlayerType, Long>> countryCounts = players.stream()
                    .filter(p -> p.getCountry() != null)
                    .collect(Collectors.groupingBy(AnalysePlayer::getCountry, Collectors.groupingBy(AnalysePlayer::getType, Collectors.counting())));

            Map<String, Long> countryTotals = countryCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().values().stream().mapToLong(Long::longValue).sum()));

            Map<String, Map<PlayerType, Long>> topCountries = countryCounts.entrySet().stream()
                    .sorted(Comparator.comparingLong((Map.Entry<String, Map<PlayerType, Long>> e) -> e.getValue().getOrDefault(PlayerType.JAVA, 0L) + e.getValue().getOrDefault(PlayerType.BEDROCK, 0L)).reversed())
                    .limit(amountOfCountries)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

            if (topCountries.size() > 0) {
                for (Map.Entry<String, Map<PlayerType, Long>> entry : topCountries.entrySet()) {
                    String country = entry.getKey();
                    Map<PlayerType, Long> counts = entry.getValue();
                    long countryTotal = countryTotals.get(country);
                    long javaCountryCount = counts.getOrDefault(PlayerType.JAVA, 0L);
                    long bedrockCountryCount = counts.getOrDefault(PlayerType.BEDROCK, 0L);

                    Locale locale = new Locale("", country);
                    String countryName = locale.getDisplayCountry();

                    // Create a tooltip with the country stats using chatcomponents
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[]{
                            new TextComponent("&b" + countryName + "\n"),

                            new TextComponent("\n&7⚡ &7Java: &f" + javaCountryCount),
                            new TextComponent("\n&7⚡ &7Bedrock: &f" + bedrockCountryCount),
                            new TextComponent("\n\n&7Click for more information"),
                    });

                    if (sender instanceof Player) {
                        TextComponent component = new TextComponent("&7⚡ &b" + countryName + ": &f" + countryTotal + " &7online &8&o(Hover for details)");
                        component.setHoverEvent(hoverEvent);

                        ((Player) sender).spigot().sendMessage(component);
                    }
                }
            } else {
                getPlatform().sendMessage(sender, "&7⚡ &7No countries found.");
            }

            getPlatform().sendMessage(sender, "&7");
            getPlatform().sendMessage(sender, "&7A total of " + totalCount + " players online.");

        } else {
            getPlatform().sendMessage(sender, "&cUsage: /analytics stats [domain|country]");
        }
    }
}