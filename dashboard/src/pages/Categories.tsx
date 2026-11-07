import { useCallback, useEffect, useState } from "react";
import { del, get, post, put, session } from "../api";

export type Cat = { id: string; kind: string; name: string; position: number };

export function useCategories() {
  const [cats, setCats] = useState<Cat[]>([]);
  const reload = useCallback(() => { get<Cat[]>("/categories").then(setCats).catch(() => {}); }, []);
  useEffect(reload, [reload]);
  return { cats, reload };
}

export function Categories() {
  const { cats, reload } = useCategories();
  const [f, setF] = useState<Cat>({ id: "", kind: "movie", name: "", position: 0 });
  const [err, setErr] = useState("");
  const isAdmin = session.role === "admin";

  const save = async () => {
    setErr("");
    try {
      if (f.id && cats.some((c) => c.id === f.id)) await put(`/categories/${f.id}`, f);
      else await post("/categories", f);
      setF({ id: "", kind: f.kind, name: "", position: 0 });
      reload();
    } catch (e) { setErr((e as Error).message); }
  };

  return (
    <>
      <div className="card form">
        <div><label>المعرّف (اختياري، حروف إنجليزية)</label><input value={f.id} onChange={(e) => setF({ ...f, id: e.target.value })} /></div>
        <div><label>النوع</label>
          <select value={f.kind} onChange={(e) => setF({ ...f, kind: e.target.value })}>
            <option value="movie">أفلام</option><option value="series">مسلسلات</option><option value="channel">قنوات</option>
          </select>
        </div>
        <div><label>الاسم</label><input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></div>
        <div><label>الترتيب</label><input type="number" value={f.position} onChange={(e) => setF({ ...f, position: +e.target.value })} /></div>
        <div className="full row"><button className="btn" onClick={save} disabled={!f.name.trim()}>حفظ</button>{err && <span className="err">{err}</span>}</div>
      </div>
      <h2>القائمة</h2>
      <div className="card">
        <table>
          <thead><tr><th>الاسم</th><th>النوع</th><th>الترتيب</th><th></th></tr></thead>
          <tbody>
            {cats.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td><td><span className="tag">{c.kind}</span></td><td>{c.position}</td>
                <td className="row">
                  <button className="btn ghost sm" onClick={() => setF(c)}>تعديل</button>
                  {isAdmin && <button className="btn danger sm" onClick={async () => { if (confirm("حذف التصنيف؟")) { await del(`/categories/${c.id}`); reload(); } }}>حذف</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
