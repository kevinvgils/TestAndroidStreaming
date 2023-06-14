package com.example.finalfinal;//package com.example.finalfinal;
//
//import java.net.URI;
//import java.nio.ByteBuffer;
//
//import javax.websocket.ClientEndpoint;
//import javax.websocket.CloseReason;
//import javax.websocket.ContainerProvider;
//import javax.websocket.OnClose;
//import javax.websocket.OnMessage;
//import javax.websocket.OnOpen;
//import javax.websocket.Session;
//import javax.websocket.WebSocketContainer;
//
//@ClientEndpoint
//public class WebSocketClientEndpointTest {
//
//    Session userSession = null;
//    private MessageHandler messageHandler;
//
//    public WebSocketClientEndpointTest(URI endpointURI) {
//        try {
//            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//            container.connectToServer(this, endpointURI);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    @OnOpen
//    public void onOpen(Session userSession) {
//        System.out.println("opening websocket");
//        this.userSession = userSession;
//    }
//
//
//    @OnClose
//    public void onClose(Session userSession, CloseReason reason) {
//        System.out.println("closing websocket");
//        this.userSession = null;
//    }
//
//
//    @OnMessage
//    public void onMessage(String message) {
//        if (this.messageHandler != null) {
//            this.messageHandler.handleMessage(message);
//        }
//    }
//
//    @OnMessage
//    public void onMessage(ByteBuffer bytes) {
//        System.out.println("Handle byte buffer");
//    }
//
//    public void addMessageHandler(MessageHandler msgHandler) {
//        this.messageHandler = msgHandler;
//    }
//
//    public void sendMessage(String message) {
//        this.userSession.getAsyncRemote().sendText(message);
//    }
//
//    public void sendFrame(byte[] frame) {
//        this.userSession.getAsyncRemote().sendBinary(ByteBuffer.wrap(frame));
//    }
//
//    public static interface MessageHandler {
//
//        public void handleMessage(String message);
//    }
//}
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

@ClientEndpoint
public class WebSocketClientEndpointTest {

    Session userSession = null;
    RemoteEndpoint.Async remoteEndpoint = null;
    private MessageHandler messageHandler;

    public WebSocketClientEndpointTest(URI endpointURI) {
        try {
            ClientManager client = ClientManager.createClient();
            client.asyncConnectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
        System.out.println(this.userSession);
        this.remoteEndpoint = userSession.getAsyncRemote();
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        this.userSession = null;
        try {
            userSession.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer bytes) {
        System.out.println("Handle byte buffer");
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        System.out.println(message);
        this.remoteEndpoint.sendText(message);
        System.out.println(this.userSession);
    }

    public void sendFrame(byte[] frame) {
        this.userSession.getAsyncRemote().sendBinary(ByteBuffer.wrap(frame));
    }

    public static interface MessageHandler {
        public void handleMessage(String message);
    }
}
