package com.pusher.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.impl.ChannelImpl;
import com.pusher.client.channel.impl.ChannelManager;
import com.pusher.client.channel.impl.PresenceChannelImpl;
import com.pusher.client.channel.impl.PrivateChannelImpl;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.impl.InternalConnection;
import com.pusher.client.util.Factory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Factory.class})
public class PusherTest {

    private static final String API_KEY = "123456";
    private static final String PUBLIC_CHANNEL_NAME = "my-channel";
    private static final String PRIVATE_CHANNEL_NAME = "private-my-channel";
    private static final String PRESENCE_CHANNEL_NAME = "presence-my-channel";
    
    private Pusher pusher;
    private @Mock PusherOptions mockPusherOptions;
    private @Mock Authorizer mockAuthorizer;
    private @Mock InternalConnection mockConnection;
    private @Mock ChannelManager mockChannelManager;
    private @Mock ConnectionEventListener mockConnectionEventListener;
    private @Mock ChannelImpl mockPublicChannel;
    private @Mock PrivateChannelImpl mockPrivateChannel;
    private @Mock PresenceChannelImpl mockPresenceChannel;
    private @Mock ChannelEventListener mockChannelEventListener;
    private @Mock PrivateChannelEventListener mockPrivateChannelEventListener;
    private @Mock PresenceChannelEventListener mockPresenceChannelEventListener;
    
    @Before
    public void setUp()
    {
	PowerMockito.mockStatic(Factory.class);
	when(Factory.getConnection(API_KEY)).thenReturn(mockConnection);
	when(Factory.getChannelManager(mockConnection, mockPusherOptions)).thenReturn(mockChannelManager);
	when(Factory.newPublicChannel(PUBLIC_CHANNEL_NAME)).thenReturn(mockPublicChannel);
	when(Factory.newPrivateChannel(mockConnection, PRIVATE_CHANNEL_NAME)).thenReturn(mockPrivateChannel);
	when(Factory.newPresenceChannel(mockConnection, PRESENCE_CHANNEL_NAME)).thenReturn(mockPresenceChannel);
	
	when(mockPusherOptions.getAuthorizer()).thenReturn(mockAuthorizer);
	
	this.pusher = new Pusher(API_KEY, mockPusherOptions);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullAPIKeyThrowsIllegalArgumentException() {
	new Pusher(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyAPIKeyThrowsIllegalArgumentException() {
	new Pusher("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullPusherOptionsThrowsIllegalArgumentException() {
	new Pusher(API_KEY, null);
    }
    
    @Test
    public void testCreatesConnectionObjectWhenConstructed() {
	assertNotNull(pusher.getConnection());
	assertSame(mockConnection, pusher.getConnection());
    }
    
    @Test
    public void testConnectCallWithNoListenerIsDelegatedToUnderlyingConnection() {
	pusher.connect();
	verify(mockConnection).connect();
    }
    
    @Test
    public void testDisconnectCallIsDelegatedToUnderlyingConnectionAndClearsSubscriptions() {
    	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
    	
    	pusher.disconnect();
    	verify(mockConnection).disconnect();
    	verify(mockChannelManager).clear();
    }
    
    @Test
    public void testDisconnectCallDoesNothingIfStateIsDisconnected() {
    	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
    	
    	pusher.disconnect();
    	verify(mockConnection, never()).disconnect();
    	verify(mockChannelManager, never()).clear();	
    }
    
    @Test
    public void testDisconnectCallDoesNothingIfStateIsConnecting() {
    	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
    	
    	pusher.disconnect();
    	verify(mockConnection, never()).disconnect();
    	verify(mockChannelManager, never()).clear();	
    }
    
    @Test
    public void testDisconnectCallDoesNothingIfStateIsDisconnecting() {
    	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTING);
    	
    	pusher.disconnect();
    	verify(mockConnection, never()).disconnect();
    	verify(mockChannelManager, never()).clear();	
    }
    
    @Test
    public void testConnectCallWithListenerAndEventsBindsListenerToEventsBeforeConnecting() {
	pusher.connect(mockConnectionEventListener, ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);
	
	verify(mockConnection).bind(ConnectionState.CONNECTED, mockConnectionEventListener);
	verify(mockConnection).bind(ConnectionState.DISCONNECTED, mockConnectionEventListener);
	verify(mockConnection).connect();
    }
    
    @Test
    public void testConnectCallWithListenerAndNoEventsBindsListenerToAllEventsBeforeConnecting() {
	pusher.connect(mockConnectionEventListener);
	
	verify(mockConnection).bind(ConnectionState.ALL, mockConnectionEventListener);
	verify(mockConnection).connect();
    }
    
    @Test
    public void testConnectCallWithNullListenerAndNoEventsJustConnectsWithoutBinding() {
	pusher.connect(null);
	
	verify(mockConnection, never()).bind(any(ConnectionState.class), any(ConnectionEventListener.class));
	verify(mockConnection).connect();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConnectCallWithNullListenerAndEventsThrowsException() {
	pusher.connect(null, ConnectionState.CONNECTED);
    }
    
    @Test
    public void testSubscribeCreatesPublicChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
	
	verify(mockChannelManager).subscribeTo(mockPublicChannel, mockChannelEventListener);
    }
    
    @Test
    public void testSubscribeWithEventNamesCreatesPublicChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener, "event1", "event2");
	
	verify(mockChannelManager).subscribeTo(mockPublicChannel, mockChannelEventListener, "event1", "event2");
    }

    @Test(expected=IllegalStateException.class)
    public void testSubscribeWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribeWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
    }
    
    @Test
    public void testSubscribePresenceCreatesPresenceChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener);
	
	verify(mockChannelManager).subscribeTo(mockPresenceChannel, mockPresenceChannelEventListener);
    }
    
    @Test
    public void testSubscribePresenceWithEventNamesCreatesPresenceChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener, "event1", "event2");
	
	verify(mockChannelManager).subscribeTo(mockPresenceChannel, mockPresenceChannelEventListener, "event1", "event2");
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePresenceIfNoPusherOptionsHaveBeenPassedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher = new Pusher(API_KEY);
	
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePresenceIfPusherOptionsHaveBeenPassedButNoAuthorizerHasBeenSetThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	when(mockPusherOptions.getAuthorizer()).thenReturn(null);
	pusher = new Pusher(API_KEY, mockPusherOptions);
	
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener);
    }

    @Test(expected=IllegalStateException.class)
    public void testSubscribePresenceWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePresenceWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.subscribePresence(PRESENCE_CHANNEL_NAME, mockPresenceChannelEventListener);
    }

    @Test
    public void testSubscribePrivateCreatesPrivateChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener);
	
	verify(mockChannelManager).subscribeTo(mockPrivateChannel, mockPrivateChannelEventListener);
    }
    
    @Test
    public void testSubscribePrivateWithEventNamesCreatesPrivateChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener, "event1", "event2");
	
	verify(mockChannelManager).subscribeTo(mockPrivateChannel, mockPrivateChannelEventListener, "event1", "event2");
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePrivateIfNoPusherOptionsHaveBeenPassedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher = new Pusher(API_KEY);
	
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePrivateIfPusherOptionsHaveBeenPassedButNoAuthorizerHasBeenSetThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	when(mockPusherOptions.getAuthorizer()).thenReturn(null);
	pusher = new Pusher(API_KEY, mockPusherOptions);
	
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener);
    }

    @Test(expected=IllegalStateException.class)
    public void testSubscribePrivateWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribePrivateWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.subscribePrivate(PRIVATE_CHANNEL_NAME, mockPrivateChannelEventListener);
    }
    
    @Test
    public void testUnsubscribeDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
	verify(mockChannelManager).unsubscribeFrom(PUBLIC_CHANNEL_NAME);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testUnsubscribeWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testUnsubscribeWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
    }
}