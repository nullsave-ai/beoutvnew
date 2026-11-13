import { useCallback, useEffect, useState } from "react";
import { del, get, post, put, session } from "../api";

type Ad = { id: string; placement: string; image: string | null; target: string | null; enabled: boolean; position: number };
const blank: Ad = { id: "", placement: "home", image: "", target: "", enabled: true, position: 0 };

export function Ads() {
  const [list, setList] = useState<Ad[]>([]);
  const [f, setF] = useState<Ad>(blank);
  const [err, setErr] = useState("");
  const isAdmin = session.role === "admin";
  const load = useCallback(() => { get<Ad[]>("/ads").then(setList).catch(() => setErr("تعذر التحميل")); }, []);
  useEffect(load, [load]);

  const save = async () => {
    setErr("");
    try {
      if (f.id) await put(`/ads/${f.id}`, f); else await post("/ads", f);
      setF(blank); load();
    } catch (e) { setErr((e as Error).message); }
  };

  return (
    <>
      <div className="card form">
        <div><label>الموضع</label><input value={f.placement} onChange={(e) => setF({ ...f, placement: e.target.value })} /></div>
        <div><label>رابط الصورة</label><input value={f.image ?? ""} onChange={(e) => setF({ ...f, image: e.target.value })} /></div>
        <div><label>الرابط الهدف</label><input value={f.target ?? ""} onChange={(e) => setF({ ...f, target: e.target.value })} /></div>
        <div><label>الترتيب</label><input type="number" value={f.position} onChange={(e) => setF({ ...f, position: +e.target.value })} /></div>
        <label className="chk"><input type="checkbox" checked={f.enabled} onChange={(e) => setF({ ...f, enabled: e.target.checked })} />مفعّل</label>
        <div className="full row"><button className="btn" onClick={save}>حفظ</button>{err && <span className="err">{err}</span>}</div>
      </div>
      <h2>القائمة</h2>
      <div className="card">
        <table>
          <thead><tr><th>الموضع</th><th>الهدف</th><th>الحالة</th><th></th></tr></thead>
          <tbody>
            {list.map((a) => (
              <tr key={a.id}>
                <td>{a.placement}</td><td>{a.target}</td><td>{a.enabled ? "مفعّل" : "متوقف"}</td>
                <td className="row">
                  <button className="btn ghost sm" onClick={() => setF(a)}>تعديل</button>
                  {isAdmin && <button className="btn danger sm" onClick={async () => { if (confirm("حذف؟")) { await del(`/ads/${a.id}`); load(); } }}>حذف</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
