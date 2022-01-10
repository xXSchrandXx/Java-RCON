package de.xxschrandxx.wsc.rcon.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.logging.Logger;

import de.xxschrandxx.wsc.rcon.commandsender.RconCommandSender;

public class RconServer extends Thread {

    private final String password;
    public String getPassword() {
        return this.password;
    }

    private List<InetAddress> whitelist;
    public List<InetAddress> getWhiteList() {
        return this.whitelist;
    }

    private final RconCommandSender sender;
    public RconCommandSender getSender() {
        return this.sender.clone();
    }

    private final Logger logger;
    public Logger getLogger() {
        return this.logger;
    }

    private ServerBootstrap bootstrap = new ServerBootstrap();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public RconServer(final String password, List<InetAddress> whitelist, RconCommandSender sender, Logger logger) {
        this.password = password;
        this.whitelist = whitelist;
        this.sender = sender;
        this.logger = logger;
    }

    @Override
    public void run() {
        bootstrap
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                .addLast(new RconFramingHandler())
                .addLast(new RconHandler(RconServer.this, password, whitelist));
            }
        });
    }

    /**
     * Bind the server on the specified address.
     *
     * @param address The address.
     * @return Netty channel future for bind operation.
     */
    public ChannelFuture bind(final SocketAddress address) {
        return bootstrap.bind(address);
    }

    /**
     * Shut the Rcon server down.
     */
    public void shutdown() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        this.interrupt();
    }

}
