package org.develar.mapsforgeTileServer

import io.netty.channel.*
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor

ChannelHandler.Sharable
public class ChannelRegistrar() : ChannelInboundHandlerAdapter() {
  private val openChannels = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  public fun addServerChannel(serverChannel: Channel) {
    assert(serverChannel is ServerChannel)
    openChannels.add(serverChannel)
  }

  override fun channelActive(context: ChannelHandlerContext) {
    // we don't need to remove channel on close - ChannelGroup do it
    openChannels.add(context.channel())

    super.channelActive(context)
  }

  public fun closeAndSyncUninterruptibly() {
    openChannels.close().syncUninterruptibly()
  }
}