package de.xxschrandxx.wsc.rcon.commandsender;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Server;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.BaseComponent;

public class RconCommandSenderBukkit extends RconCommandSender implements RemoteConsoleCommandSender {

    private static RconCommandSenderBukkit instance;
    private Plugin plugin;

    public RconCommandSenderBukkit(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public RconCommandSenderBukkit clone() {
        return new RconCommandSenderBukkit(this.plugin);
    }

    public static RconCommandSenderBukkit getSender() {
        return instance;
    }

    @Override
    public Server getServer() {
        return this.plugin.getServer();
    }

    @Override
    public boolean dispatchCommand(String commandLine) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        new BukkitRunnable() {
            @Override
            public void run() {
                future.complete(getServer().dispatchCommand(getSender(), commandLine));
            }
        }.runTask(this.plugin);
        return future.join();
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String line : messages) {
            sendMessage(line);
        }
    }

    @Override
    public void sendMessage(UUID sender, String message) {
        sendMessage(sender.toString() + ": " + message);
    }

    @Override
    public void sendMessage(UUID sender, String[] messages) {
        for (String line : messages) {
            sendMessage(sender, line);
        }
    }

    @Override
    public boolean isPermissionSet(String name) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return true;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void recalculatePermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
        throw new UnsupportedOperationException("OP status cannot be changed for Rcon.");
    }

    @Override
    public Spigot spigot() {
        return spigot;
    }

    private Spigot spigot = new RconSpigot(this);

    public class RconSpigot extends Spigot {
        private final RconCommandSenderBukkit parent;

        public RconSpigot(RconCommandSenderBukkit parent) {
            this.parent = parent;
        }

        @Override
        public void sendMessage(BaseComponent component) {
            this.parent.sendMessage(component.toLegacyText());
        }
    
        @Override
        public void sendMessage(BaseComponent... components) {
            for (BaseComponent line : components) {
                sendMessage(line);
            }
        }
    
        @Override
        public void sendMessage(UUID sender, BaseComponent component) {
        }
    
        @Override
        public void sendMessage(UUID sender, BaseComponent... components) {
        }
    }
}
