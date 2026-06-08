package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to handle transactional persistence of AI messages.
 * Uses REQUIRES_NEW propagation to ensure messages are saved in their own transactions,
 * which is safe when called from asynchronous reactive streams (Reactor).
 */
@Service
@RequiredArgsConstructor
public class AiMessagePersister {

    private final AiMessageRepository aiMessageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage saveMessage(UUID conversationId, UUID userId, AiMessageRole role,
                                 String content, String mode, Integer tokenInput, Integer tokenOutput) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setMode(mode);
        message.setTokenInput(tokenInput);
        message.setTokenOutput(tokenOutput);
        return aiMessageRepository.save(message);
    }
}
