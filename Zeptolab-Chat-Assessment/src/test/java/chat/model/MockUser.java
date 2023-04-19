package chat.model;

import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockUser {

    public final User user = mock(User.class);
    private final Set<Channel> channels;

    public MockUser(final String username, final String password) {
        when(user.getUsername()).thenReturn(username);
        when(user.getPassword()).thenReturn(password);
        channels = new HashSet<>();
        when(user.getChannels()).thenReturn(channels);
    }

    public MockUser addChannel(final Channel channel) {
        channels.add(channel);
        return this;
    }

}
