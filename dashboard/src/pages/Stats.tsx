import { useEffect, useState } from "react";
import { get } from "../api";

type S = {
  counts: { movies: number; series: number; channels: number; users: number; activeSubs: number; plays7d: number };
  top: { id: string; title: string; plays: number }[];
};

export function Stats() {
  const [s, setS] = useState<S | null>(null);
  const [err, setErr] = useState("");
  useEffect(() => { get<S>("/stats").then(setS).catch(() => setErr("تعذر تحميل الإحصائيات")); }, []);
  if (err) return <div className="err">{err}</div>;
  if (!s) return <div>...</div>;
  const c = s.counts;
  const cards: [string, number][] = [
    ["الأفلام", c.movies], ["المسلسلات", c.series], ["القنوات", c.channels],
    ["المستخدمون", c.users], ["اشتراكات فعالة", c.activeSubs], ["مشاهدات آخر 7 أيام", c.plays7d],
  ];
  return (
    <>
      <div className="grid">
        {cards.map(([l, n]) => <div key={l} className="card stat"><b>{n}</b>{l}</div>)}
      </div>
      <h2>الأكثر مشاهدة (7 أيام)</h2>
      <div className="card">
        <table>
          <thead><tr><th>العنوان</th><th>المشاهدات</th></tr></thead>
          <tbody>
            {s.top.map((t) => <tr key={t.id}><td>{t.title}</td><td>{t.plays}</td></tr>)}
            {s.top.length === 0 && <tr><td colSpan={2}>لا بيانات بعد</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
