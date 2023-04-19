package chat.model;

import java.util.*;

public class ChatChannel {

    private final String name;
    private final Set<User> users;
    private final List<String> messages;
    private final int maxMessageCount;

    public ChatChannel(final String name, final int maxMessageCount) {
        this.name = name;
        this.users = new HashSet<>();
        this.messages = new ArrayList<>();
        this.maxMessageCount = maxMessageCount;
    }

    public Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public synchronized boolean addUser(final User user) {
        if (users.size() < 10) {
            return users.add(user);
        }
        return false;
    }

    public synchronized void removeUser(final User user) {
        users.remove(user);
    }

    public synchronized void write(final User user, final String message) {
        users.stream().flatMap(u -> u.getChannels().stream())
                .forEach(ch -> ch.writeAndFlush(String.format("%s: %s%s", user, message, System.lineSeparator())));
        addMessage(user.getUsername() + ": " + message);
    }

    public synchronized void addMessage(final String message) {
        messages.add(message);
        if (messages.size() > maxMessageCount) {
            messages.remove(0);
        }
    }

    public String getName() {
        return name;
    }

    public synchronized List<String> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return name;
    }
}
