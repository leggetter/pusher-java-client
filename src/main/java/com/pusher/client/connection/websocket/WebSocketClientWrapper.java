package com.pusher.client.connection.websocket;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver;
import de.tavendo.autobahn.WebSocketException;

/**
 * A thin wrapper around the WebSocketClient class from the Java-WebSocket
 * library. The purpose of this class is to enable the WebSocketConnection class
 * to be unit tested by swapping out an instance of this wrapper for a mock
 * version.
 */
public class WebSocketClientWrapper
	implements WebSocketClientEventHandler {

	private final WebSocketListener proxy;
	
	private final de.tavendo.autobahn.WebSocketConnection connection;
	private URI uri;

	public WebSocketClientWrapper(URI uri, WebSocketListener proxy)
			throws SSLException {
		connection = new de.tavendo.autobahn.WebSocketConnection();

		this.uri = uri;
		this.proxy = proxy;
	}
	
	@Override
	public void close() {
		this.connection.disconnect();
	}
	
	@Override
	public void connect() {

		try {
			this.connection.connect(this.uri, new WebSocketConnectionObserver() {

				@Override
				public void onOpen() {
					WebSocketClientWrapper.this.onOpen();
				}

				@Override
				public void onClose(WebSocketCloseNotification code, String reason) {
					// TODO: any way of determining if the remote/server closed the connection?
					WebSocketClientWrapper.this.onClose(code.ordinal(), reason, false);
					
				}

				@Override
				public void onTextMessage(String payload) {
					WebSocketClientWrapper.this.onMessage(payload);
				}

				@Override
				public void onRawTextMessage(byte[] payload) {
					// not supported
					// TODO: throw exception
				}

				@Override
				public void onBinaryMessage(byte[] payload) {
					// not supported
					// TODO: throw exception
				}
				
			});
		} catch (WebSocketException e) {
			// TODO Auto-generated catch block
			// call onException
			e.printStackTrace();
		}
	}
	
	@Override
	public void send(String message) {
		this.connection.sendTextMessage(message);
	}

	@Override
	public void onOpen() {
		proxy.onOpen();
	}

	@Override
	public void onMessage(String message) {
		proxy.onMessage(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		proxy.onClose(code, reason, remote);
	}

	@Override
	public void onError(Exception ex) {
		proxy.onError(ex);
	}
}