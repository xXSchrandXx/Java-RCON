package de.xxschrandxx.wsc.rcon.commandsender;

import java.net.InetAddress;

public class RconCommandSender {

    private InetAddress ip;

    public InetAddress getIP() {
        return this.ip;
    }

    public void setIP(InetAddress ip) {
        this.ip = ip;
    }

    public String getName() {
        return "Rcon";
    }

    public boolean hasPermission(String permission) {
        return true;
    }

    protected final StringBuffer buffer = new StringBuffer();

    public String flush() {
        String result = buffer.toString();
        buffer.setLength(0);
        return result;
    }

    public void sendMessage(String message) {
        buffer.append(message).append("\n");
    }

    public Object getServer() {
        return null;
    }

    public boolean dispatchCommand(String commandLine) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RconCommandSender clone() {
        return new RconCommandSender();
    }
}
