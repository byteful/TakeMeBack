package me.byteful.plugin.tmb;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerTracker {
  private final TakeMeBackPlugin plugin;
  private final Player player;
  private final List<Location> particles;
  private int blocksMoved;

  public PlayerTracker(final TakeMeBackPlugin plugin, final Player player) {
    this.plugin = plugin;
    this.player = player;
    this.particles = new ArrayList<>();
    this.blocksMoved = 0;
    this.particles.add(player.getLocation());
  }

  public void tick() {
    particles.stream()
        .filter(
            loc ->
                loc.distance(player.getLocation())
                    <= plugin.getConfig().getDouble("display_radius", 25.0D))
        .forEach(
            loc -> /*CompletableFuture.runAsync(() -> */ plugin.spawnParticle(player, loc) /*)*/);
  }

  public void addNewLocation(Location loc) {
    if (blocksMoved < plugin.getConfig().getInt("particle_place_frequency", 5)) {
      blocksMoved++;

      return;
    }

    blocksMoved = 0;

    particles.add(loc);
  }
}
