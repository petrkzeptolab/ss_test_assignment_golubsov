package chat.model;

import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Set;

public class User {

    private final String username;
    private final String password;
    private final Set<Channel> channels;

    public User(final String username, final String password) {
        this.username = username;
        this.password = password;
        channels = new HashSet<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof User user) {
            return username.equalsIgnoreCase(user.getUsername())
                    && password.equals(user.getPassword());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return username.toLowerCase().hashCode() * password.hashCode();
    }

    @Override
    public String toString() {
        return username;
    }

    public Set<Channel> getChannels() {
        return Set.copyOf(channels);
    }

    public void addChannel(final Channel channel) {
        channels.add(channel);
    }

    public void removeChannel(final Channel channel) {
        channels.remove(channel);
    }
}
