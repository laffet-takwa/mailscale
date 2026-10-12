-- Base demo organization and template (user accounts are seeded by
-- DemoDataInitializer when MAILSCALE_DEMO_SEED_ENABLED=true so that
-- passwords are hashed with the configured encoder).

INSERT INTO organizations (name, slug, plan) VALUES ('Demo Company', 'demo', 'FREE');

INSERT INTO templates (organization_id, name, active_version)
SELECT id, 'Welcome Email', 1 FROM organizations WHERE slug = 'demo';

INSERT INTO template_versions (template_id, version, subject, html_body, text_body, variables)
SELECT t.id, 1, 'Welcome to {{company}}, {{firstName}}!',
       '<html><body><h1>Welcome {{firstName}}!</h1><p>Thanks for joining {{company}}.</p></body></html>',
       'Welcome {{firstName}}! Thanks for joining {{company}}.',
       '["firstName","company"]'
FROM templates t JOIN organizations o ON o.id = t.organization_id WHERE o.slug = 'demo';

INSERT INTO contact_tags (organization_id, name)
SELECT id, 'newsletter' FROM organizations WHERE slug = 'demo';
INSERT INTO contact_tags (organization_id, name)
SELECT id, 'customers' FROM organizations WHERE slug = 'demo';

INSERT INTO segments (organization_id, name, definition)
SELECT id, 'All active contacts', '{"match":"ALL","rules":[{"field":"status","operator":"EQUALS","value":"ACTIVE"}]}'
FROM organizations WHERE slug = 'demo';
