import { useEffect, useState } from "react";
import { post, session, setUnauthorizedHandler } from "./api";
import { Stats } from "./pages/Stats";
import { Items } from "./pages/Items";
import { Categories } from "./pages/Categories";
import { HomeLayout } from "./pages/HomeLayout";
import { Users } from "./pages/Users";
import { Notifications } from "./pages/Notifications";
import { Ads } from "./pages/Ads";
import { Config } from "./pages/Config";

type Tab = { id: string; label: string; adminOnly?: boolean; el: () => JSX.Element };

const TABS: Tab[] = [
  { id: "stats", label: "الإحصائيات", el: () => <Stats /> },
  { id: "movies", label: "الأفلام", el: () => <Items kind="movie" /> },
  { id: "series", label: "المسلسلات", el: () => <Items kind="series" /> },
  { id: "channels", label: "القنوات", el: () => <Items kind="channel" /> },
  { id: "cats", label: "التصنيفات", el: () => <Categories /> },
  { id: "home", label: "الصفحة الرئيسية", el: () => <HomeLayout /> },
  { id: "ads", label: "الإعلانات", el: () => <Ads /> },
  { id: "notifs", label: "الإشعارات", adminOnly: true, el: () => <Notifications /> },
  { id: "users", label: "المستخدمون والاشتراكات", adminOnly: true, el: () => <Users /> },
  { id: "config", label: "إعدادات التطبيق", adminOnly: true, el: () => <Config /> },
];

function Login({ onDone }: { onDone: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true); setErr("");
    try {
      const r = await post<{ token: string; role: string }>("/login", { email, password });
      session.set(r.token, r.role);
      onDone();
    } catch {
      setErr("بيانات الدخول غير صحيحة");
    } finally { setBusy(false); }
  };

  return (
    <form className="card login" onSubmit={submit}>
      <div className="brand">BEOUT TV</div>
      <div><label>البريد الإلكتروني</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></div>
      <div><label>كلمة المرور</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
      {err && <div className="err">{err}</div>}
      <button className="btn" disabled={busy}>دخول</button>
    </form>
  );
}

export function App() {
  const [authed, setAuthed] = useState(!!session.token);
  const [tab, setTab] = useState("stats");

  useEffect(() => { setUnauthorizedHandler(() => setAuthed(false)); }, []);

  if (!authed) return <Login onDone={() => setAuthed(true)} />;

  const isAdmin = session.role === "admin";
  const tabs = TABS.filter((t) => !t.adminOnly || isAdmin);
  const current = tabs.find((t) => t.id === tab) ?? tabs[0];

  const logout = async () => {
    try { await post("/logout"); } catch { /* ignore */ }
    session.clear(); setAuthed(false);
  };

  return (
    <div className="layout">
      <aside className="side">
        <div className="brand">BEOUT TV</div>
        <nav className="nav">
          {tabs.map((t) => (
            <button key={t.id} className={t.id === current.id ? "on" : ""} onClick={() => setTab(t.id)}>{t.label}</button>
          ))}
          <button onClick={logout}>تسجيل الخروج</button>
        </nav>
      </aside>
      <main className="main" key={current.id}>
        <h1>{current.label}</h1>
        {current.el()}
      </main>
    </div>
  );
}
