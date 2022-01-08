package me.byteful.plugin.tmb;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.particle.ParticleBuilder;
import xyz.xenondevs.particle.ParticleEffect;

import java.awt.*;
import java.util.*;

public final class TakeMeBackPlugin extends JavaPlugin implements Listener {
  private final Map<UUID, PlayerTracker> trackers = new HashMap<>();

  @Override
  public void onEnable() {
    saveDefaultConfig();

    Bukkit.getPluginManager().registerEvents(this, this);

    Objects.requireNonNull(getCommand("takemeback"))
        .setExecutor(
            (sender, command, label, args) -> {
              if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cOnly players can use this command!"));

                return true;
              }

              if (!sender.hasPermission("takemeback.use")) {
                sender.sendMessage(colorize(getConfig().getString("messages.no_permission")));

                return true;
              }

              final Player player = (Player) sender;
              final UUID uuid = player.getUniqueId();

              if (trackers.containsKey(uuid)) {
                sender.sendMessage(colorize(getConfig().getString("messages.toggle_trail_off")));

                trackers.remove(uuid);
              } else {
                sender.sendMessage(colorize(getConfig().getString("messages.toggle_trail_on")));

                trackers.put(uuid, new PlayerTracker(this, player));
              }

              return true;
            });

    Bukkit.getScheduler()
        .scheduleSyncRepeatingTask(
            this, () -> trackers.values().forEach(PlayerTracker::tick), 0L, 20L);
  }

  @Override
  public void onDisable() {
    trackers.clear();
  }

  void spawnParticle(Player player, Location location) {
    final Set<? extends Player> players =
        new HashSet<>(
            getConfig().getBoolean("broadcast_particle", false)
                ? Bukkit.getOnlinePlayers()
                : Collections.singleton(player));
    players.removeIf(p -> !p.getWorld().equals(location.getWorld()));

    final String type =
        Objects.requireNonNull(
                getConfig().getString("particle.type", "REDSTONE"),
                "Failed to find 'type' in particle config.")
            .toUpperCase(Locale.ROOT)
            .replace(" ", "_")
            .trim();

    final ConfigurationSection data = getConfig().getConfigurationSection("particle.data");
    assert data != null : "Failed to find 'data' section in particle config.";

    final int amount = data.getInt("amount", 1);

    final ConfigurationSection color = data.getConfigurationSection("color");
    assert color != null : "Failed to find 'color' section in particle data config.";

    final int red = color.getInt("r", 255);
    final int green = color.getInt("g", 0);
    final int blue = color.getInt("b", 0);

    final ConfigurationSection offset = data.getConfigurationSection("offset");
    assert offset != null : "Failed to find 'offset' section in particle data config.";

    final int x = color.getInt("x", 0);
    final int y = color.getInt("y", 0);
    final int z = color.getInt("z", 0);

    final int speed = data.getInt("speed", 1);

    new ParticleBuilder(ParticleEffect.valueOf(type), location)
        .setAmount(amount)
        .setColor(new Color(red, green, blue))
        .setOffset(x, y, z)
        .setSpeed(speed)
        .display(players);
  }

  @EventHandler
  public void onLeave(PlayerJoinEvent event) {
    if (getConfig().getBoolean("stop_trail.on_leave", true)) {
      trackers.remove(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent event) {
    if (getConfig().getBoolean("stop_trail.on_death", true)) {
      trackers.remove(event.getEntity().getUniqueId());
    }
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    if (getConfig().getBoolean("stop_trail.on_world_change", true)) {
      trackers.remove(event.getPlayer().getUniqueId());
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    final Player player = event.getPlayer();
    final Location from = event.getFrom();
    final Location to = event.getTo();

    if (to == null
        || !trackers.containsKey(player.getUniqueId())
        || from.getBlock().equals(to.getBlock())) {
      return;
    }

    trackers.get(player.getUniqueId()).addNewLocation(to);
  }

  private String colorize(String msg) {
    return ChatColor.translateAlternateColorCodes('&', msg);
  }
}
