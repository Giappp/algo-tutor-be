package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.dto.AiStreamDoneResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSseEventServiceTest {

    private final AiSseEventService aiSseEventService = new AiSseEventService();

    @Test
    void completeSuccessfully_shouldSendDoneEventBeforeCompletingEmitter() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();

        aiSseEventService.completeSuccessfully(emitter);

        assertThat(emitter.eventData())
                .extracting(ResponseBodyEmitter.DataWithMediaType::getData)
                .contains("event:done\ndata:", new AiStreamDoneResponse(true));
        assertThat(emitter.completed()).isTrue();
    }

    @Test
    void sendMetadata_shouldFailFastWhenEmitterCannotSend() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("client disconnected"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        assertThatThrownBy(() -> aiSseEventService.sendMetadata(emitter, "metadata"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("metadata");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void completeWithError_shouldSendDoneBeforeCompletingWithError() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        RuntimeException error = new RuntimeException("provider failed");

        aiSseEventService.completeWithError(emitter, "stream failed", error);

        assertThat(emitter.eventData())
                .extracting(ResponseBodyEmitter.DataWithMediaType::getData)
                .contains("event:done\ndata:", new AiStreamDoneResponse(true));
        assertThat(emitter.completedWithError()).isSameAs(error);
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private Set<ResponseBodyEmitter.DataWithMediaType> eventData;
        private boolean completed;
        private Throwable completedWithError;

        @Override
        public void send(SseEventBuilder builder) {
            eventData = builder.build();
        }

        @Override
        public void complete() {
            completed = true;
        }

        @Override
        public void completeWithError(Throwable error) {
            completedWithError = error;
        }

        Set<ResponseBodyEmitter.DataWithMediaType> eventData() {
            return eventData;
        }

        boolean completed() {
            return completed;
        }

        Throwable completedWithError() {
            return completedWithError;
        }
    }
}
