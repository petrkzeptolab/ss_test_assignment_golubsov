package chat.data;

import chat.model.ChatChannel;
import chat.model.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage implements Storage {

    private final List<User> users = new ArrayList<>();
    private final Set<ChatChannel> chatChannels = new HashSet<>();
    private final Map<User, ChatChannel> userToChat = new ConcurrentHashMap<>();
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final AttributeKey<User> USER_KEY = AttributeKey.valueOf("user");

    @Override
    public synchronized Set<ChatChannel> getChatChannels() {
        return Collections.unmodifiableSet(chatChannels);
    }

    @Override
    public synchronized void loginUser(final Channel channel, final User user) {
        user.addChannel(channel);
    }

    @Override
    public synchronized void logoutUser(final ChannelHandlerContext ctx, final User user) {
        if (user != null) {
            final var channel = ctx.channel();
            user.removeChannel(channel);
            if (user.getChannels().isEmpty()) {
                userToChat.get(user).removeUser(user);
            }
        }
    }

    @Override
    public synchronized User createUser(final ChannelHandlerContext ctx, final String username, final String password) {
        final var user = new User(username, password);
        ctx.channel().attr(USER_KEY).set(user);
        final var channel = ctx.channel();
        user.addChannel(channel);
        users.add(user);
        return user;
    }
    public synchronized User getUserByName(final String username) {
        return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    @Override
    public synchronized Set<User> getUserList(final User user) {
        final var channel = userToChat.get(user);
        if (channel == null) {
            return Set.of();
        }
        synchronized (channel) {
            return Set.copyOf(channel.getUsers());
        }
    }

    @Override
    public synchronized ChatChannel getCurrentChatChannel(final User user) {
        final var channel = userToChat.get(user);
        return channel != null && (channel.getUsers().contains(user) || channel.addUser(user)) ? channel : null;
    }

    @Override
    public synchronized boolean anyChannelContainsUser(final User user) {
        return userToChat.containsKey(user);
    }

    @Override
    public synchronized void removeUserFromChannels(final User user) {
        synchronized (chatChannels) {
            chatChannels.forEach(channel -> {
                final var chUsers = channel.getUsers();
                if (chUsers.contains(user)) {
                    channel.removeUser(user);
                }
            });
        }
    }

    @Override
    public synchronized void removeChannelFromUser(final User user) {
        userToChat.remove(user);
    }

    @Override
    public synchronized void addChatChannelToUser(final User user, final ChatChannel channel) {
        userToChat.put(user, channel);
    }

    @Override
    public synchronized void addChatChannel(final ChatChannel channel) {
        chatChannels.add(channel);
    }

    @Override
    public synchronized User getUserByChannel(Channel channel) {
        return users.stream().filter(u -> u.getChannels().contains(channel)).findFirst().orElse(null);
    }
}
