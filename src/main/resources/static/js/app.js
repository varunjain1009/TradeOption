var stompClient = null;
window.currentSpotPrice = 22000; // Default fallback

function connect() {
    var socket = new SockJS('/ws-endpoint');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        document.getElementById('connection-status').innerText = 'Connected: ' + frame;
        document.getElementById('connection-status').style.color = '#4caf50';

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

        // Start Safe Fetch Loop
        // scheduleNextFetch(); // Disabled for manual updates

    }, function (err) {
        document.getElementById('connection-status').innerText = 'Disconnected';
        document.getElementById('connection-status').style.color = '#f44336';
        setTimeout(connect, 5000);
    });
}

// ---------------------------------------------------------
// UI: Strategy Builder
// ---------------------------------------------------------
function addLeg() {
    var tbody = document.getElementById('legs-body');
    var row = document.createElement('tr');

    // Default values based on Spot
    var spot = window.currentSpotPrice || 24000;
    var strike = Math.round(spot / 100) * 100; // Round to nearest 100 for Nifty/BankNifty generic
    if (document.getElementById('symbol').value === 'NIFTY') {
        strike = Math.round(spot / 50) * 50;
    }

    var lots = 1;
    var expiry = document.getElementById('expiry').value || '';
    var price = 0;

    row.innerHTML = `
        <td><input type="checkbox" checked style="accent-color: var(--accent-blue);"></td>
        <td>
            <select class="leg-type" style="width: 60px; background: transparent; border: none; color: white;">
                <option value="CE">CE</option>
                <option value="PE">PE</option>
            </select>
        </td>
        <td>
            <select class="leg-action" style="width: 70px; background: transparent; border: none; font-weight: bold; color: var(--accent-green);">
                <option value="BUY">BUY</option>
                <option value="SELL">SELL</option>
            </select>
        </td>
        <td>
            <input type="number" class="leg-strike" value="${strike}" step="50" style="width: 80px; background: #222; border: 1px solid #444; color: white;">
        </td>
        <td>
            <input type="text" class="leg-expiry" value="${expiry}" style="width: 90px; background: #222; border: 1px solid #444; color: white; font-size: 0.9em;">
        </td>
        <td style="text-align: right;">
            <input type="number" class="leg-price" value="${price}" step="0.05" style="width: 70px; text-align: right; background: #222; border: 1px solid #444; color: white;">
        </td>
        <td style="text-align: right;">
            <input type="number" class="leg-exit-price" value="" step="0.05" placeholder="--" style="width: 70px; text-align: right; background: #222; border: 1px solid #444; color: white;">
        </td>
        <td style="text-align: right;">
            <input type="number" class="leg-lots" value="${lots}" style="width: 60px; text-align: right; background: #222; border: 1px solid #444; color: white;">
        </td>
        <td style="text-align: right;">
            <button onclick="this.parentElement.parentElement.remove()" style="color: var(--accent-red); background: none; padding: 2px;">✖</button>
        </td>
    `;

    // Add listener for color change
    var actionSelect = row.querySelector('.leg-action');
    actionSelect.addEventListener('change', function () {
        this.style.color = this.value === 'BUY' ? 'var(--accent-green)' : 'var(--accent-red)';
    });

    // Add listeners for Quote Fetching - REMOVED for manual mode
    // var inputs = row.querySelectorAll('.leg-type, .leg-action, .leg-strike, .leg-expiry');
    // ...

    tbody.appendChild(row);

    // Initial Fetch - REMOVED
    // fetchQuote(row);
}

// Utility: Debounce
function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

// function fetchQuote(row) { ... } // Removed for manual mode

function getStrategyFromUI() {
    var symbol = document.getElementById('symbol').value;
    var topExpiry = document.getElementById('expiry').value;

    var legRows = document.querySelectorAll('#legs-body tr');
    var strategyLegs = [];

    legRows.forEach(row => {
        // Only include checked rows
        var checkbox = row.querySelector('input[type="checkbox"]');
        if (checkbox && !checkbox.checked) return;

        var type = row.querySelector('.leg-type').value;
        var action = row.querySelector('.leg-action').value;
        var strike = parseFloat(row.querySelector('.leg-strike').value);
        var lots = parseFloat(row.querySelector('.leg-lots').value);
        var price = parseFloat(row.querySelector('.leg-price').value) || 0;
        var exitPrice = parseFloat(row.querySelector('.leg-exit-price').value) || null;
        var expiry = row.querySelector('.leg-expiry').value || topExpiry;

        var lotSize = (window.lotSizes && window.lotSizes[symbol]) ? window.lotSizes[symbol] : 1;
        var qty = lots * lotSize;

        var signedQty = action === 'BUY' ? qty : -qty;

        strategyLegs.push({
            type: type,                  // Changed from optionType
            action: action,
            strikePrice: strike,
            quantity: signedQty,
            expiryDate: expiry,
            entryPrice: price,           // Changed from price
            exitPrice: exitPrice,        // Exit price for closed strategies
            symbol: symbol               // Fix: Pass symbol for backend market data fetch
        });
    });

    return {
        symbol: symbol,
        legs: strategyLegs,
        name: "UI Strategy"
    };
}

function submitStrategy() {
    var strategy = getStrategyFromUI();

    fetch('/api/strategy/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(strategy)
    })
        .then(response => response.json())
        .then(metrics => {
            console.log('Analysis received:', metrics);
            updateDashboard(metrics);
            // Force immediate graph update
            window.analyzeStrategyManual();
        })
        .catch(error => console.error('Error:', error));
}

// ---------------------------------------------------------
// UI: Dashboard Updates
// ---------------------------------------------------------
function updateSpot(data) {
    window.currentSpotPrice = data.price;
    document.getElementById('spot-price').innerText = data.price.toFixed(2);
}

function updateVolatility(data) {
    var val = parseFloat(data) * 100;
    document.getElementById('volatility').innerText = val.toFixed(1) + '%';
}

function updatePnl(data) {
    var val = parseFloat(data);
    var el = document.getElementById('pnl');
    if (el) {
        el.innerText = '₹' + val.toFixed(2);
        el.className = val >= 0 ? 'positive' : 'negative';
    }

    // Also update Current PNL
    var curEl = document.getElementById('current-pnl');
    if (curEl) {
        curEl.innerText = '₹' + val.toFixed(2);
        curEl.style.color = val >= 0 ? 'var(--accent-green)' : 'var(--accent-red)';
    }
}

function updateDashboard(data) {
    // Greeks
    if (data.greeks) {
        document.getElementById('delta').innerText = data.greeks.delta.toFixed(2);
        document.getElementById('gamma').innerText = data.greeks.gamma.toFixed(4);
        document.getElementById('theta').innerText = data.greeks.theta.toFixed(2);
        document.getElementById('vega').innerText = data.greeks.vega.toFixed(2);
    }

    // Metrics
    document.getElementById('max-profit').innerText = '₹' + data.maxProfit.toFixed(0);
    document.getElementById('max-loss').innerText = '₹' + data.maxLoss.toFixed(0);
    document.getElementById('pop').innerText = (data.probabilityOfProfit * 100).toFixed(1) + '%';
    document.getElementById('risk-reward').innerText = "1:" + data.riskRewardRatio.toFixed(1);

    // PNL Visibility
    var pnlContainer = document.getElementById('pnl-container');
    if (data.closed) { // Check for 'closed' boolean from DashboardMetrics
        pnlContainer.style.display = 'block';
        updatePnl(data.currentPnl);
    } else {
        pnlContainer.style.display = 'none';
        // Hide PnL values if open, or show T+0 if desired? 
        // User requested: "will only appear when i enter exit price..." 
        // So strict hiding is correct.
    }
}

// ---------------------------------------------------------
// Chart: Payoff Graph (Plotly)
// ---------------------------------------------------------
var isFetching = false;

// function scheduleNextFetch() { ... } // Removed for manual update

function calculateTimeToExpiry(expiryDateStr) {
    if (!expiryDateStr) return 0.1;
    var now = new Date();
    var expiry = new Date(expiryDateStr);
    var diffTime = expiry - now;
    if (diffTime <= 0) return 0.001;
    var diffDays = diffTime / (1000 * 60 * 60 * 24);
    return diffDays / 365.0;
}

// RENAMED TO BREAK HIDDEN CALLERS
window.analyzeStrategyManual = function () {
    console.log("analyzeStrategyManual called manually");

    // 2. OVERLAP CHECK
    if (isFetching) {
        console.warn("Already fetching, skipping...");
        return;
    }

    var strategy = getStrategyFromUI();
    if (strategy.legs.length === 0) {
        console.warn("No legs to analyze.");
        return;
    }

    isFetching = true;

    // UI Feedback
    var btn = document.getElementById('refresh-btn');
    if (btn) btn.innerText = 'Analyzing...';

    // Parameters
    var spotStr = document.getElementById('spot-price').innerText;
    var spot = parseFloat(spotStr);

    // Fallback: If spot is unknown, use average strike from strategy
    if (isNaN(spot)) {
        if (strategy.legs.length > 0) {
            var totalStrike = strategy.legs.reduce((sum, leg) => sum + leg.strikePrice, 0);
            spot = totalStrike / strategy.legs.length;
            console.log("Using average strike as spot fallback:", spot);
        } else {
            spot = 22000; // Final fallback
        }
    }

    var volStr = document.getElementById('volatility').innerText;
    var vol = parseFloat(volStr) / 100.0;
    if (isNaN(vol)) vol = 0.20;

    var expiry = document.getElementById('expiry').value;
    var time = calculateTimeToExpiry(expiry);
    var rate = 0.10;

    var params = new URLSearchParams({
        spot: spot,
        volatility: vol,
        timeToExpiry: time,
        interestRate: rate,
        range: 0.15 // Wider range
    });

    fetch('/api/analytics/payoff-graph?' + params.toString(), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(strategy)
    })
        .then(response => response.json())
        .then(data => {
            updatePayoffChart(data);
        })
        .catch(err => console.error('Error fetching graph:', err))
        .finally(() => {
            isFetching = false;
            // scheduleNextFetch(); // Disabled
            if (btn) btn.innerText = 'Refresh Analysis';
        });
}

function updatePayoffChart(data) {
    if (!data || !data.spotPrices || !data.expiryPnl) return;

    // Update Net Credit
    var netCreditElem = document.getElementById('net-credit');
    if (netCreditElem && data.netCredit !== undefined && data.netCredit !== null) {
        var val = data.netCredit;
        var formatted = '₹' + Math.abs(val).toFixed(2);

        // Remove old classes
        netCreditElem.classList.remove('positive', 'negative');

        // If Net Credit > 0, it means we RECEIVED money (Credit) -> Green
        // If Net Credit < 0, it means we PAID money (Debit) -> Red
        if (val >= 0) {
            netCreditElem.classList.add('positive');
            netCreditElem.innerText = formatted + ' (Cr)';
        } else {
            netCreditElem.classList.add('negative');
            netCreditElem.innerText = formatted + ' (Dr)';
        }
    }

    // Update Breakevens
    var breakevenElem = document.getElementById('breakevens');
    if (breakevenElem && data.breakevens) {
        if (data.breakevens.length === 0) {
            breakevenElem.innerText = 'None';
        } else {
            breakevenElem.innerText = data.breakevens.join(', ');
        }
    }

    // Helper to interp zero crossings for clean fill
    function splitData(x, y) {
        var xGreen = [], yGreen = [];
        var xRed = [], yRed = [];

        for (var i = 0; i < x.length - 1; i++) {
            var x1 = x[i], y1 = y[i];
            var x2 = x[i + 1], y2 = y[i + 1];

            // Push current point
            if (y1 >= 0) {
                xGreen.push(x1); yGreen.push(y1);
                xRed.push(x1); yRed.push(0); // Clamps red to 0
            } else {
                xRed.push(x1); yRed.push(y1);
                xGreen.push(x1); yGreen.push(0); // Clamps green to 0
            }

            // Check crossing
            if ((y1 > 0 && y2 < 0) || (y1 < 0 && y2 > 0)) {
                var m = (y2 - y1) / (x2 - x1);
                var xZero = x1 + (0 - y1) / m;

                // Add Zero Point to both
                xGreen.push(xZero); yGreen.push(0);
                xRed.push(xZero); yRed.push(0);
            }
        }
        // Last point
        var lastX = x[x.length - 1];
        var lastY = y[x.length - 1];
        if (lastY >= 0) {
            xGreen.push(lastX); yGreen.push(lastY);
            xRed.push(lastX); yRed.push(0);
        } else {
            xRed.push(lastX); yRed.push(lastY);
            xGreen.push(lastX); yGreen.push(0);
        }

        return { xGreen, yGreen, xRed, yRed };
    }

    var split = splitData(data.spotPrices, data.expiryPnl);

    // Trace 1: Profit Area (Green)
    var traceProfit = {
        x: split.xGreen,
        y: split.yGreen,
        mode: 'lines',
        name: 'Max Profit',
        line: { color: '#00e676', width: 2 },
        fill: 'tozeroy',
        fillcolor: 'rgba(0, 230, 118, 0.2)', // Opstra Green
        hoverinfo: 'y'
    };

    // Trace 2: Loss Area (Red)
    var traceLoss = {
        x: split.xRed,
        y: split.yRed,
        mode: 'lines',
        name: 'Max Loss',
        line: { color: '#ff3d00', width: 2 },
        fill: 'tozeroy',
        fillcolor: 'rgba(255, 61, 0, 0.2)', // Opstra Red
        hoverinfo: 'y'
    };

    // Trace 3: T+0 PNL (Dashed Blue)
    var traceT0 = {
        x: data.spotPrices,
        y: data.tzeroPnl,
        mode: 'lines',
        name: 'T+0 PNL',
        line: { color: '#2979ff', width: 2, dash: 'dash' }, // Opstra Blue
        hoverinfo: 'y'
    };

    // Find current spot index for vertical line (approx)
    // We can add a shape line for Spot Price if provided, 
    // but the cursor spikeline handles the dynamic "current" check generally.

    var layout = {
        title: false,
        paper_bgcolor: 'rgba(30,30,30,1)', // Dark Card BG
        plot_bgcolor: 'rgba(30,30,30,1)',
        font: { color: '#e0e0e0', family: 'Segoe UI, sans-serif' },
        xaxis: {
            title: 'Spot Price',
            gridcolor: '#444',
            zerolinecolor: '#888',
            showspikes: true,           // Vertical Arrow/Line
            spikemode: 'across',
            spikesnap: 'cursor',
            spikedash: 'dash',
            spikecolor: '#ffffff',
            spikethickness: 1
        },
        yaxis: {
            title: 'PNL (₹)',
            gridcolor: '#444',
            zeroline: true,
            zerolinecolor: '#fff',
            zerolinewidth: 1,
            showspikes: true,           // Horizontal Arrow/Line
            spikemode: 'across',
            spikesnap: 'cursor',
            spikedash: 'dash',
            spikecolor: '#ffffff',
            spikethickness: 1
        },
        margin: { l: 60, r: 20, t: 30, b: 40 },
        showlegend: true,
        legend: { orientation: 'h', y: 1.1, x: 0.5, xanchor: 'center' },
        hovermode: 'x unified',         // Crosshair behavior
        dragmode: 'pan'
    };

    var config = {
        responsive: true,
        displayModeBar: true,
        displaylogo: false,
        modeBarButtonsToRemove: ['lasso2d', 'select2d']
    };

    // Plot
    Plotly.react('payoffChart', [traceProfit, traceLoss, traceT0], layout, config);
}

// ---------------------------------------------------------
// Config & Initialization
// ---------------------------------------------------------
function fetchConfig() {
    fetch('/api/config')
        .then(response => response.json())
        .then(config => {
            var symbolExpiries = config.symbolExpiries;
            window.lotSizes = config.lotSizes || {}; // Store globally
            var symbolSelect = document.getElementById('symbol');
            var expirySelect = document.getElementById('expiry');
            var expiryHtmlCache = {};

            Object.keys(symbolExpiries).forEach(sym => {
                var dates = symbolExpiries[sym] || [];
                var optionsHtml = dates.map(date => `<option value="${date}">${date}</option>`).join('');
                expiryHtmlCache[sym] = optionsHtml;
            });

            function updateExpiryDropdown(symbol) {
                if (expiryHtmlCache[symbol]) {
                    expirySelect.innerHTML = expiryHtmlCache[symbol];
                } else {
                    expirySelect.innerHTML = '<option value="">-- No Expiry --</option>';
                }
            }

            symbolSelect.innerHTML = '';
            Object.keys(symbolExpiries).forEach(sym => {
                var opt = document.createElement('option');
                opt.value = sym;
                opt.innerText = sym;
                symbolSelect.appendChild(opt);
            });

            if (symbolSelect.options.length > 0) {
                updateExpiryDropdown(symbolSelect.value);
            }

            symbolSelect.addEventListener('change', function () {
                updateExpiryDropdown(this.value);
            });
        })
        .catch(err => console.error('Error loading config:', err));
}

// ---------------------------------------------------------
// Suggestions
// ---------------------------------------------------------
function populateLegs(strategy) {
    var tbody = document.getElementById('legs-body');
    tbody.innerHTML = '';

    // Auto populate expiry from strategy if consistent
    if (strategy.legs.length > 0) {
        // We let each leg dictate expiry in the row
    }

    strategy.legs.forEach(leg => {
        var row = document.createElement('tr');
        var isBuy = leg.action === 'BUY';
        var actionColor = isBuy ? 'var(--accent-green)' : 'var(--accent-red)';
        var price = leg.entryPrice !== undefined ? leg.entryPrice : (leg.price || 0);
        var expiry = leg.expiryDate || document.getElementById('expiry').value || '';

        var currentSymbol = document.getElementById('symbol').value; // Potentially unsafe if mixed symbols
        var lotSize = (window.lotSizes && window.lotSizes[currentSymbol]) ? window.lotSizes[currentSymbol] : 1;
        var lots = Math.abs(leg.quantity) / lotSize;
        // Handle case where quantity isn't perfectly divisible (custom strategies) - maybe show decimal?
        // lots = Math.round(lots * 100) / 100; 

        row.innerHTML = `
            <td><input type="checkbox" checked style="accent-color: var(--accent-blue);"></td>
            <td>
                <select class="leg-type" style="width: 60px; background: transparent; border: none; color: white;">
                    <option value="CE" ${leg.type === 'CE' ? 'selected' : ''}>CE</option>
                    <option value="PE" ${leg.type === 'PE' ? 'selected' : ''}>PE</option>
                </select>
            </td>
            <td>
                <select class="leg-action" style="width: 70px; background: transparent; border: none; font-weight: bold; color: ${actionColor};">
                    <option value="BUY" ${isBuy ? 'selected' : ''}>BUY</option>
                    <option value="SELL" ${!isBuy ? 'selected' : ''}>SELL</option>
                </select>
            </td>
            <td>
                <input type="number" class="leg-strike" value="${leg.strikePrice}" style="width: 80px; background: #222; border: 1px solid #444; color: white;">
            </td>
             <td>
                <input type="text" class="leg-expiry" value="${expiry}" style="width: 90px; background: #222; border: 1px solid #444; color: white; font-size: 0.9em;">
            </td>
            <td style="text-align: right;">
                <input type="number" class="leg-price" value="${price}" step="0.05" style="width: 70px; text-align: right; background: #222; border: 1px solid #444; color: white;">
            </td>
            <td style="text-align: right;">
                <input type="number" class="leg-exit-price" value="${leg.exitPrice || ''}" step="0.05" placeholder="--" style="width: 70px; text-align: right; background: #222; border: 1px solid #444; color: white;">
            </td>
            <td style="text-align: right;">
                <input type="number" class="leg-lots" value="${lots}" style="width: 60px; text-align: right; background: #222; border: 1px solid #444; color: white;">
            </td>
            <td style="text-align: right;">
                <button onclick="this.parentElement.parentElement.remove()" style="color: #666; background: none; cursor: pointer;">✖</button>
            </td>
         `;

        // Add listener for color change on new rows
        var actionSelect = row.querySelector('.leg-action');
        actionSelect.addEventListener('change', function () {
            this.style.color = this.value === 'BUY' ? 'var(--accent-green)' : 'var(--accent-red)';
        });

        tbody.appendChild(row);
    });
}

function suggestStraddle() {
    var symbol = document.getElementById('symbol').value;
    fetch('/api/strategy/suggest/straddle?symbol=' + symbol)
        .then(res => res.json())
        .then(data => {
            populateLegs(data);
            console.log('Straddle strategy suggested. Click "Analyze Strategy" or "Refresh Analysis" to see the payoff graph.');
        })
        .catch(console.error);
}

function suggestStrangle() {
    var symbol = document.getElementById('symbol').value;
    fetch('/api/strategy/suggest/strangle?symbol=' + symbol)
        .then(res => res.json())
        .then(data => {
            populateLegs(data);
            console.log('Strangle strategy suggested. Click "Analyze Strategy" or "Refresh Analysis" to see the payoff graph.');
        })
        .catch(console.error);
}

function resetStrategy() {
    // Clear Legs
    document.getElementById('legs-body').innerHTML = '';

    // Clear Graph
    Plotly.purge('payoffChart');
    document.getElementById('payoffChart').innerHTML = '';

    // Reset Summary Metrics
    ['pop', 'max-profit', 'max-loss', 'risk-reward', 'breakevens', 'net-credit', 'pnl', 'current-pnl', 'delta', 'gamma', 'theta', 'vega'].forEach(id => {
        var el = document.getElementById(id);
        if (el) {
            el.innerText = '--';
            el.className = '';
            el.style.color = '';
        }
    });

    // Reset Name
    document.getElementById('strategy-name').value = '';
    currentStrategyId = null;
}

// ---------------------------------------------------------
// Strategy Persistence (Save/Load)
// ---------------------------------------------------------

var currentStrategyId = null;
var currentFilter = 'ACTIVE';

function saveStrategy() {
    var strategy = getStrategyFromUI();
    if (!strategy.legs || strategy.legs.length === 0) {
        alert("Cannot save empty strategy.");
        return;
    }

    var nameInput = document.getElementById('strategy-name');
    var name = nameInput.value.trim();

    // Auto-generate name if empty
    if (!name) {
        name = generateStrategyName(strategy);
        nameInput.value = name;
    }

    strategy.name = name;

    if (currentStrategyId) {
        strategy.id = currentStrategyId;
    }

    // Preserve existing status/pnl if updating
    fetch('/api/strategies/' + (currentStrategyId || 'new'))
        .then(res => {
            if (res.ok) return res.json();
            return { status: 'ACTIVE' };
        })
        .then(existing => {
            if (existing.id) {
                strategy.status = existing.status;
                strategy.closedTimestamp = existing.closedTimestamp;
                strategy.realizedPnl = existing.realizedPnl;
            }

            return fetch('/api/strategies', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(strategy)
            });
        })
        .then(response => response.json())
        .then(data => {
            alert('Strategy saved successfully!');
            currentStrategyId = data.id;
            loadSavedStrategies();
        })
        .catch(err => console.error('Error saving strategy:', err));
}

function generateStrategyName(strategy) {
    if (!strategy.symbol) return "Custom Strategy";
    var type = "Strategy";

    // Simple heuristic for naming
    var legs = strategy.legs;
    if (legs.length === 2 && legs[0].type !== legs[1].type) {
        // likely straddle or strangle
        if (legs[0].strikePrice === legs[1].strikePrice) type = "Straddle";
        else type = "Strangle";
    } else if (legs.length === 4) {
        type = "Iron Condor/Fly";
    }

    var action = "Long";
    // Check if net sell
    var sold = legs.filter(l => l.action === 'SELL').length;
    if (sold >= legs.length / 2) action = "Short";

    return strategy.symbol + " " + action + " " + type;
}

function filterStrategies(status) {
    currentFilter = status;

    // Update UI buttons
    document.getElementById('filter-active').style.background = status === 'ACTIVE' ? 'var(--accent-blue)' : '#444';
    document.getElementById('filter-active').style.color = status === 'ACTIVE' ? 'white' : '#aaa';

    document.getElementById('filter-closed').style.background = status === 'CLOSED' ? 'var(--accent-blue)' : '#444';
    document.getElementById('filter-closed').style.color = status === 'CLOSED' ? 'white' : '#aaa';

    loadSavedStrategies();
}

function loadSavedStrategies() {
    var startDateVal = document.getElementById('filter-start-date').value;
    var endDateVal = document.getElementById('filter-end-date').value;

    var startTs = startDateVal ? new Date(startDateVal).getTime() : 0;
    var endTs = endDateVal ? new Date(endDateVal).getTime() + 86400000 : 9999999999999; // End of day

    fetch('/api/strategies')
        .then(res => res.json())
        .then(allStrategies => {
            // Filter for Graph (All relevant trades in range)
            var graphStrategies = allStrategies.filter(s => {
                var ts = (s.status === 'CLOSED' && s.closedTimestamp) ? s.closedTimestamp : s.createdTimestamp;
                return ts >= startTs && ts <= endTs;
            });

            renderPnlChart(graphStrategies);

            // Filter for List (Based on current status tab AND date)
            var listStrategies = graphStrategies.filter(s => s.status === currentFilter);

            var listContainer = document.getElementById('saved-strategies-list');
            listContainer.innerHTML = '';

            if (listStrategies.length === 0) {
                listContainer.innerHTML = '<div style="color: #666; font-style: italic; padding: 10px;">No ' + currentFilter.toLowerCase() + ' strategies found in range.</div>';
                return;
            }

            listStrategies.forEach(s => {
                var item = document.createElement('div');
                item.style.padding = '12px';
                item.style.borderBottom = '1px solid #444';
                item.style.display = 'flex';
                item.style.justifyContent = 'space-between';
                item.style.alignItems = 'center';
                item.style.background = 'rgba(255,255,255,0.05)';
                item.style.borderRadius = '4px';

                var date = new Date(s.updatedTimestamp).toLocaleDateString();
                var infoText = `${s.symbol} • ${s.legs.length} Legs • ${date}`;

                if (s.status === 'CLOSED' && s.closedTimestamp) {
                    var closeDate = new Date(s.closedTimestamp).toLocaleDateString();
                    var pnlColor = (s.realizedPnl >= 0) ? 'var(--accent-green)' : 'var(--accent-red)';
                    infoText += `<br><span style="font-size: 0.9em; color: #aaa;">Closed: ${closeDate} | PnL: <span style="color: ${pnlColor}; font-weight: bold;">${s.realizedPnl}</span></span>`;
                }

                var actionsHtml = '';
                if (s.status === 'ACTIVE') {
                    actionsHtml += `<button onclick="closeStrategy('${s.id}')" style="background: var(--accent-red); border: none; color: white; padding: 4px 8px; border-radius: 4px; font-size: 0.8em; margin-right: 5px; cursor: pointer;">Close Trade</button>`;
                }
                actionsHtml += `<button onclick="deleteStrategy('${s.id}')" style="background: none; border: none; color: #888; cursor: pointer;">🗑️</button>`;

                item.innerHTML = `
                <div onclick="loadStrategy('${s.id}')" style="flex-grow: 1; cursor: pointer;">
                    <div style="font-weight: bold; color: var(--accent-blue); font-size: 1.1em;">${s.name}</div>
                    <div style="font-size: 0.85em; color: #aaa; margin-top: 2px;">
                        ${infoText}
                    </div>
                </div>
                <div style="display: flex; align-items: center;">
                    ${actionsHtml}
                </div>
            `;
                listContainer.appendChild(item);
            });
        })
        .catch(err => console.error('Error loading strategies:', err));
}

function renderPnlChart(strategies) {
    // 1. Prepare Closed Trades (Line)
    var closedTrades = strategies.filter(s => s.status === 'CLOSED' && s.closedTimestamp).sort((a, b) => a.closedTimestamp - b.closedTimestamp);

    var x = [];
    var y = [];
    var ids = [];
    var text = [];
    var cumulative = 0;

    closedTrades.forEach(s => {
        cumulative += (s.realizedPnl || 0);
        x.push(new Date(s.closedTimestamp).toISOString());
        y.push(cumulative);
        ids.push(s.id);
        text.push(`${s.name} (PnL: ${s.realizedPnl})`);
    });

    var traceClosed = {
        x: x,
        y: y,
        mode: 'lines+markers',
        name: 'Cumulative PnL',
        customdata: ids,
        text: text,
        hovertemplate: '%{text}<br>Cumulative: %{y}<extra></extra>',
        line: { color: '#4caf50', width: 2 },
        marker: { size: 6 }
    };

    // 2. Prepare Active Trades (Markers)
    var activeTrades = strategies.filter(s => s.status === 'ACTIVE').sort((a, b) => a.createdTimestamp - b.createdTimestamp);

    var xActive = [];
    var yActive = [];
    var activeIds = [];
    var activeText = [];

    activeTrades.forEach(s => {
        var createTime = s.createdTimestamp || Date.now();
        xActive.push(new Date(createTime).toISOString());

        // Find cumulative PnL at this time for visual context
        var pnlAtTime = 0;
        for (var i = 0; i < closedTrades.length; i++) {
            if (closedTrades[i].closedTimestamp <= createTime) {
                pnlAtTime = y[i];
            } else {
                break;
            }
        }

        yActive.push(pnlAtTime);
        activeIds.push(s.id);
        activeText.push(`${s.name} (Active)`);
    });

    var traceActive = {
        x: xActive,
        y: yActive,
        mode: 'markers',
        name: 'Active Trades',
        customdata: activeIds,
        text: activeText,
        hovertemplate: '%{text}<extra></extra>',
        marker: { size: 8, color: '#2979ff', symbol: 'circle-open', line: { width: 2 } }
    };

    var data = [];
    if (x.length > 0) data.push(traceClosed);
    if (xActive.length > 0) data.push(traceActive);

    if (data.length === 0) {
        document.getElementById('pnl-chart').innerHTML = '<div style="display:flex; justify-content:center; align-items:center; height:100%; color:#666;">No trades in range</div>';
        return;
    }

    var layout = {
        title: { text: 'Performance & Activity', font: { color: '#e0e0e0', size: 14 } },
        paper_bgcolor: 'rgba(0,0,0,0)',
        plot_bgcolor: 'rgba(0,0,0,0)',
        height: 200,
        margin: { t: 30, r: 20, l: 40, b: 30 },
        xaxis: {
            color: '#aaaaaa',
            gridcolor: '#333'
        },
        yaxis: {
            color: '#aaaaaa',
            gridcolor: '#333',
            zerolinecolor: '#666'
        },
        showlegend: true,
        legend: { orientation: 'h', y: 1.1, x: 0.5, xanchor: 'center' }
    };

    Plotly.newPlot('pnl-chart', data, layout, { displayModeBar: false });

    // Interactivity
    document.getElementById('pnl-chart').on('plotly_click', function (data) {
        if (data.points && data.points.length > 0) {
            var id = data.points[0].customdata;
            if (id) {
                loadStrategy(id);
            }
        }
    });
}

function updateStrategyStatus(id, newStatus) {
    fetch('/api/strategies/' + id)
        .then(res => res.json())
        .then(strategy => {
            strategy.status = newStatus;
            return fetch('/api/strategies', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(strategy)
            });
        })
        .then(() => {
            loadSavedStrategies();
        })
        .catch(console.error);
}

function loadStrategy(id) {
    fetch('/api/strategies/' + id)
        .then(res => res.json())
        .then(strategy => {
            currentStrategyId = strategy.id;
            document.getElementById('strategy-name').value = strategy.name;

            // Fix: Set symbol BEFORE populating legs to ensure correct Lot Size is used
            if (strategy.symbol) {
                var symbolSelect = document.getElementById('symbol');
                symbolSelect.value = strategy.symbol;
                // Trigger change to update expiries (handled by event listener)
                var event = new Event('change');
                symbolSelect.dispatchEvent(event);
            }

            populateLegs({ legs: strategy.legs });

            // UI Constraints for Closed Strategies
            var isClosed = (strategy.status === 'CLOSED');
            var buttons = document.querySelectorAll('button[onclick*="analyzeStrategy"], button[onclick*="suggest"], button[onclick="resetStrategy()"]');

            buttons.forEach(btn => {
                if (isClosed) {
                    btn.disabled = true;
                    btn.style.opacity = '0.5';
                    btn.style.cursor = 'not-allowed';
                } else {
                    btn.disabled = false;
                    btn.style.opacity = '1';
                    btn.style.cursor = 'pointer';
                }
            });

            if (isClosed) {
                // Don't auto-analyze
            } else {
                submitStrategy();
                // Force graph update
                if (window.analyzeStrategyManual) {
                    window.analyzeStrategyManual();
                }
            }

            // Hide panel
            document.getElementById('saved-strategies-panel').style.display = 'none';

            // Auto-switch filter tab if strategy doesn't match current filter
            if (strategy.status !== currentFilter) {
                filterStrategies(strategy.status);
            }

            if (strategy.symbol) document.getElementById('symbol').value = strategy.symbol;

            // Fetch and display current PnL
            if (strategy.id) fetchCurrentPnL(strategy.id);
        })
        .catch(err => console.error('Error loading strategy:', err));
}

// Fetch and display current PnL for a saved strategy
function fetchCurrentPnL(strategyId) {
    fetch(`/api/strategies/${strategyId}/current-pnl`)
        .then(res => res.json())
        .then(data => {
            var pnlElem = document.getElementById('current-pnl');
            if (pnlElem && data.currentPnl !== undefined) {
                var pnl = data.currentPnl;
                var formatted = '₹' + Math.abs(pnl).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                var color = pnl >= 0 ? 'var(--accent-green)' : 'var(--accent-red)';
                var sign = pnl >= 0 ? '+' : '-';
                pnlElem.innerHTML = `<span style="color: ${color}">${sign}${formatted}</span>`;
                pnlElem.title = data.isClosed ? 'Realized PnL' : 'Unrealized PnL';
            }
        })
        .catch(err => {
            console.warn('Could not fetch current PnL:', err);
            document.getElementById('current-pnl').textContent = '--';
        });
}

function deleteStrategy(id) {
    if (!confirm("Are you sure you want to delete this strategy?")) return;

    fetch('/api/strategies/' + id, { method: 'DELETE' })
        .then(() => {
            loadSavedStrategies();
            if (currentStrategyId === id) {
                currentStrategyId = null;
                document.getElementById('strategy-name').value = '';
            }
        })
        .catch(err => console.error('Error deleting strategy:', err));
}

function toggleSavedStrategies() {
    var panel = document.getElementById('saved-strategies-panel');
    if (panel.style.display === 'none') {
        panel.style.display = 'block';
        loadSavedStrategies();
    } else {
        panel.style.display = 'none';
    }
}

// ---------------------------------------------------------
// Existing Positions (Legacy Support)
// ---------------------------------------------------------
function fetchPositions() {
    fetch('/api/strategy/positions')
        .then(response => response.json())
        .then(positions => {
            renderPositions(positions);
        })
        .catch(err => console.error('Error fetching positions:', err));
}

function renderPositions(positions) {
    var container = document.getElementById('positions-container');
    // ... (Keep existing implementation or map to new table if desired) ...
    // For now, let's keep it minimal or it'll break the "Positions" view logic.
    // The overhauled UI has a "Strategy Positions" table in the builder. 
    // The "Open Positions" is a separate list of executed trades.

    // We can inject it below or wherever. 
    // For brevity, skipping full re-implementation of renderPositions here, using simple injection.

    let tableBody = document.getElementById('positions-body');
    // If we are reusing the same table ID, we need to be careful.
    // In new index.html, builder table is 'legs-body'.
    // Existing positions table body needs a place. 
    // Let's create a container if not exists.

    if (!container) {
        // Create container at bottom
        const grid = document.querySelector('.grid-container');
        container = document.createElement('div');
        container.id = 'positions-container';
        container.className = 'card';
        container.style.gridColumn = 'span 2';
        container.style.marginTop = '20px';
        container.innerHTML = `<div class="card-header">Trade History</div><div class="card-body" id="history-list"></div>`;
        document.body.appendChild(container); // Append to body or grid
    }
}

// ---------------------------------------------------------
// Main
// ---------------------------------------------------------
document.addEventListener('DOMContentLoaded', function () {
    console.log("APP VERSION 3.6.0 LOADED - PnL Features Active");
    fetchConfig();
    fetchPositions();
    connect();
    // No explicit chart init needed, Plotly.react handles it on first data arrival
});
