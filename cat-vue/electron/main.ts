import { app, BrowserWindow } from 'electron'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    title: '管理系统',
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  win.once('ready-to-show', () => {
    win.show()
  })

  // win.webContents.openDevTools()

  if (process.env.VITE_DEV_SERVER_URL) {
    win.loadURL(process.env.VITE_DEV_SERVER_URL)
    // win.webContents.openDevTools()
  } else {
    const indexPath = path.join(__dirname, '../dist/index.html')
    win.loadFile(indexPath)
  }
}

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
