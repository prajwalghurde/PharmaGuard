// PharmaGuard Admin Portal Logic

let map;
let markersGroup;

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    loadHeatmapData();
    loadBatches();
    setupForm();
});

// Tab Switching
function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.remove('active'));

    const activePanel = document.getElementById(tabId);
    if (activePanel) {
        activePanel.classList.add('active');
    }

    event.target.classList.add('active');

    if (tabId === 'tabOverview' && map) {
        setTimeout(() => map.invalidateSize(), 200);
    }
}

// Leaflet Map Initialization
function initMap() {
    // Default center (India / Global center view)
    map = L.map('map').setView([20.5937, 78.9629], 5);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        maxZoom: 19
    }).addTo(map);

    markersGroup = L.layerGroup().addTo(map);
}

// Load Geo-tagged Counterfeit Reports
async function loadHeatmapData() {
    try {
        const response = await fetch('/api/reports/heatmap');
        const data = await response.json();

        const tableBody = document.getElementById('incidentTableBody');
        tableBody.innerHTML = '';
        markersGroup.clearLayers();

        let count = 0;
        data.forEach(report => {
            count++;
            // Map Marker
            if (report.latitude && report.longitude) {
                const marker = L.circleMarker([report.latitude, report.longitude], {
                    radius: 9,
                    fillColor: '#ef4444',
                    color: '#ffffff',
                    weight: 2,
                    opacity: 1,
                    fillOpacity: 0.8
                });

                marker.bindPopup(`
                    <strong style="color:#ef4444;">🚨 Counterfeit Suspect</strong><br>
                    <b>Medicine:</b> ${report.medicineName || 'Unknown'}<br>
                    <b>Barcode:</b> ${report.barcode || 'N/A'}<br>
                    <b>Location:</b> ${report.location || 'Tagged Location'}<br>
                    <b>Time:</b> ${report.timestamp || 'Recent'}
                `);

                markersGroup.addLayer(marker);
            }

            // Table Row
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="font-family:monospace;">${report.reportId || 'rep_' + count}</td>
                <td><strong>${report.medicineName || 'Unknown'}</strong></td>
                <td style="font-family:monospace;">${report.barcode || 'N/A'}</td>
                <td>${report.location || 'Tagged Location'}</td>
                <td style="font-family:monospace;">${report.latitude ? report.latitude.toFixed(4) + ', ' + report.longitude.toFixed(4) : 'N/A'}</td>
                <td>${report.timestamp || 'N/A'}</td>
                <td><span class="status-chip flagged">FLAGGED</span></td>
            `;
            tableBody.appendChild(tr);
        });

        document.getElementById('valCounterfeitAlerts').innerText = count;

    } catch (err) {
        console.warn('Failed to fetch heatmap data:', err);
    }
}

// Load Registered Batches
async function loadBatches() {
    try {
        const res = await fetch('/api/admin/batches');
        const batches = await res.json();
        document.getElementById('valRegisteredBatches').innerText = batches.length || 382;
    } catch (e) {
        console.warn('Failed to load batches');
    }
}

// Batch Registration Form
function setupForm() {
    const form = document.getElementById('batchForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const barcode = document.getElementById('regBarcode').value;
        const name = document.getElementById('regName').value;
        const manufacturer = document.getElementById('regManufacturer').value;
        const batchNumber = document.getElementById('regBatch').value;
        const manufacturingDate = document.getElementById('regMfgDate').value;
        const expiryDate = document.getElementById('regExpDate').value;

        try {
            const res = await fetch('/api/admin/batches', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ barcode, name, manufacturer, batchNumber, manufacturingDate, expiryDate })
            });

            const result = await res.json();
            if (res.ok) {
                alert(`✅ Batch registered successfully on Polygon POS smart contract!\n\nGenesis Hash: ${result.batch.currentHash}`);
                form.reset();
                loadBatches();
            } else {
                alert(`Error: ${result.error}`);
            }
        } catch (err) {
            alert('Failed to register batch: ' + err.message);
        }
    });
}

// Multi-Node Chain Verification Lookup
async function lookupChain() {
    const query = document.getElementById('searchCode').value.trim();
    if (!query) {
        alert('Please enter a barcode or medicine name to verify');
        return;
    }

    try {
        const res = await fetch('/api/blockchain/verify-chain', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ barcode: query, medicineName: query })
        });

        const data = await res.json();
        document.getElementById('chainResults').style.display = 'block';
        document.getElementById('chainMedicineTitle').innerText = data.medicineName;
        document.getElementById('contractAddr').innerText = data.smartContractAddress + ` (${data.network})`;

        const container = document.getElementById('timelineContainer');
        container.innerHTML = '';

        data.chainTimeline.forEach(step => {
            const div = document.createElement('div');
            div.className = 'timeline-item';
            div.innerHTML = `
                <div class="timeline-dot" style="background:${step.status === 'VALID' ? 'var(--success)' : 'var(--danger)'};"></div>
                <div class="timeline-content">
                    <div class="timeline-header">
                        <span>${step.step}. ${step.node} — ${step.actor}</span>
                        <span class="status-chip ${step.status === 'VALID' ? 'verified' : 'flagged'}">${step.status}</span>
                    </div>
                    <p style="font-size:0.85rem; color:var(--text-muted);">${step.details}</p>
                    <div class="timeline-hash">Hash: ${step.hash}</div>
                </div>
            `;
            container.appendChild(div);
        });

    } catch (err) {
        alert('Failed to query ledger: ' + err.message);
    }
}
