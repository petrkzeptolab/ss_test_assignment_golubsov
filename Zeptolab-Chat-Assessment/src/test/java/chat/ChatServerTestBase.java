package chat;

import chat.data.Storage;
import chat.model.*;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

public class ChatServerTestBase {

    protected final Storage storage = mock(Storage.class);
    public final Set<ChatChannel> channels = new HashSet<>();

    {
        when(storage.getChatChannels()).thenReturn(channels);
        doAnswer(e -> {
            channels.add(e.getArgument(0));
            return null;
        }).when(storage).addChatChannel(any());
        when(storage.anyChannelContainsUser(any()))
                .thenAnswer(e -> channels.stream()
                        .anyMatch(ch -> ch.getUsers().contains(e.getArgument(0))));
        doAnswer(e -> {
            final var user = (User) e.getArgument(0);
            channels.stream().filter(ch -> ch.getUsers().contains(user)).forEach(ch -> ch.removeUser(user));
            return null;
        }).when(storage).removeUserFromChannels(any());
    }

    protected final MockUser mockUser(final String username, final String password) {
        final var mockUser = new MockUser(username, password);
        when(storage.getUserByName(username)).thenReturn(mockUser.user);
        return mockUser;
    }

    protected final MockChatChannel mockChannel(final String name) {
        return mockChannel(name, 10);
    }

    protected final MockChatChannel mockChannel(final String name, final int capacity) {
        final var channel = new MockChatChannel(name, capacity);
        channels.add(channel.channel);
        return channel;
    }

}
