import { useCallback, useEffect, useState } from "react";
import { get, post, put } from "../api";

type U = { id: string; email: string | null; device: string | null; role: string; banned: boolean; lastSeen: string; subscribedUntil: string | null };

export function Users() {
  const [list, setList] = useState<U[]>([]);
  const [err, setErr] = useState("");
  const load = useCallback(() => { get<U[]>("/users").then(setList).catch(() => setErr("تعذر التحميل")); }, []);
  useEffect(load, [load]);
  const guard = async (f: () => Promise<unknown>) => { setErr(""); try { await f(); load(); } catch (e) { setErr((e as Error).message); } };

  return (
    <>
      {err && <div className="err">{err}</div>}
      <div className="card" style={{ overflowX: "auto" }}>
        <table>
          <thead><tr><th>المستخدم</th><th>الصلاحية</th><th>آخر ظهور</th><th>الاشتراك حتى</th><th></th></tr></thead>
          <tbody>
            {list.map((u) => (
              <tr key={u.id}>
                <td>{u.email ?? (u.device ? u.device.slice(0, 8) + "…" : u.id.slice(0, 8))}{u.banned && <span className="tag"> محظور</span>}</td>
                <td>
                  <select value={u.role} onChange={(e) => guard(() => put(`/users/${u.id}`, { role: e.target.value }))}>
                    <option value="user">مستخدم</option><option value="editor">محرر</option><option value="admin">مسؤول</option>
                  </select>
                </td>
                <td>{new Date(u.lastSeen).toLocaleString("ar")}</td>
                <td>{u.subscribedUntil ? new Date(u.subscribedUntil).toLocaleDateString("ar") : "-"}</td>
                <td className="row">
                  <button className="btn sm" onClick={() => { const d = prompt("عدد أيام الاشتراك", "30"); if (d) guard(() => post(`/users/${u.id}/subscription`, { plan: "manual", days: +d })); }}>اشتراك</button>
                  <button className="btn ghost sm" onClick={() => guard(() => put(`/users/${u.id}`, { banned: !u.banned }))}>{u.banned ? "رفع الحظر" : "حظر"}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
