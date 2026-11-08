-- الرئيسية بنمط المشغل المباشر: قنوات وأقسام + شريط اقتراحات واحد منفصل.
-- لا أفلام ولا مسلسلات كأقسام في الرئيسية؛ الاقتراح الوحيد هو القسم من النوع suggest.

-- السماح بنوع جديد: suggest (يُسقَط القيد القديم بغض النظر عن اسمه ثم يُعاد بالقائمة الموسّعة)
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'home_sections'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) ILIKE '%type%'
    LOOP
        EXECUTE format('ALTER TABLE home_sections DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE home_sections
    ADD CONSTRAINT home_sections_type_check
    CHECK (type IN ('hero', 'row', 'channels', 'continue', 'categories', 'suggest'));

-- تعطيل أفلام/مسلسلات من الرئيسية (تبقى في الجدول ويمكن إعادتها من لوحة التحكم)
UPDATE home_sections SET enabled = FALSE WHERE id IN ('movies', 'series');

-- شريط "مقترح لك": مميز أولاً ثم الأحدث، أفلام ومسلسلات معاً
INSERT INTO home_sections(id, type, title, source, position, enabled) VALUES
    ('suggest', 'suggest', 'مقترح لك', 'suggested', 9, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ترتيب: قنوات مميزة، رياضة، ثم الاقتراح في الآخر
UPDATE home_sections SET position = 1 WHERE id = 'continue';
UPDATE home_sections SET position = 2 WHERE id = 'channels';
UPDATE home_sections SET position = 3 WHERE id = 'sports';
