package eu.decentsoftware.holograms.api.utils.scheduler.folia;

import eu.decentsoftware.holograms.api.utils.scheduler.adapter.SchedulerAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

// Credits: https://github.com/HarvelsX (https://github.com/DecentSoftware-eu/DecentHolograms/pull/188)

@SuppressWarnings("JavaLangInvokeHandleSignature")
@RequiredArgsConstructor
public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private static final boolean SUPPORTED;

    private static @Nullable MethodHandle ASYNC_SCHEDULER_RUN_NOW;
    private static @Nullable MethodHandle ASYNC_SCHEDULER_RUN_DELAYED;
    private static @Nullable MethodHandle ASYNC_SCHEDULER_RUN_RATE;

    private static @Nullable MethodHandle ENTITY_SCHEDULER_EXECUTE;
    private static @Nullable MethodHandle ENTITY_SCHEDULER_RUN_DELAYED;

    private static @Nullable MethodHandle SCHEDULED_TASK_CANCEL;

    static {
        boolean supporting = true;
        try {
            val lookup = MethodHandles.publicLookup();

            val scheduledTaskType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            SCHEDULED_TASK_CANCEL = lookup.findVirtual(scheduledTaskType, "cancel", MethodType.methodType(
                    Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask$CancelledState")
            ));

            val asyncSchedulerType = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");

            val getAsyncScheduler = lookup.findVirtual(Server.class, "getAsyncScheduler", MethodType.methodType(asyncSchedulerType));
            val asyncScheduler = getAsyncScheduler.invoke(Bukkit.getServer());

            ASYNC_SCHEDULER_RUN_NOW = lookup.findVirtual(asyncSchedulerType, "runNow", MethodType.methodType(
                    scheduledTaskType, Plugin.class, Consumer.class
            )).bindTo(asyncScheduler);
            ASYNC_SCHEDULER_RUN_DELAYED = lookup.findVirtual(asyncSchedulerType, "runDelayed", MethodType.methodType(
                    scheduledTaskType, Plugin.class, Consumer.class, long.class, TimeUnit.class
            )).bindTo(asyncScheduler);
            ASYNC_SCHEDULER_RUN_RATE = lookup.findVirtual(asyncSchedulerType, "runAtFixedRate", MethodType.methodType(
                    scheduledTaskType, Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class
            )).bindTo(asyncScheduler);

            val entitySchedulerType = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");

            val getEntityScheduler = lookup.findVirtual(Entity.class, "getScheduler", MethodType.methodType(entitySchedulerType));
            ENTITY_SCHEDULER_EXECUTE = MethodHandles.filterArguments(
                    lookup.findVirtual(entitySchedulerType, "execute", MethodType.methodType(
                            boolean.class, Plugin.class, Runnable.class, Runnable.class, long.class
                    )),
                    0,
                    getEntityScheduler
            );
            ENTITY_SCHEDULER_RUN_DELAYED = MethodHandles.filterArguments(
                    lookup.findVirtual(entitySchedulerType, "runDelayed", MethodType.methodType(
                            scheduledTaskType, Plugin.class, Consumer.class, Runnable.class, long.class
                    )),
                    0,
                    getEntityScheduler
            );

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            supporting = false;
        } catch (Throwable throwable) {
            Logger.getLogger(FoliaSchedulerAdapter.class.getName()).log(Level.WARNING, "Error in Folia scheduler adapter initialization", throwable);
        }
        SUPPORTED = supporting;
    }

    private final @NonNull Plugin plugin;

    public static boolean isSupported() {
        return SUPPORTED;
    }

    @Override
    public BukkitTask runAsync(Runnable runnable) {
        try {
            final Consumer<Object> consumer = task -> runnable.run();
            return new ScheduledTask(Objects.requireNonNull(ASYNC_SCHEDULER_RUN_NOW).invoke(this.plugin, consumer));
        } catch (Throwable e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error in task scheduling by the Folia scheduler adapter", e);
        }

        return new ScheduledTask(null);
    }

    @Override
    public BukkitTask runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        try {
            final Consumer<Object> consumer = task -> runnable.run();
            return new ScheduledTask(Objects.requireNonNull(ASYNC_SCHEDULER_RUN_DELAYED).invoke(this.plugin, consumer, delay, unit));
        } catch (Throwable e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error in task scheduling by the Folia scheduler adapter", e);
        }

        return new ScheduledTask(null);
    }

    @Override
    public BukkitTask runAsyncRate(Runnable runnable, long delay, long period, TimeUnit unit) {
        try {
            final Consumer<Object> consumer = task -> runnable.run();
            return new ScheduledTask(Objects.requireNonNull(ASYNC_SCHEDULER_RUN_RATE).invoke(this.plugin, consumer, delay, period, unit));
        } catch (Throwable e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error in task scheduling by the Folia scheduler adapter", e);
        }

        return new ScheduledTask(null);
    }

    @Override
    public BukkitTask runAtEntity(Entity entity, Runnable runnable) {
        try {
            final Runnable retried = () -> {
            };
            return new ScheduledTask(Objects.requireNonNull(ENTITY_SCHEDULER_EXECUTE).invoke(entity, this.plugin, runnable, retried, 0L));
        } catch (Throwable e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error in task scheduling by the Folia scheduler adapter", e);
        }

        return new ScheduledTask(null);
    }

    @Override
    public BukkitTask runAtEntityDelayed(Entity entity, Runnable runnable, long delay) {
        try {
            final Consumer<Object> consumer = task -> runnable.run();
            final Runnable retried = () -> {
            };
            return new ScheduledTask(Objects.requireNonNull(ENTITY_SCHEDULER_RUN_DELAYED).invoke(entity, this.plugin, consumer, retried, delay));
        } catch (Throwable e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error in task scheduling by the Folia scheduler adapter", e);
        }

        return new ScheduledTask(null);
    }

    @RequiredArgsConstructor
    private class ScheduledTask implements BukkitTask {

        private final Object task;

        @Override
        public int getTaskId() {
            return 0;
        }

        @Override
        public Plugin getOwner() {
            return null;
        }

        @Override
        public boolean isSync() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void cancel() {
            try {
                Objects.requireNonNull(SCHEDULED_TASK_CANCEL).invoke(this.task);
            } catch (Throwable e) {
                FoliaSchedulerAdapter.this.plugin.getLogger().log(Level.SEVERE, "Error in task canceling by the Folia scheduler adapter", e);
            }
        }
    }
}
