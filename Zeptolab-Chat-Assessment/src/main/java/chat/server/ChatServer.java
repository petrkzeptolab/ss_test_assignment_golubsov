package chat.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ChatServer {

    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new ChatServer(8080).run();
    }

    public ChatServer(final int port) {
        this.port = port;
    }

    public void run() throws InterruptedException {
        final var bossGroup = new NioEventLoopGroup();
        final var workGroup = new NioEventLoopGroup();
        try {
            final var bootstrap = new ServerBootstrap()
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChatServerInitializer());
            bootstrap.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

}
