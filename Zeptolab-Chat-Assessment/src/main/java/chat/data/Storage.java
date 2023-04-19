package chat.data;

import chat.model.ChatChannel;
import chat.model.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

public interface Storage {

    Set<ChatChannel> getChatChannels();
    User createUser(final ChannelHandlerContext ctx, final String username, final String password);
    User getUserByName(final String username);
    Set<User> getUserList(final User user);
    ChatChannel getCurrentChatChannel(final User user);
    boolean anyChannelContainsUser(final User user);
    void removeUserFromChannels(final User user);
    void removeChannelFromUser(final User user);
    void addChatChannelToUser(final User user, final ChatChannel channel);
    void addChatChannel(final ChatChannel channel);
    void loginUser(final Channel channel, final User user);
    void logoutUser(final ChannelHandlerContext ctx, final User user);
    User getUserByChannel(final Channel channel);
}