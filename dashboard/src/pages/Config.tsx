import { useEffect, useState } from "react";
import { get, put } from "../api";

type Cfg = { minVersion: number; maxReconnect: number; maintenance: string | null };

export function Config() {
  const [c, setC] = useState<Cfg | null>(null);
  const [msg, setMsg] = useState("");
  useEffect(() => { get<Cfg>("/config").then((v) => setC({ ...{ minVersion: 0, maxReconnect: 6, maintenance: null } as Cfg, ...v })).catch(() => setMsg("تعذر التحميل")); }, []);
  if (!c) return <div className="err">{msg || "..."}</div>;

  const save = async () => {
    setMsg("");
    try { await put("/config", { ...c, maintenance: c.maintenance?.trim() ? c.maintenance : null }); setMsg("تم الحفظ"); }
    catch (e) { setMsg((e as Error).message); }
  };

  return (
    <div className="card form">
      <div>
        <label>أدنى versionCode مسموح (يُجبر المستخدم على التحديث)</label>
        <input type="number" value={c.minVersion} onChange={(e) => setC({ ...c, minVersion: +e.target.value })} />
      </div>
      <div>
        <label>أقصى محاولات إعادة اتصال المشغل</label>
        <input type="number" value={c.maxReconnect} onChange={(e) => setC({ ...c, maxReconnect: +e.target.value })} />
      </div>
      <div className="full">
        <label>رسالة الصيانة (اتركها فارغة للتشغيل الطبيعي)</label>
        <input value={c.maintenance ?? ""} onChange={(e) => setC({ ...c, maintenance: e.target.value })} />
      </div>
      <div className="full row"><button className="btn" onClick={save}>حفظ</button>{msg && <span className={msg === "تم الحفظ" ? "ok" : "err"}>{msg}</span>}</div>
    </div>
  );
}
