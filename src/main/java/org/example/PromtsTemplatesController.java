package org.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/apis/")
public class PromtsTemplatesController {
    ChatClient chatClient;
    private final Resource userMsgResource;
    private final Resource systemMsgResource;

    public PromtsTemplatesController(ChatClient.Builder builder, @Value("classpath:/prompts/user-message-1.st") Resource userMsgResource, @Value("classpath:/prompts/system-message.st") Resource systemMsgResource, ChatMemory chatMemory) {
        this.chatClient = builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor()).build(

        );
        this.userMsgResource = userMsgResource;
        this.systemMsgResource = systemMsgResource;
    }

    @GetMapping("/listOfCities")
    List<String> getCityName(String country) {
        ListOutputConverter converter = new ListOutputConverter();
        PromptTemplate promptTemplate = new PromptTemplate(userMsgResource);
        Map<String, Object> vars = Map.of("country", country, "format", converter.getFormat());
        Message message = promptTemplate.createMessage(vars);

        return converter.convert(chatClient.prompt().system(systemMsgResource).messages(message).call().content());
    }

    @GetMapping("/chat")
    ResponseEntity<Output> chat(String input,
                                @CookieValue(name = "X-CONV-ID", required = false) String convId) {
        String conversationId = convId == null ? UUID.randomUUID().toString() : convId;
        var response = this.chatClient.prompt()
                .user(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().content();
        ResponseCookie cookie = ResponseCookie.from("X-CONV-ID", conversationId)
                .path("/")
                .maxAge(3600)
                .build();
        
        Output output = new Output(response);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(output);
    }

    // record Input(String prompt) {}
    record Output(String content) {
    }
}
