package chat.handler;

import chat.data.LocalStorage;
import chat.server.ChatServerInitializer;
import chat.service.ChatService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatHandlerIT {

    private static final int PORT = 8080;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @BeforeEach
    void setUp() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChatServerInitializer());
        serverChannel = b.bind(PORT).sync().channel();
    }

    @AfterEach
    void tearDown() throws Exception {
        serverChannel.close().sync();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Test
    void testRegisterNewUser() throws Exception {
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), new LocalStorage()));
        clientChannel.writeInbound("/login name pass");
        final var loginResponse = clientChannel.readOutbound();
        assertEquals(String.format("%s%s", "Welcome name.", System.lineSeparator()), loginResponse);
        clientChannel.close().sync();
    }

    @Test
    void testJoinNewChannel() throws Exception {
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), new LocalStorage()));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join test");
        final var loginResponse = clientChannel.readOutbound();
        assertEquals(String.format("%s%s", "New channel test has been created.",
                System.lineSeparator()), loginResponse);
        clientChannel.close().sync();
    }

    @Test
    void testJoinMaxCapacityChannel() throws Exception {
        final var storage = new LocalStorage();
        final var channels = new ArrayList<EmbeddedChannel>();
        for (int i = 0; i < 10; i++) {
            final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
            clientChannel.writeInbound("/login " + i + " pass");
            clientChannel.writeInbound("/join test");
            channels.add(clientChannel);
        }
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login 11 pass");
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join test");
        final var loginResponse = clientChannel.readOutbound();
        assertEquals(String.format("%s%s", "The channel you've tried to connect is at max capacity.",
                System.lineSeparator()), loginResponse);
        channels.forEach(ch -> {
            try {
                ch.close().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        clientChannel.close().sync();
    }

    @Test
    void testJoinChannelAfterClientClose() throws Exception {
        final var storage = new LocalStorage();
        for (int i = 0; i < 10; i++) {
            final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
            clientChannel.writeInbound("/login " + i + " pass");
            clientChannel.writeInbound("/join test");
            clientChannel.close().sync();
        }
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login 11 pass");
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join test");
        final var loginResponse = clientChannel.readOutbound();
        assertEquals(String.format("%s%s", "You've joined channel test.", System.lineSeparator()), loginResponse);
        clientChannel.close().sync();
    }

    @Test
    void testUserRejoinAfterClientClose() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.writeInbound("/join test");
        clientChannel.close().sync();
        final var newClientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        newClientChannel.writeInbound("/login name pass");
        newClientChannel.readOutbound();
        final var loginResponse = newClientChannel.readOutbound();
        assertEquals(String.format("%s%s", "You've joined channel test.", System.lineSeparator()), loginResponse);
        newClientChannel.close().sync();
    }

    @Test
    void testUserDoesNotRejoinIfChannelIsFull() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.writeInbound("/join test");
        clientChannel.close().sync();
        final var channels = new ArrayList<EmbeddedChannel>();
        for (int i = 0; i < 10; i++) {
            final var newClientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
            newClientChannel.writeInbound("/login " + i + " pass");
            newClientChannel.writeInbound("/join test");
            channels.add(newClientChannel);
        }
        final var newClientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        newClientChannel.writeInbound("/login name pass");
        newClientChannel.readOutbound();
        final var loginResponse = newClientChannel.readOutbound();
        assertNull(loginResponse);
        channels.forEach(ch -> {
            try {
                ch.close().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        newClientChannel.close().sync();
    }

    @Test
    void testListReturnsAllChannels() throws Exception {
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), new LocalStorage()));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join ch1");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join ch2");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel.writeInbound("/join ch3");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel.writeInbound("/list");
        final var expected = List.of(String.format("%s%s", "ch1", System.lineSeparator()),
                String.format("%s%s", "ch2", System.lineSeparator()),
                String.format("%s%s", "ch3", System.lineSeparator()));
        final List<String> actual = List.of(clientChannel.readOutbound(),
                clientChannel.readOutbound(),
                clientChannel.readOutbound());
        assertTrue(expected.containsAll(actual));
        clientChannel.close().sync();
    }

    @Test
    void testOneUserFromMultipleClients() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/login name pass");
        clientChannel2.readOutbound();
        clientChannel.writeInbound("/join test");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel.writeInbound("Hello!");
        final var response1 = clientChannel2.readOutbound();
        clientChannel2.writeInbound("World!");
        clientChannel.readOutbound();
        final var response2 = clientChannel.readOutbound();
        assertAll(
                () -> assertEquals(String.format("%s%s", "name: Hello!", System.lineSeparator()), response1),
                () -> assertEquals(String.format("%s%s", "name: World!", System.lineSeparator()), response2));
        clientChannel.close().sync();
        clientChannel2.close().sync();
    }

    @Test
    void testLeaveMessageIsBroadcasted() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/login name2 pass");
        clientChannel2.readOutbound();
        clientChannel.writeInbound("/join test");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/join test");
        clientChannel2.readOutbound();
        clientChannel.writeInbound("/leave");
        final var loginResponse = clientChannel2.readOutbound();
        assertEquals(String.format("%s%s", "User name has left the channel.", System.lineSeparator()), loginResponse);
        clientChannel.close().sync();
        clientChannel2.close().sync();
    }

    @Test
    void testDisconnectLeaveMessageIsBroadcasted() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/login name2 pass");
        clientChannel2.readOutbound();
        clientChannel.writeInbound("/join test");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/join test");
        clientChannel2.readOutbound();
        clientChannel.close().sync();
        final var loginResponse = clientChannel2.readOutbound();
        assertEquals(String.format("%s%s", "User name has left the channel.", System.lineSeparator()), loginResponse);
        clientChannel2.close().sync();
    }

    @Test
    void testUserConnectedFromTwoChannelsDisconnectingInOneWillNotSendDisconnectMessage() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel3 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.readOutbound();
        clientChannel2.writeInbound("/login name pass");
        clientChannel2.readOutbound();
        clientChannel3.writeInbound("/login name2 pass");
        clientChannel3.readOutbound();
        clientChannel.writeInbound("/join test");
        clientChannel.readOutbound();
        clientChannel.readOutbound();
        clientChannel3.writeInbound("/join test");
        clientChannel3.readOutbound();
        clientChannel.close().sync();
        final var loginResponse = clientChannel3.readOutbound();
        assertNull(loginResponse);
        clientChannel2.close().sync();
        clientChannel3.close().sync();
    }

    @Test
    void testUserReceiveOldMessagesWhenJoinChannel() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.writeInbound("/join test");
        clientChannel.writeInbound("1");
        clientChannel.writeInbound("2");
        clientChannel.writeInbound("3");
        clientChannel2.writeInbound("/login name2 pass");
        clientChannel2.readOutbound();
        clientChannel2.writeInbound("/join test");
        clientChannel2.readOutbound();
        assertAll(
                () -> assertEquals(String.format("%s%s", "name: 1", System.lineSeparator()),
                        clientChannel2.readOutbound()),
                () -> assertEquals(String.format("%s%s", "name: 2", System.lineSeparator()),
                        clientChannel2.readOutbound()),
                () -> assertEquals(String.format("%s%s", "name: 3", System.lineSeparator()),
                        clientChannel2.readOutbound()));
        clientChannel.close().sync();
        clientChannel2.close().sync();
    }

    @Test
    void testUsersShowsAllUsersInChannel() throws Exception {
        final var storage = new LocalStorage();
        final var clientChannel = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel2 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        final var clientChannel3 = new EmbeddedChannel(new ChatHandler(new ChatService(), storage));
        clientChannel.writeInbound("/login name pass");
        clientChannel.writeInbound("/join test");
        clientChannel2.writeInbound("/login name2 pass");
        clientChannel2.writeInbound("/join test");
        clientChannel3.writeInbound("/login name3 pass");
        clientChannel3.writeInbound("/join test");
        clientChannel3.readOutbound();
        clientChannel3.readOutbound();
        clientChannel3.writeInbound("/users");
        final var expected = List.of(String.format("%s%s", "name", System.lineSeparator()),
                String.format("%s%s", "name2", System.lineSeparator()),
                String.format("%s%s", "name3", System.lineSeparator()));
        final List<String> actual = List.of(clientChannel3.readOutbound(),
                clientChannel3.readOutbound(),
                clientChannel3.readOutbound());
        assertTrue(expected.containsAll(actual));
        clientChannel.close().sync();
        clientChannel2.close().sync();
        clientChannel3.close().sync();
    }
}
