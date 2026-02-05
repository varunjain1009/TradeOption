@echo off
cd electron

if not exist node_modules (
    echo Installing dependencies...
    call npm install
)

echo Starting TradeOption...
call npm start
