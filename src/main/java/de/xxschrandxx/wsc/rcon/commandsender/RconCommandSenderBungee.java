package de.xxschrandxx.wsc.rcon.commandsender;

import java.util.ArrayList;
import java.util.Collection;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;

public class RconCommandSenderBungee extends RconCommandSender implements CommandSender {

    public RconCommandSenderBungee clone() {
        return new RconCommandSenderBungee();
    }

    @Override
    public ProxyServer getServer() {
        return ProxyServer.getInstance();
    }

    @Override
    public boolean dispatchCommand(String commandline) {
        return getServer().getPluginManager().dispatchCommand(this, commandline);
    }

    @Override
    public void sendMessages(String... messages) {
        for (String line : messages) {
            sendMessage(line);
        }
    }

    @Override
    public void sendMessage(BaseComponent... message) {
        for (BaseComponent line : message) {
            sendMessage(line);
        }
    }

    @Override
    public void sendMessage(BaseComponent message) {
        sendMessage(message.toLegacyText());
    }

    @Override
    public Collection<String> getGroups() {
        return new ArrayList<>();
    }

    @Override
    public void addGroups(String... groups) {
    }

    @Override
    public void removeGroups(String... groups) {
    }

    @Override
    public void setPermission(String permission, boolean value) {
    }

    @Override
    public Collection<String> getPermissions() {
        return new ArrayList<>();
    }
}
