package eu.decentsoftware.holograms.api.utils.scheduler.bukkit;

import eu.decentsoftware.holograms.api.utils.scheduler.adapter.SchedulerAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BukkitSchedulerAdapter implements SchedulerAdapter {

    private @NonNull Plugin plugin;

    @Override
    public BukkitTask runAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }

    @Override
    public BukkitTask runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, runnable, unit.toMillis(delay) / 50);
    }

    @Override
    public BukkitTask runAsyncRate(Runnable runnable, long delay, long period, TimeUnit unit) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, runnable, unit.toMillis(delay) / 50, unit.toMillis(period) / 50);
    }

    @Override
    public BukkitTask runAtEntity(Entity entity, Runnable runnable) {
        return Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    @Override
    public BukkitTask runAtEntityDelayed(Entity entity, Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delay);
    }

}
