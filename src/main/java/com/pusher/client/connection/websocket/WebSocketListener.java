package com.pusher.client.connection.websocket;

public interface WebSocketListener {

    void onOpen();
    
    void onMessage(String message);
    
    void onClose(int code, String reason, boolean remote);
    
    void onError(Exception ex);
}