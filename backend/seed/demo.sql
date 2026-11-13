-- محتوى تجريبي: يستخدم بثوثاً عامة للاختبار. للتشغيل:
-- docker compose exec -T db psql -U postgres -d <db> < backend/seed/demo.sql
INSERT INTO categories(id,kind,name,position) VALUES
 ('news','channel','أخبار',1),('sports','channel','رياضة',2),('kids','channel','أطفال',3),('music','channel','موسيقى',4),
 ('action','movie','أكشن',1),('drama','series','دراما',1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO items(id,kind,title,description,category_id,live,featured,position,stream_url) VALUES
 ('ch1','channel','قناة الأخبار 1','بث تجريبي','news',TRUE,TRUE,1,'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('ch2','channel','قناة الأخبار 2','بث تجريبي','news',TRUE,TRUE,2,'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8'),
 ('ch3','channel','رياضة 1','بث تجريبي','sports',TRUE,TRUE,3,'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8'),
 ('ch4','channel','رياضة 2','بث تجريبي','sports',TRUE,FALSE,4,'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('ch5','channel','أطفال','بث تجريبي','kids',TRUE,TRUE,5,'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8'),
 ('ch6','channel','موسيقى','بث تجريبي','music',TRUE,FALSE,6,'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8'),
 ('ch7','channel','وثائقية','بث تجريبي','news',TRUE,TRUE,7,'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('ch8','channel','منوعات','بث تجريبي','music',TRUE,FALSE,8,'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8'),
 ('mv1','movie','فيلم تجريبي 1','فيلم للاختبار','action',FALSE,TRUE,1,'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('mv2','movie','فيلم تجريبي 2','فيلم للاختبار','action',FALSE,TRUE,2,'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8'),
 ('mv3','movie','فيلم تجريبي 3','فيلم للاختبار','action',FALSE,TRUE,3,'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('sr1','series','مسلسل تجريبي','مسلسل للاختبار','drama',FALSE,TRUE,4,NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO seasons(id,series_id,number,title) VALUES ('sr1s1','sr1',1,'الموسم 1') ON CONFLICT (id) DO NOTHING;
INSERT INTO episodes(id,season_id,number,title,stream_url) VALUES
 ('sr1e1','sr1s1',1,'الحلقة 1','https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'),
 ('sr1e2','sr1s1',2,'الحلقة 2','https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8')
ON CONFLICT (id) DO NOTHING;
