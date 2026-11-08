import { useCallback, useEffect, useState } from "react";
import { get, post } from "../api";

type N = { id: string; title: string; body: string; createdAt: string };

export function Notifications() {
  const [list, setList] = useState<N[]>([]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [msg, setMsg] = useState("");
  const load = useCallback(() => { get<N[]>("/notifications").then(setList).catch(() => setMsg("تعذر التحميل")); }, []);
  useEffect(load, [load]);

  const send = async () => {
    setMsg("");
    try { await post("/notifications", { title, body }); setTitle(""); setBody(""); load(); setMsg("تم التسجيل"); }
    catch (e) { setMsg((e as Error).message); }
  };

  return (
    <>
      <p className="tag">تُحفظ الإشعارات هنا، والإرسال الفعلي للأجهزة تتولاه خدمة الدفع المستقلة (FCM) التي تقرأ هذا الجدول.</p>
      <div className="card form">
        <div className="full"><label>العنوان</label><input value={title} onChange={(e) => setTitle(e.target.value)} /></div>
        <div className="full"><label>النص</label><textarea value={body} onChange={(e) => setBody(e.target.value)} /></div>
        <div className="full row"><button className="btn" disabled={!title.trim() || !body.trim()} onClick={send}>إرسال</button>{msg && <span>{msg}</span>}</div>
      </div>
      <h2>السجل</h2>
      <div className="card">
        <table>
          <thead><tr><th>العنوان</th><th>النص</th><th>التاريخ</th></tr></thead>
          <tbody>{list.map((n) => <tr key={n.id}><td>{n.title}</td><td>{n.body}</td><td>{new Date(n.createdAt).toLocaleString("ar")}</td></tr>)}</tbody>
        </table>
      </div>
    </>
  );
}
