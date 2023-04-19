package chat.service;

import chat.ChatServerTestBase;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static chat.data.LocalStorage.USER_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatServiceTest extends ChatServerTestBase {

    private static final String LINE_SEPARATOR = System.lineSeparator();
    final ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    final Channel channel = mock(Channel.class);
    final ChatService cs = new ChatService();

    {
        when(ctx.channel()).thenReturn(channel);
    }

    @Test
    void whenLoginIsCalledAnNoArgumentsMissingArgumentsMessageIsShown() {
        cs.handleLogin(ctx, storage, null);
        verify(ctx, times(1)).writeAndFlush(String.format("ERROR: Missing arguments.%s", LINE_SEPARATOR));
    }

    @ParameterizedTest
    @CsvSource({"name pass wrong", "onlyName"})
    void whenArgumentsLengthIsNot2InvalidArgumentsMessageIsShown(final String params) {
        cs.handleLogin(ctx, storage, params);
        verify(ctx, times(1)).writeAndFlush(String.format("ERROR: Invalid arguments.%s", LINE_SEPARATOR));
    }

    @Test
    void whenUserDoesNotExistItIsCreated() {
        cs.handleLogin(ctx, storage, "name pass");
        verify(storage, times(1)).createUser(ctx, "name", "pass");
    }

    @Test
    void whenUserExistsButPasswordIsWrongErrorMessageIsShown() {
        mockUser("name", "pass");
        cs.handleLogin(ctx, storage, "name wrongpass");
        verify(ctx, times(1)).writeAndFlush(String.format("ERROR: Wrong password.%s", LINE_SEPARATOR));
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenUserExistsAndLoginWithPasswordAreCorrectUserIsLoggedIn() {
        final var userName = "name";
        final var user = mockUser(userName, "pass").user;
        final var attr = mock(Attribute.class);
        when(channel.attr(USER_KEY)).thenReturn(attr);
        cs.handleLogin(ctx, storage, "name pass");
        assertAll(() -> verify(attr).set(user),
                () -> verify(storage).loginUser(channel, user),
                () -> verify(ctx).writeAndFlush(String.format("You've logged in as %s.%s", userName, LINE_SEPARATOR)));
    }

    @Test
    void whenUserIsNotNullLoggedInIsTrue() {
        final var user = mockUser("name", "pass").user;
        assertFalse(cs.loginRequired(ctx, user));
    }

    @Test
    void whenUserIsNullLoggedInIsFalseAndErrorMessageIsShown() {
        assertAll(() -> assertTrue(cs.loginRequired(ctx, null)),
                () -> verify(ctx).writeAndFlush(String.format("ERROR: Please login to system first.%s", LINE_SEPARATOR)));
    }

    @Test
    void whenNoArgumentOnHandleJoinErrorIsThrown() {
        final var user = mockUser("name", "pass").user;
        cs.handleJoin(ctx, storage, user, null);
        verify(ctx).writeAndFlush(String.format("ERROR: Please input channel name.%s", LINE_SEPARATOR));
    }

    @Test
    void whenHandleJoinIsCalledChannelIsCreated() {
        final var chName = "channel";
        final var user = mockUser("name", "pass").user;
        cs.handleJoin(ctx, storage, user, chName);
        assertAll(() -> verify(ctx).writeAndFlush(String.format("New channel %s has been created.%s", chName, LINE_SEPARATOR)),
                () -> verify(ctx).writeAndFlush(String.format("You've joined channel %s.%s", chName, LINE_SEPARATOR)),
                () -> assertEquals(1, channels.size()));
    }

    @Test
    void whenChannelExistsNewChannelIsNotCreated() {
        final var chName = "channel";
        final var user = mockUser("name", "pass").user;
        mockChannel(chName);
        cs.handleJoin(ctx, storage, user, chName);
        assertAll(() -> verify(ctx, never()).writeAndFlush(String.format("New channel %s has been created.%s", chName, LINE_SEPARATOR)),
                () -> verify(ctx).writeAndFlush(String.format("You've joined channel %s.%s", chName, LINE_SEPARATOR)),
                () -> assertEquals(1, channels.size()));
    }

    @Test
    void whenChannelExistsAndFullNewUsersCannotJoinIt() {
        final var chName = "channel";
        final var channel = mockChannel(chName);
        for (int i = 0; i < 10; i++) {
            channel.addUser(mockUser("" + i, "pass").user);
        }
        final var user = mockUser("name", "pass").user;
        cs.handleJoin(ctx, storage, user, chName);
        assertAll(() ->
                        verify(ctx)
                                .writeAndFlush(String.format("The channel you've tried to connect is at max capacity.%s", LINE_SEPARATOR)),
                () ->
                        verify(ctx, never())
                                .writeAndFlush(String.format("New channel %s has been created.%s", chName, LINE_SEPARATOR)),
                () ->
                        verify(ctx, never())
                                .writeAndFlush(String.format("You've joined channel %s.%s", chName, LINE_SEPARATOR)));
    }

    @Test
    void whenUserJoinsNewChannelTheyAreRemovedFromOldOne() {
        final var oldChannel = mockChannel("channel1");
        final var user = mockUser("name", "pass").user;
        final var newChannel = mockChannel("channel2").channel;
        oldChannel.addUser(user);
        cs.joinChannel(ctx, storage, user, newChannel);
        assertAll(
                () -> assertTrue(storage.anyChannelContainsUser(user)),
                () -> verify(storage).removeUserFromChannels(user),
                () -> assertFalse(oldChannel.channel.getUsers().contains(user)));
    }

    @Test
    void whenUserLeaveChannelHeIsRemovedFromChannelUserList() {
        final var channel = mock(Channel.class);
        final var user = mockUser("name", "pass").user;
        final var secondUser = mockUser("name2", "pass").addChannel(channel).user;
        final var cc = mockChannel("channel").addUser(user).addUser(secondUser);
        when(storage.getCurrentChatChannel(user)).thenReturn(cc.channel);
        cs.handleLeave(ctx, storage, user);
        assertAll(() ->
                        verify(channel).writeAndFlush(String.format("User %s has left the channel.%s", user, LINE_SEPARATOR)),
                () -> assertFalse(cc.users.contains(user)));
    }

}
