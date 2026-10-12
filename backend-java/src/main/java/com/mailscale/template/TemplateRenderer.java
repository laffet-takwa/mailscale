package com.mailscale.template;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders {{variable}} tokens in template content.
 *
 * <p>Complexity: O(n) single pass over the template with regex matching;
 * values are HTML-escaped to prevent injection through variable content.
 */
@Component
public class TemplateRenderer {

    private final TemplateService templateService;

    public TemplateRenderer(TemplateService templateService) {
        this.templateService = templateService;
    }

    public String render(String template, Map<String, Object> variables) {
        return templateService.render(template, variables);
    }
}
