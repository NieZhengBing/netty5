package com.nzb.netty.netty5.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class MultiClient {

	private Bootstrap bootstrap = new Bootstrap();

	private List<Channel> channels = new ArrayList<Channel>();

	private final AtomicInteger index = new AtomicInteger();

	public void init(int count) {
		EventLoopGroup worker = new NioEventLoopGroup();

		bootstrap.group(worker);

		bootstrap.channel(NioSocketChannel.class);
		bootstrap.handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline().addLast(new StringDecoder());
				ch.pipeline().addLast(new StringEncoder());
				ch.pipeline().addLast(new ClientHandler());
			}
		});

		for (int i = 0; i < count; i++) {
			ChannelFuture future = bootstrap.connect("127.0.0.1", 10101);
			channels.add(future.channel());
		}
	}

	public Channel nextChannel() {
		return getFirstActiveChannel(0);
	}

	private Channel getFirstActiveChannel(int count) {
		Channel channel = channels.get(Math.abs(index.getAndIncrement() % channels.size()));
		if (!channel.isActive()) {
			reconnect(channel);
			if (count >= channels.size()) {
				throw new RuntimeException("number can not use channel");
			}
			return getFirstActiveChannel(count + 1);
		}
		return channel;
	}

	private void reconnect(Channel channel) {
		synchronized (channel) {
			if (channels.indexOf(channel) == -1) {
				return;
			}
			Channel newChannel = bootstrap.connect("192.168.0.103", 10101).channel();
			channels.set(channels.indexOf(channel), newChannel);
		}
	}

}
