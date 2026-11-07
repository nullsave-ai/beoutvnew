import { useCallback, useEffect, useState } from "react";
import { del, get, post, put, session } from "../api";
import { useCategories } from "./Categories";

type Item = {
  id: string; kind: string; title: string; description: string | null; poster: string | null; backdrop: string | null;
  logo: string | null; year: number | null; durationMin: number | null; quality: string | null; categoryId: string | null;
  live: boolean; online: boolean; featured: boolean; premium: boolean; published: boolean; position: number; streamUrl: string | null;
};
type Episode = { id: string; number: number; title: string; durationMin: number | null; poster: string | null; streamUrl: string | null };
type Season = { id: string; number: number; title: string; episodes: Episode[] };

const empty = (kind: string): Item => ({
  id: "", kind, title: "", description: "", poster: "", backdrop: "", logo: "", year: null, durationMin: null, quality: "",
  categoryId: "", live: kind === "channel", online: true, featured: false, premium: false, published: true, position: 0, streamUrl: "",
});

const num = (v: string) => (v === "" ? null : +v);

function Field({ label, children, full }: { label: string; children: React.ReactNode; full?: boolean }) {
  return <div className={full ? "full" : ""}><label>{label}</label>{children}</div>;
}

function Seasons({ seriesId }: { seriesId: string }) {
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [err, setErr] = useState("");
  const load = useCallback(() => { get<Season[]>(`/items/${seriesId}/seasons`).then(setSeasons).catch(() => setErr("تعذر التحميل")); }, [seriesId]);
  useEffect(load, [load]);
  const guard = async (f: () => Promise<unknown>) => { setErr(""); try { await f(); load(); } catch (e) { setErr((e as Error).message); } };
  const isAdmin = session.role === "admin";

  return (
    <div>
      <div className="row sp">
        <h2>المواسم والحلقات</h2>
        <button className="btn sm" onClick={() => guard(() => post(`/items/${seriesId}/seasons`, { number: (seasons.at(-1)?.number ?? 0) + 1, title: "" }))}>موسم جديد</button>
      </div>
      {err && <div className="err">{err}</div>}
      {seasons.map((s) => (
        <div key={s.id} className="card" style={{ marginBottom: 10 }}>
          <div className="row sp">
            <b>الموسم {s.number}</b>
            <div className="row">
              <button className="btn sm" onClick={() => guard(() => post(`/seasons/${s.id}/episodes`, { number: (s.episodes.at(-1)?.number ?? 0) + 1, title: "", streamUrl: "" }))}>حلقة جديدة</button>
              {isAdmin && <button className="btn danger sm" onClick={() => confirm("حذف الموسم بحلقاته؟") && guard(() => del(`/seasons/${s.id}`))}>حذف</button>}
            </div>
          </div>
          <table>
            <thead><tr><th style={{ width: 70 }}>#</th><th>العنوان</th><th>رابط البث</th><th style={{ width: 90 }}>دقائق</th><th></th></tr></thead>
            <tbody>
              {s.episodes.map((e) => <EpisodeRow key={e.id} e={e} onSave={(v) => guard(() => put(`/episodes/${e.id}`, v))} onDelete={isAdmin ? () => guard(() => del(`/episodes/${e.id}`)) : undefined} />)}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}

function EpisodeRow({ e, onSave, onDelete }: { e: Episode; onSave: (v: Episode) => void; onDelete?: () => void }) {
  const [v, setV] = useState(e);
  const dirty = JSON.stringify(v) !== JSON.stringify(e);
  return (
    <tr>
      <td><input type="number" value={v.number} onChange={(x) => setV({ ...v, number: +x.target.value })} /></td>
      <td><input value={v.title} onChange={(x) => setV({ ...v, title: x.target.value })} /></td>
      <td><input value={v.streamUrl ?? ""} placeholder="https://..." onChange={(x) => setV({ ...v, streamUrl: x.target.value })} /></td>
      <td><input type="number" value={v.durationMin ?? ""} onChange={(x) => setV({ ...v, durationMin: num(x.target.value) })} /></td>
      <td className="row">
        <button className="btn sm" disabled={!dirty} onClick={() => onSave(v)}>حفظ</button>
        {onDelete && <button className="btn danger sm" onClick={onDelete}>حذف</button>}
      </td>
    </tr>
  );
}

function Editor({ init, onClose }: { init: Item; onClose: (changed: boolean) => void }) {
  const { cats } = useCategories();
  const [f, setF] = useState<Item>(init);
  const [id, setId] = useState(init.id);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof Item>(k: K, v: Item[K]) => setF((p) => ({ ...p, [k]: v }));

  const save = async () => {
    setBusy(true); setErr("");
    try {
      if (id) { await put(`/items/${id}`, f); onClose(true); }
      else { const r = await post<{ id: string }>("/items", f); setId(r.id); setF((p) => ({ ...p, id: r.id })); }
    } catch (e) { setErr((e as Error).message); } finally { setBusy(false); }
  };

  const kindCats = cats.filter((c) => c.kind === f.kind);
  const txt = (k: keyof Item) => (f[k] as string | null) ?? "";

  return (
    <div className="modal" onClick={(e) => e.target === e.currentTarget && onClose(!!id)}>
      <div className="card">
        <div className="row sp"><h2>{id ? "تعديل" : "عنصر جديد"}</h2><button className="btn ghost sm" onClick={() => onClose(!!id)}>إغلاق</button></div>
        <div className="form">
          <Field label="العنوان" full><input value={f.title} onChange={(e) => set("title", e.target.value)} /></Field>
          <Field label="التصنيف">
            <select value={f.categoryId ?? ""} onChange={(e) => set("categoryId", e.target.value)}>
              <option value="">بدون</option>{kindCats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="الترتيب"><input type="number" value={f.position} onChange={(e) => set("position", +e.target.value)} /></Field>
          {f.kind !== "channel" && <>
            <Field label="السنة"><input type="number" value={f.year ?? ""} onChange={(e) => set("year", num(e.target.value))} /></Field>
            <Field label="المدة (دقيقة)"><input type="number" value={f.durationMin ?? ""} onChange={(e) => set("durationMin", num(e.target.value))} /></Field>
            <Field label="الجودة (HD, 4K...)"><input value={txt("quality")} onChange={(e) => set("quality", e.target.value)} /></Field>
            <Field label="الوصف" full><textarea value={txt("description")} onChange={(e) => set("description", e.target.value)} /></Field>
            <Field label="رابط الغلاف"><input value={txt("poster")} onChange={(e) => set("poster", e.target.value)} /></Field>
            <Field label="رابط الخلفية"><input value={txt("backdrop")} onChange={(e) => set("backdrop", e.target.value)} /></Field>
          </>}
          {f.kind === "channel" && <Field label="رابط الشعار"><input value={txt("logo")} onChange={(e) => set("logo", e.target.value)} /></Field>}
          {f.kind !== "series" && <Field label="رابط البث (HLS / DASH / MP4)" full><input value={txt("streamUrl")} placeholder="https://..." onChange={(e) => set("streamUrl", e.target.value)} /></Field>}
          <div className="full row">
            <label className="chk"><input type="checkbox" checked={f.published} onChange={(e) => set("published", e.target.checked)} />منشور</label>
            <label className="chk"><input type="checkbox" checked={f.featured} onChange={(e) => set("featured", e.target.checked)} />مميز</label>
            <label className="chk"><input type="checkbox" checked={f.premium} onChange={(e) => set("premium", e.target.checked)} />يتطلب اشتراكاً</label>
            {f.kind === "channel" && <>
              <label className="chk"><input type="checkbox" checked={f.online} onChange={(e) => set("online", e.target.checked)} />متصل</label>
              <label className="chk"><input type="checkbox" checked={f.live} onChange={(e) => set("live", e.target.checked)} />بث مباشر</label>
            </>}
          </div>
          <div className="full row"><button className="btn" onClick={save} disabled={busy || !f.title.trim()}>حفظ</button>{err && <span className="err">{err}</span>}</div>
        </div>
        {f.kind === "series" && (id ? <Seasons seriesId={id} /> : <p className="tag">احفظ المسلسل أولاً لإضافة المواسم</p>)}
      </div>
    </div>
  );
}

export function Items({ kind }: { kind: "movie" | "series" | "channel" }) {
  const [list, setList] = useState<Item[]>([]);
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [edit, setEdit] = useState<Item | null>(null);
  const [err, setErr] = useState("");
  const isAdmin = session.role === "admin";

  const load = useCallback(() => {
    get<Item[]>(`/items?kind=${kind}&q=${encodeURIComponent(q)}&cursor=${page * 50}`).then(setList).catch(() => setErr("تعذر التحميل"));
  }, [kind, q, page]);
  useEffect(() => { const t = setTimeout(load, 200); return () => clearTimeout(t); }, [load]);

  return (
    <>
      <div className="row sp" style={{ marginBottom: 12 }}>
        <input style={{ maxWidth: 280 }} placeholder="بحث بالعنوان" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <button className="btn" onClick={() => setEdit(empty(kind))}>إضافة</button>
      </div>
      {err && <div className="err">{err}</div>}
      <div className="card">
        <table>
          <thead><tr><th></th><th>العنوان</th><th>الحالة</th><th></th></tr></thead>
          <tbody>
            {list.map((i) => (
              <tr key={i.id}>
                <td>{(i.poster || i.logo) && <img className="thumb" src={(i.poster || i.logo)!} alt="" loading="lazy" />}</td>
                <td>{i.title}</td>
                <td className="row">
                  {!i.published && <span className="tag">مخفي</span>}
                  {i.featured && <span className="tag">مميز</span>}
                  {i.premium && <span className="tag">مدفوع</span>}
                  {kind === "channel" && !i.online && <span className="tag">غير متصل</span>}
                </td>
                <td className="row">
                  <button className="btn ghost sm" onClick={() => setEdit(i)}>تعديل</button>
                  {isAdmin && <button className="btn danger sm" onClick={async () => { if (confirm("حذف نهائي؟")) { await del(`/items/${i.id}`); load(); } }}>حذف</button>}
                </td>
              </tr>
            ))}
            {list.length === 0 && <tr><td colSpan={4}>لا توجد نتائج</td></tr>}
          </tbody>
        </table>
      </div>
      <div className="row" style={{ marginTop: 10 }}>
        <button className="btn ghost sm" disabled={page === 0} onClick={() => setPage(page - 1)}>السابق</button>
        <button className="btn ghost sm" disabled={list.length < 50} onClick={() => setPage(page + 1)}>التالي</button>
      </div>
      {edit && <Editor init={edit} onClose={(changed) => { setEdit(null); if (changed) load(); }} />}
    </>
  );
}
