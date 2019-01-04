package com.computools.pubcam.eventmanager.service;

import com.computools.pubcam.core.protocol.message.PubCamMessage;
import com.computools.pubcam.core.protocol.message.SubscribeMessage;
import com.computools.pubcam.core.services.context.EventStreamContext;
import com.computools.pubcam.core.services.stream.Streamer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.computools.pubcam.core.util.WebSocketAttributes.LAST_SENT_TIME;
import static com.computools.pubcam.core.util.WebSocketAttributes.TRACK_ID;

public class EventManagerStreamer implements Streamer {
    private final Logger logger = LoggerFactory.getLogger(EventManagerStreamer.class);

    private final EventStreamContext streamContext;
    private final ObjectMapper objectMapper;

    @Async
    public void stream(String cameraID, byte [] content) {
        streamContext.getSubscribers(cameraID).forEach(session -> {
            try {
                sendMessage(session, content, cameraID);
            } catch (IOException e) {
                logger.error("Cant sent frame  to " + session.toString(), e);
            }
        });
    }

    @Override
    public void unSubscribe(String cameraId) {
        streamContext.getSubscribers(cameraId)
                .forEach(webSocketSession -> {
                    try {
                        sendUnSubscribe(webSocketSession, cameraId);
                    } catch (IOException e) {
                        logger.error("Cant sent unsibscribe message to " + webSocketSession, e );
                    }
                });
    }



    @SuppressWarnings("unchecked")
    private void sendMessage(WebSocketSession session, byte[] currentFrame, String cameraID) throws IOException {
        int offset = 0;
        int length = 8196;
        boolean isLast = false;
        boolean isFirst = true;

        while (offset < currentFrame.length) {
            if ((offset + length) >= currentFrame.length) {
                length = currentFrame.length - offset;
                isLast = true;
            }

            byte[] buffer = new byte[length];
            if (isFirst) {
                System.arraycopy(currentFrame, offset, buffer, 1, length - 1);
                isFirst = false;
                Map<String, Byte> tracks =  (Map<String, Byte>)session.getAttributes().get(TRACK_ID);
                buffer[0] = tracks.get(cameraID);
                offset -= 1;
            } else {
                System.arraycopy(currentFrame, offset, buffer, 0, length);
            }

            if (session.isOpen()) {
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(buffer), isLast));
            } else {
                session.close(CloseStatus.GOING_AWAY);
            }
            offset += length;
        }
        if (session.isOpen()){
            session.sendMessage(new PingMessage());
            markSentFrame(session);
        }
    }

    private void markSentFrame(WebSocketSession session){
        session.getAttributes().remove(LAST_SENT_TIME);
        session.getAttributes().put(LAST_SENT_TIME, System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    private void sendUnSubscribe(WebSocketSession webSocketSession, String cameraId) throws IOException {
        SubscribeMessage subscribeMessage = new SubscribeMessage();
        subscribeMessage.setCameraId(cameraId);
        subscribeMessage.setMessageId(PubCamMessage.UN_SUBSCRIBE_MESSAGE_ID);
        Map<String, Byte> tracks =  (Map<String, Byte>)webSocketSession.getAttributes().get(TRACK_ID);
        subscribeMessage.setTrackId(String.valueOf(tracks.get(cameraId)));
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(subscribeMessage)));
    }


    public EventManagerStreamer(EventStreamContext streamContext, ObjectMapper objectMapper) {
        this.streamContext = streamContext;
        this.objectMapper = objectMapper;
    }
}
