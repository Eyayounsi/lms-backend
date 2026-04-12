package com.elearning.ProjetPfe.controller.communication;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.chatbot.AiAgentRequest;
import com.elearning.ProjetPfe.dto.chatbot.AiAgentResponse;
import com.elearning.ProjetPfe.service.communication.PublicChatbotService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/ai/chatbot")
public class AiChatbotController {

    private final PublicChatbotService publicChatbotService;

    public AiChatbotController(PublicChatbotService publicChatbotService) {
        this.publicChatbotService = publicChatbotService;
    }

    @PostMapping("/student/tutor")
    public ResponseEntity<AiAgentResponse> studentTutor(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.studentTutor(request));
    }

    @PostMapping("/instructor/quiz")
    public ResponseEntity<AiAgentResponse> instructorQuiz(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorQuiz(request));
    }

    @PostMapping("/instructor/plan")
    public ResponseEntity<AiAgentResponse> instructorPlan(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorPlan(request));
    }

    @PostMapping("/instructor/copilot")
    public ResponseEntity<AiAgentResponse> instructorCopilot(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.instructorCopilot(request));
    }

    @PostMapping("/admin/copilot")
    public ResponseEntity<AiAgentResponse> adminCopilot(@Valid @RequestBody AiAgentRequest request) {
        return ResponseEntity.ok(publicChatbotService.adminCopilot(request));
    }
}
