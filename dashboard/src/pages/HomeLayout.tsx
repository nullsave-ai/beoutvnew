import { useEffect, useState } from "react";
import { get, put } from "../api";

type Sec = { id: string; type: string; title: string; source: string; enabled: boolean };

const SOURCES: [string, string][] = [
  ["featured", "المحتوى المميز"], ["latest_movies", "أحدث الأفلام"], ["latest_series", "أحدث المسلسلات"],
  ["channels_featured", "القنوات المميزة"], ["recent", "أضيف حديثاً"], ["recommended", "مقترح"],
  ["categories", "التصنيفات"], ["continue", "متابعة المشاهدة (محلي)"], ["sports", "رياضة مباشرة"], ["suggested", "اقتراح الرئيسية (أفلام ومسلسلات)"],
];
const TYPES: [string, string][] = [["hero", "بانر"], ["row", "صف"], ["channels", "قنوات"], ["continue", "متابعة"], ["categories", "تصنيفات"], ["suggest", "اقتراحات"]];

export function HomeLayout() {
  const [list, setList] = useState<Sec[]>([]);
  const [msg, setMsg] = useState("");

  useEffect(() => { get<Sec[]>("/home-sections").then(setList).catch(() => setMsg("تعذر التحميل")); }, []);

  const upd = (i: number, p: Partial<Sec>) => setList((l) => l.map((s, k) => (k === i ? { ...s, ...p } : s)));
  const move = (i: number, d: number) => setList((l) => {
    const j = i + d; if (j < 0 || j >= l.length) return l;
    const c = [...l]; [c[i], c[j]] = [c[j], c[i]]; return c;
  });
  const save = async () => {
    setMsg("");
    try { await put("/home-sections", list.map(({ id, type, title, source, enabled }) => ({ id, type, title, source, enabled }))); setMsg("تم الحفظ"); }
    catch (e) { setMsg((e as Error).message); }
  };

  return (
    <>
      <p className="tag">الترتيب والإظهار والإخفاء هنا يصل للتطبيق مباشرة بدون تحديث APK. الرئيسية في التطبيق مشغل مباشر: أقسام القنوات (النوع قنوات) تظهر في لوحة الأقسام، ونوع اقتراحات يظهر كشريط منفصل أسفلها؛ الصفوف والبانر لا تُعرض فيها.</p>
      <div className="card">
        <table>
          <thead><tr><th></th><th>العنوان</th><th>النوع</th><th>المصدر</th><th>ظاهر</th><th></th></tr></thead>
          <tbody>
            {list.map((s, i) => (
              <tr key={s.id}>
                <td className="row"><button className="btn ghost sm" onClick={() => move(i, -1)}>أعلى</button><button className="btn ghost sm" onClick={() => move(i, 1)}>أسفل</button></td>
                <td><input value={s.title} onChange={(e) => upd(i, { title: e.target.value })} /></td>
                <td><select value={s.type} onChange={(e) => upd(i, { type: e.target.value })}>{TYPES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></td>
                <td><select value={s.source} onChange={(e) => upd(i, { source: e.target.value })}>{SOURCES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></td>
                <td><input type="checkbox" style={{ width: "auto" }} checked={s.enabled} onChange={(e) => upd(i, { enabled: e.target.checked })} /></td>
                <td><button className="btn danger sm" onClick={() => setList((l) => l.filter((_, k) => k !== i))}>حذف</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="row" style={{ marginTop: 10 }}>
        <button className="btn ghost" onClick={() => setList((l) => [...l, { id: `s${Date.now().toString(36)}`, type: "row", title: "", source: "recent", enabled: true }])}>قسم جديد</button>
        <button className="btn" onClick={save}>حفظ الترتيب</button>
        {msg && <span className={msg === "تم الحفظ" ? "ok" : "err"}>{msg}</span>}
      </div>
    </>
  );
}
