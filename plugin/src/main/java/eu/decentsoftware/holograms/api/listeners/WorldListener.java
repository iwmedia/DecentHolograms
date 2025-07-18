package eu.decentsoftware.holograms.api.listeners;

import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.holograms.DisableCause;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramManager;
import eu.decentsoftware.holograms.api.utils.Log;
import eu.decentsoftware.holograms.api.utils.exception.LocationParseException;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashSet;
import java.util.Set;

public class WorldListener implements Listener {

    private final DecentHolograms decentHolograms;
    private final HologramManager hologramManager;

    public WorldListener(DecentHolograms decentHolograms, HologramManager hologramManager) {
        this.decentHolograms = decentHolograms;
        this.hologramManager = hologramManager;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();

        this.decentHolograms.getScheduler().runAsync(() -> hologramManager.getHolograms().stream()
                .filter(Hologram::isEnabled)
                .filter(hologram -> hologram.getLocation().getWorld().equals(world))
                .forEach(hologram -> hologram.disable(DisableCause.WORLD_UNLOAD)));
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();

        this.decentHolograms.getScheduler().runAsync(() -> {
            Set<String> hologramsToLoad = getHologramsToLoadByWorld(world);
            if (!hologramsToLoad.isEmpty()) {
                hologramsToLoad.forEach(fileName -> {
                    try {
                        Hologram hologram = Hologram.fromFile(fileName);
                        if (hologram.isEnabled()) {
                            hologram.showAll();
                            hologram.realignLines();
                            hologramManager.registerHologram(hologram);
                        }
                    } catch (LocationParseException ignored) {
                        Log.warn("Failed to load hologram from file: " + fileName);
                    }
                });
            }
            hologramManager.getHolograms().stream()
                    .filter(hologram -> !hologram.isEnabled())
                    .filter(hologram -> hologram.getLocation().getWorld().equals(world))
                    .filter(hologram -> hologram.getDisableCause().equals(DisableCause.WORLD_UNLOAD))
                    .forEach(Hologram::enable);
        });
    }

    private Set<String> getHologramsToLoadByWorld(World world) {
        Set<String> hologramsToLoad = new HashSet<>();
        if (hologramManager.getToLoad().containsKey(world.getName())) {
            hologramsToLoad.addAll(hologramManager.getToLoad().get(world.getName()));
        }
        if (hologramManager.getToLoad().containsKey(world.getUID().toString())) {
            hologramsToLoad.addAll(hologramManager.getToLoad().get(world.getUID().toString()));
        }
        return hologramsToLoad;
    }

}
