package com.pusher.client.connection.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.pusher.client.channel.InternalChannel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.Factory;
import com.pusher.client.util.InstantExecutor;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Factory.class})
public class WebsocketConnectionTest {

    private static final String API_KEY = "123456";
    private static final String CHANNEL_NAME = "my-channel";
    private static final String SUBSCRIBE_MESSAGE = "{\"event\":\"pusher:subscribe\"}";
    
    private WebsocketConnection connection;
    private @Mock WebSocketClientWrapper mockUnderlyingConnection;
    private @Mock ConnectionEventListener mockEventListener;
    private @Mock InternalChannel mockInternalChannel;
    
    @Before
    public void setUp() throws URISyntaxException {
	
	PowerMockito.mockStatic(Factory.class);
	when(Factory.newWebSocketClientWrapper(any(URI.class), any(WebsocketConnection.class))).thenReturn(mockUnderlyingConnection);
	when(Factory.getEventQueue()).thenReturn(new InstantExecutor());
	
	when(mockInternalChannel.getName()).thenReturn(CHANNEL_NAME);
	when(mockInternalChannel.toSubscriptionMessage()).thenReturn(SUBSCRIBE_MESSAGE);
	
	this.connection = new WebsocketConnection(API_KEY);
	this.connection.setEventListener(mockEventListener);
    }
    
    @Test
    public void testStartsInDisconnectedState() {
	assertSame(ConnectionState.DISCONNECTED, connection.getState());
    }
    
    @Test
    public void testConnectCallIsDelegatedToUnderlyingConnection() {
	connection.connect();
	verify(mockUnderlyingConnection).connect();
    }
    
    @Test
    public void testConnectUpdatesStateAndNotifiesListener() {
	connection.connect();
	verify(mockEventListener).onConnectionStateChange(new ConnectionStateChange(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
	assertEquals(ConnectionState.CONNECTING, connection.getState());
    }
    
    @Test
    public void testConnectDoesNotCallConnectOnUnderlyingConnectionIfAlreadyInConnectingState() {
	connection.connect();
	connection.connect();
	
	verify(mockUnderlyingConnection, times(1)).connect();
	verify(mockEventListener, times(1)).onConnectionStateChange(any(ConnectionStateChange.class));
    }
    
    @Test
    public void testConnectionEstablishedMessageIsTranslatedToAConnectedCallback() {
	connection.connect();
	verify(mockEventListener).onConnectionStateChange(new ConnectionStateChange(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
	
	connection.onMessage("{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"21112.816204\\\"}\"}");
	verify(mockEventListener).onConnectionStateChange(new ConnectionStateChange(ConnectionState.CONNECTING, ConnectionState.CONNECTED));
	
	assertEquals(ConnectionState.CONNECTED, connection.getState());
    }
    
    @Test
    public void testConnectionEstablishedMessageWhenAlreadyConnectedIsIgnored() {
	connection.connect();
	connection.onMessage("{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"21112.816204\\\"}\"}");
	connection.onMessage("{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"21112.816204\\\"}\"}");
	
	verify(mockEventListener, times(2)).onConnectionStateChange(any(ConnectionStateChange.class));
    }
    
    @Test
    public void testConnectionErrorMessageIsTranslatedToADisconnectedCallback() {
	connection.connect();
	verify(mockEventListener).onConnectionStateChange(new ConnectionStateChange(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
	
	connection.onMessage("{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"21112.816204\\\"}\"}");
	verify(mockEventListener).onConnectionStateChange(new ConnectionStateChange(ConnectionState.CONNECTING, ConnectionState.CONNECTED));
	
	assertEquals(ConnectionState.CONNECTED, connection.getState());	
    }
    
    @Test
    public void testSubscribeSendsSubscriptionMessageToPusherAndNotifiesChannel() {
	connect();
	
	connection.subscribeTo(mockInternalChannel);
	
	verify(mockUnderlyingConnection).send(SUBSCRIBE_MESSAGE);
	verify(mockInternalChannel).subscribeSent();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSubscribeTwiceToTheSameChannelNameThrowsException() {
	connect();
	
	connection.subscribeTo(mockInternalChannel);
	connection.subscribeTo(mockInternalChannel);
    }
    
    @Test
    public void testReceiveMessagePassesMessageToChannel() {
	connect();
	
	connection.subscribeTo(mockInternalChannel);
	connection.onMessage("{\"event\":\"my-event\",\"channel\":\"" + CHANNEL_NAME + "\",\"data\":{\"fish\":\"chips\"}}");
	
	verify(mockInternalChannel).onMessage("my-event", "{\"event\":\"my-event\",\"channel\":\"" + CHANNEL_NAME + "\",\"data\":{\"fish\":\"chips\"}}");
    }
    
    @Test
    public void testReceiveMessageDiscardsMessageIfNoChannelCanBeFound() {
	connect();
	
	connection.onMessage("{\"event\":\"my-event\",\"channel\":\"" + CHANNEL_NAME + "\",\"data\":{\"fish\":\"chips\"}}");
	
	verify(mockInternalChannel, never()).onMessage(anyString(), anyString());
    }    
    
    /* end of tests */
    
    private void connect() {
	connection.connect();
	connection.onMessage("{\"event\":\"pusher:connection_established\",\"data\":\"{\\\"socket_id\\\":\\\"21112.816204\\\"}\"}");	
    }
}