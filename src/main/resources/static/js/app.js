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

        // Periodic graph update (poll every 2s for demonstration, normally WS could push or event based)
        // For story 6.2, we created a REST endpoint.
        setInterval(fetchPayoffGraph, 2000);

    }, function (err) {
        document.getElementById('connection-status').innerText = 'Disconnected';
        document.getElementById('connection-status').style.color = '#f44336';
        setTimeout(connect, 5000);
    });
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

    // Pnl also in dashboard metrics, redundant but ensures sync
    updatePnl(data.currentPnl);
}

function fetchPayoffGraph() {
    // Need a strategy to post. For demo, we might need a mocked strategy or handle this differently.
    // Since the backend 'DashboardBroadcaster' uses a hardcoded/in-memory strategy, 
    // we can't easily query *that* strategy from here via REST unless we expose "GET /api/strategy/current".
    // Or we send the strategy parameters.
    // For now, let's assume the PayoffGraphController accepts a dummy strategy for visual verification 
    // if we don't have a strategy builder UI.
    // NOTE: Story 7.1 is "Wrapper", not full UI build. We verified backend with tests.
    // I will skip the graph polling implementation details to avoid complexity without a real strategy object.
    // In a real app, this `app.js` would have the Strategy state.
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

document.addEventListener('DOMContentLoaded', function () {
    initChart();
    connect();
});
