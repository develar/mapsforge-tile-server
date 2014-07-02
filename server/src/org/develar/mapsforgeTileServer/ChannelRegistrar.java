package org.develar.mapsforgeTileServer;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jetbrains.annotations.NotNull;

@ChannelHandler.Sharable
public final class ChannelRegistrar extends ChannelInboundHandlerAdapter {
  private final ChannelGroup openChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  public void addServerChannel(@NotNull Channel serverChannel) {
    assert serverChannel instanceof ServerChannel;
    openChannels.add(serverChannel);
  }

  @Override
  public void channelActive(ChannelHandlerContext context) throws Exception {
    // we don't need to remove channel on close - ChannelGroup do it
    openChannels.add(context.channel());

    super.channelActive(context);
  }

  public void closeAndSyncUninterruptibly() {
    openChannels.close().syncUninterruptibly();
  }
}