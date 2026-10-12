package com.mailscale.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateServiceRenderTest {

    private final TemplateService service = new TemplateService(
            null, null);

    @Test
    void rendersVariablesAndEscapesHtml() {
        String result = service.render(
                "Hello {{firstName}}, welcome!",
                Map.of("firstName", "<b>Anna</b>"));
        assertEquals("Hello &lt;b&gt;Anna&lt;/b&gt;, welcome!",
                result);
    }

    @Test
    void missingVariablesRenderEmpty() {
        String result = service.render("Hi {{name}}", Map.of());
        assertEquals("Hi ", result);
    }

    @Test
    void onlyKnownTokenNamesAreMatched() {
        String result = service.render(
                "{{firstName}} {{not-a-var}} {{last_name}}",
                Map.of("firstName", "A", "last_name", "B"));
        assertEquals("A {{not-a-var}} B", result);
    }

    @Test
    void multipleOccurrencesRender() {
        String result = service.render(
                "{{x}} and {{x}}", Map.of("x", "1"));
        assertEquals("1 and 1", result);
    }

    @Test
    void ampersandsAreEscaped() {
        String result = service.render("{{v}}",
                Map.of("v", "a&b"));
        assertEquals("a&amp;b", result);
        assertTrue(result.contains("&amp;"));
    }
}
