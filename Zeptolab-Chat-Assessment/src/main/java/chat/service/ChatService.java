package chat.service;

import chat.data.Storage;
import chat.model.ChatChannel;
import chat.model.User;
import io.netty.channel.ChannelHandlerContext;

import static chat.data.LocalStorage.LINE_SEPARATOR;
import static chat.data.LocalStorage.USER_KEY;

public class ChatService {

    /**
     * Logs in user into the application itself. If user does not exist, they're created.
     * If user exists, they're logged in if password is correct. If user was connected to the channel prior
     * to disconnecting, they're connected to that channel if it's not full.
     *
     * @param argument login and password information of user
     */
    public void handleLogin(final ChannelHandlerContext ctx, final Storage storage, final String argument) {
        if (argument == null) {
            ctx.writeAndFlush(String.format("ERROR: Missing arguments.%s", LINE_SEPARATOR));
            return;
        }

        final var loginParts = argument.split("\\s+");
        if (loginParts.length != 2) {
            ctx.writeAndFlush(String.format("ERROR: Invalid arguments.%s", LINE_SEPARATOR));
            return;
        }

        final var userName = loginParts[0];
        final var password = loginParts[1];

        final var user = storage.getUserByName(userName);
        if (user != null) {
            if (user.getPassword().equals(password)) {
                var channel = ctx.channel();
                storage.logoutUser(ctx, channel.attr(USER_KEY).get());
                channel.attr(USER_KEY).set(user);
                storage.loginUser(channel, user);
                ctx.writeAndFlush(String.format("You've logged in as %s.%s", userName, LINE_SEPARATOR));
                joinChannel(ctx, storage, user, storage.getCurrentChatChannel(user));
            } else {
                ctx.writeAndFlush(String.format("ERROR: Wrong password.%s", LINE_SEPARATOR));
            }
        } else {
            storage.createUser(ctx, userName, password);
            ctx.writeAndFlush(String.format("Welcome %s.%s", userName, LINE_SEPARATOR));
        }
    }

    /**
     * If channel doesn't exist, it is created and {@link ChatService#joinChannel} is called
     *
     * @param argument channel name that user wants to join
     */
    public void handleJoin(final ChannelHandlerContext ctx, final Storage storage, final User user, final String argument) {
        synchronized (this) {
            if (argument == null) {
                ctx.writeAndFlush(String.format("ERROR: Please input channel name.%s", LINE_SEPARATOR));
                return;
            }
            final var channel = storage.getChatChannels()
                    .stream()
                    .filter(ch -> ch.getName().equals(argument))
                    .findFirst().orElseGet(() -> {
                        ctx.writeAndFlush(String.format("New channel %s has been created.%s", argument, LINE_SEPARATOR));
                        return new ChatChannel(argument, 10);
                    });
            joinChannel(ctx, storage, user, channel);
        }
    }

    /**
     * Removes user from the chat channel
     */
    public void handleLeave(final ChannelHandlerContext ctx, final Storage storage, final User user) {
        if (loginRequired(ctx, user)) {
            return;
        }
        final var cc = storage.getCurrentChatChannel(user);
        storage.removeUserFromChannels(user);
        storage.removeChannelFromUser(user);
        final var message = String.format("User %s has left the channel.%s", user, LINE_SEPARATOR);
        cc.getUsers().stream().flatMap(u -> u.getChannels().stream()).forEach(ch -> ch.writeAndFlush(message));
    }

    /**
     * Logs out user from the application.
     */
    public void logout(final ChannelHandlerContext ctx, final Storage storage, final User user) {
        final var cc = storage.getCurrentChatChannel(user);
        if (user != null) {
            storage.logoutUser(ctx, user);
        }
        final var chatUsers = cc.getUsers();
        if (!chatUsers.contains(user)) {
            final var message = String.format("User %s has left the channel.%s", user, LINE_SEPARATOR);
            chatUsers.stream().flatMap(u -> u.getChannels().stream()).forEach(ch -> ch.writeAndFlush(message));
        }
        ctx.close();
    }

    /**
     * Lists all available chat channels to user
     */
    public void listChannels(final ChannelHandlerContext ctx, final Storage storage) {
        final var channels = storage.getChatChannels();
        for (ChatChannel channel : channels) {
            ctx.channel().writeAndFlush(String.format(channel + "%s", LINE_SEPARATOR));
        }
    }

    /**
     * Lists all users in chat channel
     */
    public void listUsers(final ChannelHandlerContext ctx, final Storage storage, final User user) {
        if (loginRequired(ctx, user)) {
            return;
        }
        final var users = storage.getUserList(user);
        users.forEach(u -> ctx.channel().writeAndFlush(String.format("%s%s", u, LINE_SEPARATOR)));
    }

    /**
     * Sends a message to all users in chat channel
     */
    public void sendMessage(final ChannelHandlerContext ctx, final Storage storage, final User user, final String msg) {
        if (loginRequired(ctx, user)) {
            return;
        }
        final var channel = storage.getCurrentChatChannel(user);
        if (channel != null) {
            channel.write(user, msg);
        } else {
            ctx.channel().writeAndFlush(String.format("You've not joined any channel.%s", LINE_SEPARATOR));
        }
    }

    /**
     * If channel exists user is connected to it, if it has enough capacity.
     * Newly connected user receives 10 last messages from this channel.
     * If channel is full, and error message is shown
     */
    public void joinChannel(final ChannelHandlerContext ctx, final Storage storage, final User user, final ChatChannel channel) {
        if (channel == null) {
            return;
        }
        if (storage.anyChannelContainsUser(user)) {
            storage.removeUserFromChannels(user);
        }

        if (channel.addUser(user)) {
            ctx.writeAndFlush(String.format("You've joined channel %s.%s", channel.getName(), LINE_SEPARATOR));
            final var messages = channel.getMessages();
            if (!messages.isEmpty()) {
                messages.forEach(m -> ctx.writeAndFlush(String.format("%s%s", m, LINE_SEPARATOR)));
            }

            if (!storage.getChatChannels().contains(channel)) {
                storage.addChatChannel(channel);
            }
            storage.addChatChannelToUser(user, channel);
        } else {
            ctx.writeAndFlush(String.format("The channel you've tried to connect is at max capacity.%s", LINE_SEPARATOR));
        }
    }

    /**
     * If client is not logged in, and error is shown
     */
    public boolean loginRequired(final ChannelHandlerContext ctx, final User user) {
        if (user == null) {
            ctx.writeAndFlush(String.format("ERROR: Please login to system first.%s", LINE_SEPARATOR));
            return true;
        }
        return false;
    }

}
