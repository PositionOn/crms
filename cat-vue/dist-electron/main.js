import { app as o, BrowserWindow as t } from "electron";
import n from "path";
import { fileURLToPath as a } from "url";
const s = n.dirname(a(import.meta.url));
function i() {
  const e = new t({
    width: 1200,
    height: 800,
    title: "管理系统",
    show: !1,
    webPreferences: {
      nodeIntegration: !1,
      contextIsolation: !0
    }
  });
  if (e.once("ready-to-show", () => {
    e.show();
  }), process.env.VITE_DEV_SERVER_URL)
    e.loadURL(process.env.VITE_DEV_SERVER_URL);
  else {
    const r = n.join(s, "../dist/index.html");
    e.loadFile(r);
  }
}
o.whenReady().then(i);
o.on("window-all-closed", () => {
  process.platform !== "darwin" && o.quit();
});
o.on("activate", () => {
  t.getAllWindows().length === 0 && i();
});
