package com.example.backend.ai;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiJsonHelperTest {

    private AIProvider provider;
    private AiJsonHelper helper;

    @BeforeEach
    void setUp() {
        provider = mock(AIProvider.class);
        helper = new AiJsonHelper(provider, new ObjectMapper());
    }

    @Test
    void parsesStructuredJson() {
        givenAiContent("{\"answer\":\"went home\",\"score\":90}");

        JsonResult result = helper.requestJson("system", "user", 0.2, JsonResult.class);

        assertEquals("went home", result.answer);
        assertEquals(90, result.score);
    }

    @Test
    void stripsMarkdownJsonFence() {
        givenAiContent("```json\n{\"answer\":\"went home\",\"score\":90}\n```");

        JsonResult result = helper.requestJson("system", "user", 0.2, JsonResult.class);

        assertEquals("went home", result.answer);
        assertEquals(90, result.score);
    }

    @Test
    void rejectsMalformedJsonInsteadOfReturningRawText() {
        givenAiContent("The learner should use the past tense.");

        assertThrows(AIProviderException.class,
                () -> helper.requestJson("system", "user", 0.2, JsonResult.class));
    }

    @Test
    void rejectsMissingRequiredValues() {
        givenAiContent("{\"answer\":null,\"score\":90}");

        assertThrows(AIProviderException.class,
                () -> helper.requestJson("system", "user", 0.2, JsonResult.class));
    }

    private void givenAiContent(String content) {
        when(provider.chat(any())).thenReturn(AiChatResponse.builder()
                .content(content)
                .model("test-model")
                .build());
    }

    static class JsonResult {
        public String answer;
        public Integer score;
    }
}
