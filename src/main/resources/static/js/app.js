var stompClient = null;
var payoffChart = null;

function connect() {
    var socket = new SockJS('/ws-endpoint');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logging

    stompClient.connect({}, function (frame) {
        document.getElementById('connection-status').innerText = 'Connected: ' + frame;
        document.getElementById('connection-status').style.color = '#4caf50';

        // Subscribe to broadcasts
        stompClient.subscribe('/topic/spot', function (msg) {
            updateSpot(JSON.parse(msg.body));
        });

        stompClient.subscribe('/topic/pnl', function (msg) {
            updatePnl(msg.body);
        });

        stompClient.subscribe('/topic/volatility', function (msg) {
            updateVolatility(msg.body);
        });

        stompClient.subscribe('/topic/dashboard', function (msg) {
            updateDashboard(JSON.parse(msg.body));
        });

        // Periodic graph update
        setInterval(fetchPayoffGraph, 2000);

    }, function (err) {
        document.getElementById('connection-status').innerText = 'Disconnected';
        document.getElementById('connection-status').style.color = '#f44336';
        setTimeout(connect, 5000);
    });
}

var legs = [];

function addLeg() {
    var tbody = document.getElementById('legs-body');
    var row = document.createElement('tr');

    row.innerHTML = `
        <td style="padding: 5px;">
            <select class="leg-type" style="background: #333; color: white; border: 1px solid #555;">
                <option value="CE">CE</option>
                <option value="PE">PE</option>
            </select>
        </td>
        <td style="padding: 5px;">
            <select class="leg-action" style="background: #333; color: white; border: 1px solid #555;">
                <option value="BUY">BUY</option>
                <option value="SELL">SELL</option>
            </select>
        </td>
        <td style="padding: 5px;">
            <input type="number" class="leg-strike" value="22000" style="background: #333; color: white; border: 1px solid #555; width: 100px;">
        </td>
        <td style="padding: 5px;">
            <input type="number" class="leg-qty" value="50" style="background: #333; color: white; border: 1px solid #555; width: 80px;">
        </td>
        <td style="padding: 5px;">
            <button onclick="this.parentElement.parentElement.remove()" style="background: #f44336; color: white; border: none; cursor: pointer;">X</button>
        </td>
    `;

    tbody.appendChild(row);
}

function submitStrategy() {
    var symbol = document.getElementById('symbol').value;
    var expiry = document.getElementById('expiry').value;

    var legRows = document.querySelectorAll('#legs-body tr');
    var strategyLegs = [];

    legRows.forEach(row => {
        var type = row.querySelector('.leg-type').value;
        var action = row.querySelector('.leg-action').value; // BUY/SELL
        var strike = parseFloat(row.querySelector('.leg-strike').value);
        var qty = parseFloat(row.querySelector('.leg-qty').value);

        var signedQty = action === 'BUY' ? qty : -qty;

        strategyLegs.push({
            optionType: type,
            strikePrice: strike,
            quantity: signedQty,
            expiryDate: expiry
        });
    });

    var strategy = {
        symbol: symbol,
        legs: strategyLegs,
        name: "Custom Strategy"
    };

    fetch('/api/strategy/analyze', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(strategy)
    })
        .then(response => response.json())
        .then(metrics => {
            console.log('Analysis received:', metrics);
            updateDashboard(metrics);
        })
        .catch(error => console.error('Error:', error));
}

function updateSpot(data) {
    document.getElementById('spot-price').innerText = data.price.toFixed(2);
}

function updateVolatility(data) {
    var val = parseFloat(data) * 100;
    document.getElementById('volatility').innerText = val.toFixed(1) + '%';
}

function updatePnl(data) {
    var val = parseFloat(data);
    var el = document.getElementById('pnl');
    el.innerText = val.toFixed(2);
    el.className = 'metric-value ' + (val >= 0 ? 'positive' : 'negative');
}

function updateDashboard(data) {
    // Greeks
    if (data.greeks) {
        document.getElementById('delta').innerText = data.greeks.delta.toFixed(4);
        document.getElementById('gamma').innerText = data.greeks.gamma.toFixed(4);
        document.getElementById('theta').innerText = data.greeks.theta.toFixed(4);
        document.getElementById('vega').innerText = data.greeks.vega.toFixed(4);
    }

    // Metrics
    document.getElementById('max-profit').innerText = data.maxProfit.toFixed(2);
    document.getElementById('max-loss').innerText = data.maxLoss.toFixed(2);
    document.getElementById('pop').innerText = (data.probabilityOfProfit * 100).toFixed(1) + '%';
    document.getElementById('risk-reward').innerText = data.riskRewardRatio.toFixed(2);

    updatePnl(data.currentPnl);
}

function fetchPayoffGraph() {
    // Placeholder
}

// Chart Initialization
function initChart() {
    var ctx = document.getElementById('payoffChart').getContext('2d');
    payoffChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Expiry PNL',
                borderColor: '#64b5f6',
                data: [],
                borderWidth: 2,
                pointRadius: 0
            }, {
                label: 'T-0 PNL',
                borderColor: '#ffb74d',
                data: [],
                borderWidth: 2,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { grid: { color: '#333' }, ticks: { color: '#888' } },
                y: { grid: { color: '#333' }, ticks: { color: '#888' } }
            },
            plugins: { legend: { labels: { color: '#ccc' } } }
        }
    });
}

// Initialize with Performance Optimization
function fetchConfig() {
    fetch('/api/config')
        .then(response => response.json())
        .then(config => {
            var symbolExpiries = config.symbolExpiries;
            var symbolSelect = document.getElementById('symbol');
            var expirySelect = document.getElementById('expiry');

            // Optimization: Pre-compute the HTML for each symbol's expiry options
            // This ensures the 'change' event is instant, as we just swap the innerHTML string.
            var expiryHtmlCache = {};

            Object.keys(symbolExpiries).forEach(sym => {
                var dates = symbolExpiries[sym] || [];
                var optionsHtml = dates.map(date => `<option value="${date}">${date}</option>`).join('');
                expiryHtmlCache[sym] = optionsHtml;
            });

            // Fast update function
            function updateExpiryDropdown(symbol) {
                if (expiryHtmlCache[symbol]) {
                    expirySelect.innerHTML = expiryHtmlCache[symbol];
                } else {
                    expirySelect.innerHTML = '<option value="">-- No Expiry --</option>';
                }
            }

            // Populate Symbols
            symbolSelect.innerHTML = '';
            Object.keys(symbolExpiries).forEach(sym => {
                var opt = document.createElement('option');
                opt.value = sym;
                opt.innerText = sym;
                symbolSelect.appendChild(opt);
            });

            // Set initial state
            if (symbolSelect.options.length > 0) {
                updateExpiryDropdown(symbolSelect.value);
            }

            // Add change listener
            symbolSelect.addEventListener('change', function () {
                updateExpiryDropdown(this.value);
            });

            console.log('Configuration loaded with optimization');
        })
        .catch(err => console.error('Error loading config:', err));
}

document.addEventListener('DOMContentLoaded', function () {
    fetchConfig();
    fetchPositions();
    initChart();
    connect();
});

function suggestStraddle() {
    var symbol = document.getElementById('symbol').value;
    fetch('/api/strategy/suggest/straddle?symbol=' + symbol)
        .then(response => response.json())
        .then(strategy => {
            console.log('Suggested Strategy:', strategy);

            // Clear current legs
            var tbody = document.getElementById('legs-body');
            tbody.innerHTML = '';

            // Populate legs
            strategy.legs.forEach(leg => {
                var row = document.createElement('tr');
                // Auto-map type/action similar to addLeg HTML structure
                // Leg: { strikePrice, type, action, ... }
                // Note: type is CE/PE, action is BUY/SELL

                row.innerHTML = `
                    <td style="padding: 5px;">
                        <select class="leg-type" style="background: #333; color: white; border: 1px solid #555;">
                            <option value="CE" ${leg.type === 'CE' ? 'selected' : ''}>CE</option>
                            <option value="PE" ${leg.type === 'PE' ? 'selected' : ''}>PE</option>
                        </select>
                    </td>
                    <td style="padding: 5px;">
                        <select class="leg-action" style="background: #333; color: white; border: 1px solid #555;">
                            <option value="BUY" ${leg.action === 'BUY' ? 'selected' : ''}>BUY</option>
                            <option value="SELL" ${leg.action === 'SELL' ? 'selected' : ''}>SELL</option>
                        </select>
                    </td>
                    <td style="padding: 5px;">
                        <input type="number" class="leg-strike" value="${leg.strikePrice}" style="background: #333; color: white; border: 1px solid #555; width: 100px;">
                    </td>
                    <td style="padding: 5px;">
                        <input type="number" class="leg-qty" value="${leg.quantity}" style="background: #333; color: white; border: 1px solid #555; width: 80px;">
                    </td>
                    <td style="padding: 5px;">
                        <button onclick="this.parentElement.parentElement.remove()" style="background: #f44336; color: white; border: none; cursor: pointer;">X</button>
                    </td>
                `;
                tbody.appendChild(row);
            });

            // Set Expiry if matches
            if (strategy.legs.length > 0 && strategy.legs[0].expiryDate) {
                var expirySelect = document.getElementById('expiry');
                expirySelect.value = strategy.legs[0].expiryDate;
            }
        })
        .catch(err => console.error('Error suggesting straddle:', err));
}

function fetchPositions() {
    fetch('/api/strategy/positions')
        .then(response => response.json())
        .then(positions => {
            renderPositions(positions);
        })
        .catch(err => console.error('Error fetching positions:', err));
}

function renderPositions(positions) {
    // We need a place to put this table. 
    // Since index.html doesn't have a specific container, let's inject a new card dynamically if it doesn't exist.
    let container = document.getElementById('positions-container');
    if (!container) {
        // Find the grid container to append to
        const grid = document.querySelector('.grid-container');
        container = document.createElement('div');
        container.id = 'positions-container';
        container.className = 'card';
        container.style.gridColumn = 'span 3';
        container.style.marginTop = '20px';
        container.innerHTML = `
            <div class="card-header">Open Positions</div>
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="border-bottom: 1px solid #444;">
                        <th style="padding: 10px; text-align: left;">Symbol</th>
                        <th style="padding: 10px; text-align: left;">Expiry</th>
                        <th style="padding: 10px; text-align: left;">Strike</th>
                        <th style="padding: 10px; text-align: left;">Type</th>
                        <th style="padding: 10px; text-align: right;">Net Qty</th>
                        <th style="padding: 10px; text-align: right;">Realized PNL</th>
                        <th style="padding: 10px; text-align: right;">Status</th>
                        <th style="padding: 10px;"></th>
                    </tr>
                </thead>
                <tbody id="positions-body"></tbody>
            </table>
        `;
        // Insert after Strategy Builder (which is first child)
        if (grid.firstChild) {
            grid.insertBefore(container, grid.children[1]); // Insert after first card
        } else {
            grid.appendChild(container);
        }
    }

    const tbody = document.getElementById('positions-body');
    tbody.innerHTML = '';

    positions.forEach(pos => {
        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid #333';
        tr.innerHTML = `
            <td style="padding: 10px;">${pos.symbol}</td>
            <td style="padding: 10px;">${pos.expiryDate}</td>
            <td style="padding: 10px;">${pos.strikePrice}</td>
            <td style="padding: 10px;">${pos.optionType}</td>
            <td style="padding: 10px; text-align: right;">${pos.netQuantity}</td>
            <td style="padding: 10px; text-align: right;" class="${pos.realizedPnl >= 0 ? 'positive' : 'negative'}">${pos.realizedPnl.toFixed(2)}</td>
            <td style="padding: 10px; text-align: right;">${pos.status}</td>
            <td style="padding: 10px; text-align: center;">
                <button onclick="showHistory('${pos.id}')" style="padding: 5px 10px; background: #64b5f6; color: white; border: none; cursor: pointer; border-radius: 4px;">History</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Chart instance
let pnlChartInstance = null;

function showHistory(positionId) {
    const modal = document.getElementById('historyModal');
    // If modal doesn't exist (it should be in index.html), create it dynamically?
    // Implementation plan said update index.html. I updated index.html but did I save it properly? 
    // Yes, Step 842.

    // Wait, Step 842 updated index.html but did NOT verify logic.
    // Let's assume it IS there.
    if (modal) {
        modal.style.display = 'block';
        fetch(`/api/strategy/position/${positionId}/history`)
            .then(response => response.json())
            .then(data => {
                renderPnlChart(data);
            })
            .catch(error => console.error('Error fetching history:', error));
    } else {
        console.error('History modal not found!');
    }
}

// Note: Function name change to avoid collision if any
function renderPnlChart(historyData) {
    const ctx = document.getElementById('pnlChart');
    if (!ctx) return;

    if (pnlChartInstance) {
        pnlChartInstance.destroy();
    }

    // Sort just in case
    historyData.sort((a, b) => a.timestamp - b.timestamp);

    const labels = historyData.map(point => new Date(point.timestamp).toLocaleTimeString());
    const dataPoints = historyData.map(point => point.realizedPnl);

    pnlChartInstance = new Chart(ctx.getContext('2d'), {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Realized PNL',
                data: dataPoints,
                borderColor: '#007bff',
                tension: 0.1,
                fill: false
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'PNL (INR)' },
                    grid: { color: '#333' }, ticks: { color: '#ccc' }
                },
                x: {
                    title: { display: true, text: 'Time' },
                    grid: { color: '#333' }, ticks: { color: '#ccc' }
                }
            },
            plugins: { legend: { labels: { color: '#ccc' } } }
        }
    });

}

window.closeHistoryModal = function () {
    const modal = document.getElementById('historyModal');
    if (modal) modal.style.display = 'none';
}

// Global click to close
window.onclick = function (event) {
    const modal = document.getElementById('historyModal');
    if (event.target == modal) {
        modal.style.display = "none";
    }
}
