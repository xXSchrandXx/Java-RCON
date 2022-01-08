package de.xxschrandxx.wsc.rcon;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.xxschrandxx.wsc.rcon.commandsender.RconCommandSender;
import de.xxschrandxx.wsc.rcon.server.RconServer;

public class Rcon {

    public Rcon(Logger logger) {
        this.logger = logger;
    }

    public static Rcon instance;

    private RconServer rconServer;
    private final Logger logger;

    /**
     * Starts the RCON listener.
     * @param host The address to listen on.
     * @param port The port to listen on.
     * @param password The password for the rcon.
     * @param sender The {@link RconCommandSender} to use.
     * @param whitelist The whitelisted hosts.
     * @throws UnknownHostException {@link UnknownHostException}
     */
    public void startListener(String host, int port, String password, RconCommandSender sender, List<String> whitelist) throws UnknownHostException {
        InetAddress hostaddress = InetAddress.getByName(host);
        List<InetAddress> whitelistaddresses = new ArrayList<InetAddress>();
        for (String e : whitelist) {
            whitelistaddresses.add(InetAddress.getByName(e));
        }
        startListener(hostaddress, port, password, sender, whitelistaddresses);
    }

    /**
     * Starts the RCON listener.
     * @param host The address to listen on. If null, it uses the wildcard address {@link InetSocketAddress}.
     * @param port The port to listen on.
     * @param password The password for the rcon.
     * @param sender The {@link RconCommandSender} to use.
     * @param whitelist The whitelisted hosts.
     */
    public void startListener(InetAddress host, int port, String password, RconCommandSender sender, List<InetAddress> whitelist) {
        SocketAddress address;
        if (host == null) {
            address = new InetSocketAddress(port);
        }
        else {
            address = new InetSocketAddress(host, port);
        }

        rconServer = new RconServer(password, whitelist, sender, logger);

        logger.log(Level.INFO, "Binding rcon to address: {0}...", address);

        ChannelFuture future = rconServer.bind(address);
        Channel channel = future.awaitUninterruptibly().channel();

        if (!channel.isActive()) {
            logger.warning("Failed to bind rcon port. Address already in use?");
        }
    }

    /**
     * Stops the RCON listener.
     */
    public void stopListener() {
        if (rconServer != null) {
            logger.log(Level.INFO, "Trying to stop RCON listener");
            
            rconServer.shutdown();
        }
    }
}
