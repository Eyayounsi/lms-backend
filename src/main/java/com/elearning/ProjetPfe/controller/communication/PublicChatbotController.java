package com.elearning.ProjetPfe.controller.communication;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.chatbot.AiAgentRequest;
import com.elearning.ProjetPfe.dto.chatbot.AiAgentResponse;
import com.elearning.ProjetPfe.dto.chatbot.ChatbotMessageRequest;
import com.elearning.ProjetPfe.dto.chatbot.ChatbotMessageResponse;
import com.elearning.ProjetPfe.service.communication.PublicChatbotService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/public/chatbot")
public class PublicChatbotController {

    private final PublicChatbotService publicChatbotService;

    public PublicChatbotController(PublicChatbotService publicChatbotService) {
        this.publicChatbotService = publicChatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatbotMessageResponse> message(@Valid @RequestBody ChatbotMessageRequest request) {
        String reply = publicChatbotService.generateReply(request.getMessage());
        return ResponseEntity.ok(new ChatbotMessageResponse(reply));
    }

    @PostMapping("/visitor-support")
    public ResponseEntity<AiAgentResponse> visitorSupport(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.visitorSupport(request));
    }
}
