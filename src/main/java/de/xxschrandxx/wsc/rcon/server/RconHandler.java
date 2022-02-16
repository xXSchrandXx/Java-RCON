package de.xxschrandxx.wsc.rcon.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

import de.xxschrandxx.wsc.rcon.commandsender.*;

public class RconHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Integer FAILURE = -1;
    private static final Integer TYPE_RESPONSE = 0;
    private static final Integer TYPE_COMMAND = 2;
    private static final Integer TYPE_LOGIN = 3;

    private final String password;

    private final List<InetAddress> whitelist;

    private boolean loggedIn = false;

    /**
     * The {@link RconServer} this handler belongs to.
     */
    private RconServer rconServer;

    /**
     * The {@link RconCommandSender} for this connection.
     */
    private final RconCommandSender commandSender;

    public RconHandler(RconServer rconServer, String password, List<InetAddress> whitelist) {
        this.rconServer = rconServer;
        this.password = password;
        this.whitelist = whitelist;

        this.commandSender = rconServer.getSender().clone();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        if (buf.readableBytes() < 8) {
            return;
        }

        InetAddress ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();

        Integer storedTries = rconServer.connectionTries.get(ip);

        int attempts = 1;
        if (storedTries != null) {
            attempts += storedTries;
        }

        if (attempts >= 100) {
            Long lastTime = rconServer.connectionTimes.get(ip);
            if (lastTime != null) {
                if (!((System.currentTimeMillis() - lastTime) >= (1 * 60 * 1000))) {
                    ctx.disconnect();
                }
            }
        }

        rconServer.connectionTries.put(ip, attempts);
        rconServer.connectionTimes.put(ip, System.currentTimeMillis());

        rconServer.getLogger().log(Level.INFO, "Connection incomming from " + ip + ".");

        this.commandSender.setIP(ip);
        if (!this.whitelist.isEmpty()) {
            if (!this.whitelist.contains(ip)) {
                sendResponse(ctx, FAILURE, TYPE_COMMAND, "Not whitelisted.");
                rconServer.getLogger().log(Level.INFO, ip + " not whitelisted.");
                ctx.disconnect();
            }
        }

        int requestId = buf.readIntLE();
        int type = buf.readIntLE();

        byte[] payloadData = new byte[buf.readableBytes() - 2];
        buf.readBytes(payloadData);
        String payload = new String(payloadData, StandardCharsets.UTF_8);

        buf.readBytes(2);

        if (type == TYPE_LOGIN) {
            handleLogin(ctx, payload, requestId);
        } else if (type == TYPE_COMMAND) {
            handleCommand(ctx, payload, requestId);
        } else {
            sendLargeResponse(ctx, requestId, "Unknown request " + Integer.toHexString(type));
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, String payload, int requestId) throws IOException {
        if (password.equals(payload)) {
            loggedIn = true;
            sendResponse(ctx, requestId, TYPE_COMMAND, "Successfully logged in.");
        } else {
            loggedIn = false;
            sendResponse(ctx, FAILURE, TYPE_COMMAND, "Wrong password.");
        }
    }

    private void handleCommand(ChannelHandlerContext ctx, String payload, int requestId) throws IOException {
        if (!loggedIn) {
            sendResponse(ctx, FAILURE, TYPE_COMMAND, "");
            return;
        }

        if (commandSender.dispatchCommand(payload)) {
            String message = commandSender.flush();

            sendLargeResponse(ctx, requestId, message);
        } else {
            String message = commandSender.flush();
            if (message.isEmpty()) {
                message =  "No such command";
            }

            sendLargeResponse(ctx, requestId, String.format("Error executing: %s (%s)", payload, message));
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, Integer requestId, Integer type, String payload) throws IOException {
        byte[] payloadData = payload.getBytes(StandardCharsets.UTF_8);
        int packetSize = 4 + 4 + payloadData.length + 1 + 1;
        ByteBuf buf = ctx.alloc().buffer(packetSize);
        buf.writeIntLE(packetSize);
        buf.writeIntLE(requestId);
        buf.writeIntLE(type);
        buf.writeBytes(payloadData);
        buf.writeByte((byte) 0);
        buf.writeByte((byte) 0);

        ctx.write(buf);
    }

    private void sendLargeResponse(ChannelHandlerContext ctx, Integer requestId, String payload) throws IOException {
        if (payload.length() == 0) {
            sendResponse(ctx, requestId, TYPE_RESPONSE, "");
            return;
        }

        int start = 0;
        while (start < payload.length()) {
            int length = payload.length() - start;
            int truncated = length > 2048 ? 2048 : length;

            sendResponse(ctx, requestId, TYPE_RESPONSE, payload.substring(start, truncated));
            start += truncated;
        }
    }
}
