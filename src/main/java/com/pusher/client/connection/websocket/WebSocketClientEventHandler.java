package com.pusher.client.connection.websocket;

public interface WebSocketClientEventHandler {

	public abstract void onOpen();

	public abstract void onMessage(String message);

	public abstract void onClose(int code, String reason, boolean remote);

	public abstract void onError(Exception ex);
	
	public abstract void close();
	
	public abstract void connect();
	
	public abstract void send(String message);

}