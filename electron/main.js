const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const http = require('http');

let mainWindow;
let backendProcess;
const BACKEND_PORT = 8080;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 800,
        backgroundColor: '#1e1e1e',
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true
        }
    });

    // Remove default menu for cleaner look
    mainWindow.setMenu(null);

    const loadURL = `http://localhost:${BACKEND_PORT}`;

    // Poll for backend availability
    const checkBackend = () => {
        http.get(loadURL, (res) => {
            if (res.statusCode === 200) {
                mainWindow.loadURL(loadURL);
                console.log('Backend ready, loading UI...');
            } else {
                setTimeout(checkBackend, 1000);
            }
        }).on('error', (err) => {
            console.log('Waiting for backend...');
            setTimeout(checkBackend, 1000);
        });
    };

    checkBackend();

    mainWindow.on('closed', function () {
        mainWindow = null;
    });
}

function startBackend() {
    const jarPath = path.join(__dirname, '..', 'target', 'TradeOption-0.0.1-SNAPSHOT.jar');
    console.log(`Starting backend from: ${jarPath}`);

    backendProcess = spawn('java', ['-jar', jarPath], {
        cwd: path.join(__dirname, '..') // Run from project root
    });

    backendProcess.stdout.on('data', (data) => {
        console.log(`[Backend]: ${data}`);
    });

    backendProcess.stderr.on('data', (data) => {
        console.error(`[Backend Error]: ${data}`);
    });

    backendProcess.on('close', (code) => {
        console.log(`Backend process exited with code ${code}`);
    });
}

function stopBackend() {
    if (backendProcess) {
        console.log('Killing backend process...');
        backendProcess.kill();
        backendProcess = null;
    }
}

app.on('ready', () => {
    startBackend();
    createWindow();
});

app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('quit', () => {
    stopBackend();
});

app.on('activate', function () {
    if (mainWindow === null) {
        createWindow();
    }
});
