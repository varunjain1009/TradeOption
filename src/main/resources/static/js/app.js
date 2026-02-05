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

    fetch('/api/strategy', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(strategy)
    })
        .then(response => {
            if (response.ok) {
                console.log('Strategy submitted successfully');
            } else {
                console.error('Failed to submit strategy');
            }
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
    initChart();
    connect();
});
