package com.mailscale.template;

import com.mailscale.auth.Organization;
import com.mailscale.common.ConflictException;
import com.mailscale.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template management with versioning and safe variable substitution.
 *
 * <p>HTML is sanitized with the OWASP Java HTML Sanitizer before storage
 * so templates cannot carry scripts, event handlers, or unsafe URLs.
 * Variable substitution uses explicit {@code {{name}}} tokens and escapes
 * values - no template engine that could evaluate expressions.
 */
@Service
public class TemplateService {

    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");
    private static final PolicyFactory HTML_SANITIZER = new HtmlPolicyBuilder()
            .allowElements("html", "body", "h1", "h2", "h3", "p", "a", "b",
                    "i", "em", "strong", "ul", "ol", "li", "br", "span",
                    "div", "table", "thead", "tbody", "tr", "td", "th", "img")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src").onElements("img")
            .allowAttributes("class").onElements("div", "span", "p", "td",
                    "th", "table", "h1", "h2", "h3")
            .allowStandardUrlProtocols()
            .toFactory();

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public TemplateService(TemplateRepository templateRepository,
                             TemplateVersionRepository templateVersionRepository) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateController.TemplateDto> list(Organization org) {
        return templateRepository.findByOrganizationId(org.getId()).stream()
                .map(t -> new TemplateController.TemplateDto(
                        t.getId(), t.getName(), t.getActiveVersion(),
                        t.getCreatedAt()))
                .toList();
    }

    @Transactional
    public TemplateController.TemplateDto create(Organization org,
            TemplateController.CreateTemplateRequest request) {
        String name = request.name().trim();
        if (templateRepository.findByOrganizationIdAndName(org.getId(), name)
                .isPresent()) {
            throw new ConflictException("Template name already exists");
        }
        Template template = new Template(org, name);
        templateRepository.save(template);
        saveVersion(template, 1, request);
        return new TemplateController.TemplateDto(template.getId(),
                template.getName(), template.getActiveVersion(),
                template.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public TemplateController.TemplateDetailDto get(Organization org, Long id) {
        Template template = templateRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        List<TemplateVersion> versions = templateVersionRepository
                .findByTemplateIdOrderByVersionDesc(template.getId());
        return new TemplateController.TemplateDetailDto(template.getId(),
                template.getName(), template.getActiveVersion(),
                template.getCreatedAt(), versions);
    }

    @Transactional
    public TemplateController.TemplateDto addVersion(Organization org, Long id,
            TemplateController.CreateTemplateRequest request) {
        Template template = templateRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        int newVersion = template.getActiveVersion() + 1;
        saveVersion(template, newVersion, request);
        template.bumpVersion();
        templateRepository.save(template);
        return new TemplateController.TemplateDto(template.getId(),
                template.getName(), template.getActiveVersion(),
                template.getCreatedAt());
    }

    @Transactional
    public void delete(Organization org, Long id) {
        Template template = templateRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        templateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public TemplateController.PreviewDto preview(Organization org, Long id,
            Map<String, Object> variables) {
        Template template = templateRepository.findByIdAndOrganizationId(id,
                org.getId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
        TemplateVersion version = templateVersionRepository
                .findByTemplateIdAndVersion(template.getId(),
                        template.getActiveVersion())
                .orElseThrow(() -> new NotFoundException("Template version not found"));
        return new TemplateController.PreviewDto(
                render(version.getSubject(), variables),
                render(version.getHtmlBody(), variables),
                render(version.getTextBody(), variables));
    }

    public String render(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.getOrDefault(name, "");
            String replacement = value == null ? ""
                    : Matcher.quoteReplacement(escapeHtml(value.toString()));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void saveVersion(Template template, int version,
            TemplateController.CreateTemplateRequest request) {
        String sanitizedHtml = HTML_SANITIZER.sanitize(request.htmlBody());
        String variables = VARIABLE_PATTERN.matcher(request.subject() + " "
                + sanitizedHtml + " " + request.textBody()).results()
                .map(m -> m.group(1))
                .distinct()
                .sorted()
                .collect(Collectors.joining("\",\"", "[\"", "\"]"));
        templateVersionRepository.save(new TemplateVersion(template, version,
                request.subject(), sanitizedHtml, request.textBody(), variables));
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
