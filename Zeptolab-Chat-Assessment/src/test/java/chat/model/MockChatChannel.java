package chat.model;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MockChatChannel {

    public final ChatChannel channel = mock(ChatChannel.class);
    public final Set<User> users;
    private final int capacity;

    public MockChatChannel(final String name, final int capacity) {
        when(channel.getName()).thenReturn(name);
        users = new HashSet<>();
        this.capacity = capacity;
        when(channel.getUsers()).thenAnswer(e -> users);
        when(channel.addUser(any())).thenAnswer(e -> {
            if (users.size() < this.capacity) {
                return users.add(e.getArgument(0));
            }
            return false;
        });
        doAnswer(e -> {
            users.remove(e.getArgument(0));
            return null;
        }).when(channel).removeUser(any());
    }

    public MockChatChannel addUser(final User user) {
        users.add(user);
        return this;
    }
}
