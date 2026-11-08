-- صفحة رئيسية أنظف: لا بانر، أقسام أقل، وقسم الرياضة المباشرة.
-- الأقسام لا تُحذف بل تُعطَّل، ويمكن إعادة تفعيلها من لوحة التحكم.
UPDATE home_sections SET enabled = FALSE WHERE id IN ('hero', 'recent', 'rec', 'cats');

INSERT INTO categories(id, kind, name, position) VALUES ('sports', 'channel', 'رياضة', 50)
ON CONFLICT (id) DO NOTHING;

INSERT INTO home_sections(id, type, title, source, position, enabled) VALUES
    ('sports', 'channels', 'مباشر الآن: رياضة', 'sports', 3, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ترتيب جديد: متابعة، قنوات، رياضة، أفلام، مسلسلات
UPDATE home_sections SET position = 1 WHERE id = 'continue';
UPDATE home_sections SET position = 2 WHERE id = 'channels';
UPDATE home_sections SET position = 3 WHERE id = 'sports';
UPDATE home_sections SET position = 4 WHERE id = 'movies';
UPDATE home_sections SET position = 5 WHERE id = 'series';
