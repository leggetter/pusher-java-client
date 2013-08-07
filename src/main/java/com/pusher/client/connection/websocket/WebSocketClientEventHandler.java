package com.pusher.client.connection.websocket;

import org.java_websocket.handshake.ServerHandshake;

public interface WebSocketClientEventHandler {

	public abstract void onOpen(ServerHandshake handshakedata);

	public abstract void onMessage(String message);

	public abstract void onClose(int code, String reason, boolean remote);

	public abstract void onError(Exception ex);
	
	public abstract void close();
	
	public abstract void connect();
	
	public abstract void send(String message);

}