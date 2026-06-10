package org.rap.algotutorbe.ai.services;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChunkResponse;
import org.rap.algotutorbe.ai.dto.AiStreamDoneResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@Service
public class AiSseEventService {

    public static final String MESSAGE_EVENT = "message";
    public static final String METADATA_EVENT = "metadata";
    public static final String DONE_EVENT = "done";

    public void sendMessage(SseEmitter emitter, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        send(emitter, MESSAGE_EVENT, new AiChunkResponse(chunk));
    }

    public void sendMetadata(SseEmitter emitter, Object metadata) {
        send(emitter, METADATA_EVENT, metadata);
    }

    public void completeSuccessfully(SseEmitter emitter) {
        sendDone(emitter);
        emitter.complete();
    }

    public void completeWithError(SseEmitter emitter, String logMessage, Throwable error) {
        log.error(logMessage, error);
        try {
            sendDone(emitter);
        } catch (RuntimeException sendError) {
            log.debug("Could not send terminal SSE event after stream error", sendError);
        }
        emitter.completeWithError(error);
    }

    public void registerLifecycle(SseEmitter emitter, Disposable subscription) {
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(error -> subscription.dispose());
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to send SSE event: " + eventName, e);
        }
    }

    private void sendDone(SseEmitter emitter) {
        send(emitter, DONE_EVENT, new AiStreamDoneResponse(true));
    }
}
