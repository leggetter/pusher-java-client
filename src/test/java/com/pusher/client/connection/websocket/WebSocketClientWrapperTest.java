package com.pusher.client.connection.websocket;

import static org.mockito.Mockito.verify;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.java_websocket.handshake.ServerHandshake;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketClientWrapperTest {
    
    private WebSocketClientEventHandler wrapper;
    private @Mock WebSocketListener mockProxy;
    
    @Before
    public void setUp() throws URISyntaxException, SSLException {
	wrapper = new WebSocketClientWrapper(new URI("http://www.test.com"), mockProxy);
    }
    
    @Test
    @Ignore
    public void testOnOpenCallIsDelegatedToTheProxy() {
	wrapper.onOpen();
	verify(mockProxy).onOpen();
    }
    
    @Test
    @Ignore
    public void testOnMessageIsDelegatedToTheProxy() {
	wrapper.onMessage("hello");
	verify(mockProxy).onMessage("hello");
    }
    
    @Test
    @Ignore
    public void testOnCloseIsDelegatedToTheProxy() {
	wrapper.onClose(1, "reason", true);
	verify(mockProxy).onClose(1, "reason", true);
    }
    
    @Test
    @Ignore
    public void testOnErrorIsDelegatedToTheProxy() {
	Exception e = new Exception();
	wrapper.onError(e);
	verify(mockProxy).onError(e);
    }
}