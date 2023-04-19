package chat.handler;

import chat.data.Storage;
import chat.service.ChatService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static chat.data.LocalStorage.USER_KEY;

public class ChatHandler extends SimpleChannelInboundHandler<String> {

    private final Storage storage;
    private final ChatService chatService;

    public ChatHandler(final ChatService chatService, final Storage storage) {
        this.chatService = chatService;
        this.storage = storage;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        final var parts = msg.trim().split("\\s+", 2);
        final var command = parts[0];
        final var argument = parts.length > 1 ? parts[1] : null;
        var user = ctx.channel().attr(USER_KEY).get();

        switch (command) {
            case "/login" -> chatService.handleLogin(ctx, storage, argument);
            case "/join" -> chatService.handleJoin(ctx, storage, user, argument);
            case "/leave" -> chatService.handleLeave(ctx, storage, user);
            case "/disconnect" -> chatService.logout(ctx, storage, user);
            case "/list" -> chatService.listChannels(ctx, storage);
            case "/users" -> chatService.listUsers(ctx, storage, user);
            default -> chatService.sendMessage(ctx, storage, user, msg);
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        chatService.logout(ctx, storage, storage.getUserByChannel(ctx.channel()));
    }
}