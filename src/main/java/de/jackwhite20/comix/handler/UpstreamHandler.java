/*
 * Copyright (c) 2015 "JackWhite20"
 *
 * This file is part of Comix.
 *
 * Comix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jackwhite20.comix.handler;

import de.jackwhite20.comix.Comix;
import de.jackwhite20.comix.network.ComixClient;
import de.jackwhite20.comix.strategy.BalancingStrategy;
import de.jackwhite20.comix.util.TargetData;
import de.jackwhite20.comix.util.Util;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by JackWhite20 on 17.07.2015.
 */
public class UpstreamHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private ComixClient client;

    private BalancingStrategy strategy;

    private Channel upstreamChannel;

    private Channel downstreamChannel;

    private boolean downstreamConnected;

    private DownstreamHandler downstreamHandler;

    private long upstreamBytesIn;

    private long downstreamBytesOut;

    private List<ByteBuf> initialPackets = new ArrayList<>();

    public UpstreamHandler(BalancingStrategy strategy) {
        this.strategy = strategy;
    }

    public void connectDownstream(ByteBuf initPacket) {
        InetSocketAddress address = (InetSocketAddress) upstreamChannel.remoteAddress();
        TargetData target = this.strategy.selectTarget(address.getHostName(), address.getPort());

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(upstreamChannel.eventLoop())
                .channel(upstreamChannel.getClass())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_TIMEOUT, 5000)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .handler(downstreamHandler = new DownstreamHandler(client, upstreamChannel));

        ChannelFuture f = bootstrap.connect(target.getHost(), target.getPort());
        downstreamChannel = f.channel();

        initialPackets.add(initPacket);

        f.addListener((future) -> {
            if (future.isSuccess()) {
                downstreamConnected = true;

                for (ByteBuf packet : initialPackets) {
                    downstreamChannel.writeAndFlush(packet);
                }

                Comix.getLogger().log(Level.INFO, "Proxy", "[" + client.getName() + "] <-> [Comix] <-> [" + target.getName() + "] tunneled");
            } else {
                upstreamChannel.close();
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        upstreamChannel = ctx.channel();

        upstreamChannel.read();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if(downstreamConnected) {
            downstreamChannel.writeAndFlush(byteBuf.retain()).addListener((future) -> {
                if(future.isSuccess()) {
                    ctx.channel().read();
                }else {
                    ctx.channel().close();
                }
            });
        }else {
            ctx.channel().read();
        }

        upstreamBytesIn += byteBuf.readableBytes();
        downstreamBytesOut += byteBuf.readableBytes();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (downstreamChannel != null) {
            if (downstreamChannel.isActive()) {
                downstreamChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }

            if(client != null)
                Comix.getInstance().removeClient(client);

            upstreamBytesIn = 0;
            downstreamBytesOut = 0;

            Comix.getLogger().info("[" + ((client != null) ? client.getName() : Util.formatSocketAddress(upstreamChannel.remoteAddress())) + "] -> UpstreamHandler has disconnected");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel ch = ctx.channel();
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void addInitialPacket(ByteBuf buf) {
        if(!downstreamConnected)
            initialPackets.add(buf);
    }

    public Channel getUpstreamChannel() {
        return upstreamChannel;
    }

    public DownstreamHandler getDownstreamHandler() {
        return downstreamHandler;
    }

    public void setClient(ComixClient client) {
        this.client = client;
    }

    public long getUpstreamBytesIn() {
        return upstreamBytesIn;
    }

    public long getDownstreamBytesOut() {
        return downstreamBytesOut;
    }

    public boolean isDownstreamConnected() {
        return downstreamConnected;
    }
}
