package org.rap.algotutorbe.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.dto.AiChatResponse;
import org.rap.algotutorbe.ai.services.AiChatService;
import org.rap.algotutorbe.common.handler.GeneralExceptionHandler;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.springframework.test.web.servlet.MvcResult;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private MessageSource messageSource;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        AiChatController controller = new AiChatController(aiChatService, rateLimiter, 20, 60);
        GeneralExceptionHandler exceptionHandler = new GeneralExceptionHandler(messageSource);
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(SecurityUser.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        SecurityUser mockSecurityUser = mock(SecurityUser.class);
                        lenient().when(mockSecurityUser.getId()).thenReturn(testUserId);
                        return mockSecurityUser;
                    }
                })
                .build();
    }

    @Test
    void chat_shouldReturnOkWhenAllowed() throws Exception {
        AiChatRequest request = new AiChatRequest(
                null, 1L, null, "GEMINI", "HINT", "I need help", null, null, null, null, Collections.emptyList()
        );

        AiChatResponse response = new AiChatResponse(
                UUID.randomUUID(), "Here is help", "HINT", Collections.emptyList(), Collections.emptyList(), true
        );

        when(rateLimiter.isAllowed(eq("ai-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(true);
        when(aiChatService.chat(any(AiChatRequest.class), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("Here is help"))
                .andExpect(jsonPath("$.data.canAskNextHint").value(true));
    }

    @Test
    void chat_shouldReturn429WhenRateLimited() throws Exception {
        AiChatRequest request = new AiChatRequest(
                null, 1L, null, "GEMINI", "HINT", "I need help", null, null, null, null, Collections.emptyList()
        );

        when(rateLimiter.isAllowed(eq("ai-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(false);
        when(rateLimiter.getRetryAfterSeconds(eq("ai-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(45L);

        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"))
                .andExpect(jsonPath("$.code").value(8004));
    }

    @Test
    void chatStream_shouldReturnOkWhenAllowed() throws Exception {
        AiChatRequest request = new AiChatRequest(
                null, 1L, null, "GEMINI", "HINT", "I need help", null, null, null, null, Collections.emptyList()
        );

        when(rateLimiter.isAllowed(eq("ai-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(true);
        doAnswer(invocation -> {
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = invocation.getArgument(2);
            emitter.complete();
            return null;
        }).when(aiChatService).chatStream(any(AiChatRequest.class), eq(testUserId), any());

        MvcResult mvcResult = mockMvc.perform(post("/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    void generalChatStream_shouldReturnOkWhenAllowed() throws Exception {
        AiChatRequest request = new AiChatRequest(
                null, null, null, "GEMINI", "EXPLAIN", "Tư vấn lộ trình học", null, null, null, null, Collections.emptyList()
        );

        when(rateLimiter.isAllowed(eq("ai-general-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(true);
        when(aiChatService.getRoadmapsForAdvisory()).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = invocation.getArgument(2);
            emitter.complete();
            return null;
        }).when(aiChatService).generalChatStream(any(AiChatRequest.class), eq(testUserId), any(), any());

        MvcResult mvcResult = mockMvc.perform(post("/ai/general/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    void generalChat_shouldReturnOkWhenAllowed() throws Exception {
        AiChatRequest request = new AiChatRequest(
                null, null, null, "GEMINI", "EXPLAIN", "Tư vấn lộ trình học", null, null, null, null, Collections.emptyList()
        );

        org.rap.algotutorbe.ai.dto.AiGeneralChatResponse response = new org.rap.algotutorbe.ai.dto.AiGeneralChatResponse(
                UUID.randomUUID(), "Here is roadmaps advisory.", Collections.emptyList()
        );

        when(rateLimiter.isAllowed(eq("ai-general-chat:" + testUserId), eq(20), eq(60000L))).thenReturn(true);
        when(aiChatService.getRoadmapsForAdvisory()).thenReturn(Collections.emptyList());
        when(aiChatService.generalChat(any(AiChatRequest.class), eq(testUserId), any())).thenReturn(response);

        mockMvc.perform(post("/ai/general/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("Here is roadmaps advisory."));
    }
}
