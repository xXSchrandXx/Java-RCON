package de.xxschrandxx.wsc.rcon.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

        rconServer.getLogger().log(Level.INFO, "Connection incomming from " + ip + ".");

        this.commandSender.setIP(ip);
        if (!this.whitelist.isEmpty()) {
            if (!this.whitelist.contains(ip)) {
                sendResponse(ctx, FAILURE, TYPE_COMMAND, "Not whitelisted.");
                rconServer.getLogger().log(Level.INFO, ip + " not whitelisted.");
            }
        }

        int requestId = buf.readIntLE();
        int type = buf.readIntLE();

        rconServer.getLogger().log(Level.INFO, "RequestID: " + requestId + ", Type: " + type);

        byte[] payloadData = new byte[buf.readableBytes() - 2];
        buf.readBytes(payloadData);
        String payload = new String(payloadData, StandardCharsets.UTF_8);

        rconServer.getLogger().log(Level.INFO, "Payload: " + payload);
        
        buf.readBytes(2); // two byte padding

        if (type == TYPE_LOGIN) {
            handleLogin(ctx, payload, requestId);
        } else if (type == TYPE_COMMAND) {
            handleCommand(ctx, payload, requestId);
        } else {
            sendLargeResponse(ctx, requestId, "Unknown request " + Integer.toHexString(type));
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, String payload, int requestId) throws IOException {
        if (password.intern().equals(payload.intern())) {
            loggedIn = true;

            sendResponse(ctx, requestId, TYPE_COMMAND, "Successfully logged in.");

            rconServer.getLogger().log(Level.INFO, "Rcon connection from [{0}]", ctx.channel().remoteAddress());
        } else {
            loggedIn = false;
            sendResponse(ctx, FAILURE, TYPE_COMMAND, "Wrong password.");

            rconServer.getLogger().log(Level.INFO, this.password + " is not equal " + payload);
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
            String message = "No such command";

            sendLargeResponse(ctx, requestId, String.format("Error executing: %s (%s)", payload, message));
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, Integer requestId, Integer type, String payload) throws IOException {
        byte[] payloadData = payload.getBytes(StandardCharsets.UTF_8);
        int packetSize = 4 + payloadData.length + 1 + 1;
        ByteBuf buf = ctx.alloc().buffer(4+packetSize);
        buf.writeIntLE(packetSize);
        buf.writeIntLE(requestId);
        buf.writeIntLE(type);
        buf.writeBytes(payloadData);
        buf.writeByte((byte) 0);
        buf.writeByte((byte) 0);

        ByteBuf readBuf = buf.copy();
        rconServer.getLogger().log(Level.INFO, "Send packet:");
        rconServer.getLogger().log(Level.INFO, "Size: " + readBuf.readIntLE());
        rconServer.getLogger().log(Level.INFO, "ID: " + readBuf.readIntLE());
        rconServer.getLogger().log(Level.INFO, "Response: " + readBuf.readIntLE());
        byte[] data = new byte[readBuf.readableBytes() - 2];
        readBuf.readBytes(data);
        String msg = new String(data, StandardCharsets.UTF_8);
        rconServer.getLogger().log(Level.INFO, "CMD: " + msg);
        readBuf.readBytes(2);

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
