window.selectedFile = null;
document.getElementById('fileInput').addEventListener('change', function(e) {
    window.selectedFile = e.target.files[0] || null;
    document.getElementById('fileName').textContent = window.selectedFile ? window.selectedFile.name : '';
    plotSelectedSpectra();
});

const spectrumOptions = [
    { endpoint: "/", name: "Original", color: "#e63946" },
    { endpoint: "/custom", name: "Custom", color: "#aa1a92ff"},
    { endpoint: "/smoothed", name: "Geglättet", color: "#457b9d", hasIterations: true },
    { endpoint: "/background", name: "Hintergrund", color: "#2a9d8f", hasIterations: false },
    { endpoint: "/smbackground", name: "SM Hintergrund", color: "#f1faee", hasIterations: false }
];

function fetchSpectrum(endpoint, iterations, windowSize, backgroundSource, customSource, customIsotopes) {
    if (endpoint === "/" && window.selectedFile) {
        // File-Upload for Original
        const formData = new FormData();
        formData.append('file', window.selectedFile);
        return fetch('http://localhost:7000/', {
            method: 'POST',
            body: formData
        }).then(response => response.json())
          .catch(() => null);
    }
    let url = 'http://localhost:7000' + endpoint;
    const params = [];
    // Algorithmus ermitteln
    let algorithm = null;
    if (endpoint === "/smoothed") {
        const algoSelect = document.querySelector('.algorithm-select[data-endpoint="/smoothed"]');
        if (algoSelect) {
            algorithm = algoSelect.value;
            params.push('algorithm=' + encodeURIComponent(algorithm));
        }
    }
    // Nur für SG: Iterationen und Window Size, für Gauss: Sigma
    if (endpoint === "/smoothed" && algorithm === "SG") {
        if (iterations) params.push('iterations=' + iterations);
        if (windowSize) params.push('window_size=' + windowSize);
    }
    if (endpoint === "/smoothed" && algorithm === "Gauss") {
        const sigmaInput = document.querySelector('.sigma-input[data-endpoint="/smoothed"]');
        if (sigmaInput) params.push('sigma=' + sigmaInput.value);
    }
    if (backgroundSource) params.push('source=' + backgroundSource);
    if (customIsotopes && customIsotopes.length > 0) params.push('isotopes=' + customIsotopes.join(','));
    if (customSource) params.push('source=' + customSource);
    // Add algorithm parameter for /smoothed endpoint
    if (endpoint === "/smoothed") {
        const algoSelect = document.querySelector('.algorithm-select[data-endpoint="/smoothed"]');
        if (algoSelect) {
            params.push('algorithm=' + encodeURIComponent(algoSelect.value));
        }
    }
    if (params.length > 0) url += '?' + params.join('&');
    return fetch(url)
        .then(response => response.json())
        .catch(() => null);
}

let allIsotopes = [];
let selectedIsotopeIds = new Set();

function renderIsotopes(filtered) {
    const container = document.getElementById('isotopeInfo');

    if (!document.getElementById('elementSearch').value.trim()) {
        container.innerHTML = '<div style="color:#888;">Please enter an element symbol.</div>';
        return;
    }
    if (!filtered.length) {
        container.innerHTML = '<div style="color:#888;">No Isotopes found.</div>';
        return;
    }
    container.innerHTML = filtered.map(iso =>
        `<div class="isotope-card">
            <label style="display:flex;align-items:center;gap:8px;">
                <input type="checkbox" class="isotope-checkbox" value="${iso.id}" ${selectedIsotopeIds.has(iso.id) ? 'checked' : ''}>
                <span>
                    <b>${iso.symbol}</b> (${iso.energy} keV)
                    <br><span style="color:#457b9d;">Intensity:</span> ${iso.intensity}
                    <br><span style="color:#457b9d;">Abundance:</span> ${iso.isotope_abundance}
                </span>
            </label>
        </div>`
    ).join('');

    // Event Listener für alle Checkboxen nach dem Rendern
    container.querySelectorAll('.isotope-checkbox').forEach(cb => {
        cb.addEventListener('change', function() {
            if (cb.checked) {
                selectedIsotopeIds.add(cb.value);
            } else {
                selectedIsotopeIds.delete(cb.value);
            }
        });
    });
}

//Render Peaks
function renderPeaks(peaks){
    const container = document.getElementById("peakInfo");
    if(peaks.length == 0) {container.innerText = 'No Peaks found!'; return;}
    container.innerHTML = peaks.map(peak => 
        peak.estimatedIsotope == "UNK" ? 
        `<div class="peak-card-unk">
            <label style="display:flex;align-items:center;gap:8px;">
                <span>
                    <b>${peak.estimatedIsotope}</b>
                    <br><span style="color:#457b9d;">Energy (keV):</span> ${peak.peakCenter}
                </span>
            </label>
        </div>`
        :
        `<div class="peak-card">
            <label style="display:flex;align-items:center;gap:8px;">
                <span>
                    <b>${peak.estimatedIsotope}</b>
                    <br><span style="color:#457b9d;">Energy (keV):</span> ${peak.peakCenter}
                    <br><span style="color:#457b9d;">Abundance:</span> ${peak.matchedIsotope.isotope_abundance}
                </span>
            </label>
        </div>`
    ).join('');

    //List of all found isotopes ex UNK and ANNH
    const found = Array.from(new Set(
        peaks
            .filter(peak => peak.estimatedIsotope && peak.estimatedIsotope !== "UNK" && peak.estimatedIsotope != "ANNH")
            .map(peak => peak.estimatedIsotope)
    ));
    document.getElementById("foundElements").innerHTML =
        found.length
            ? `<span id="foundElements-label">Recognized Isotopes:</span> ${found.map(e => `<span class="found-chip">${e}</span>`).join('')}`
            : '';
}

// Beim Custom-Plot: IDs aus dem Set verwenden!
function plotSelectedSpectra() {
    const checkedBoxes = Array.from(document.querySelectorAll('.spectrum-checkbox:checked'));
    const selectedEndpoints = checkedBoxes.map(cb => cb.value);

    Promise.all(
        spectrumOptions
            .filter(opt => selectedEndpoints.includes(opt.endpoint))
            .map(opt => {
                let iterations = null;
                let windowSize = null;
                let sigma = null;
                let backgroundSource = null;
                let customIsotopes = null;
                // Für SG: Iterationen und Window Size, für Gauss: Sigma
                if (opt.endpoint === "/smoothed") {
                    const algoSelect = document.querySelector('.algorithm-select[data-endpoint="/smoothed"]');
                    const algorithm = algoSelect ? algoSelect.value : "SG";
                    if (algorithm === "SG") {
                        const input = document.querySelector(`.iteration-input[data-endpoint="/smoothed"]`);
                        iterations = input ? input.value : null;
                        const winInput = document.querySelector(`.window-input[data-endpoint="/smoothed"]`);
                        windowSize = winInput ? winInput.value : null;
                    }
                    if(algorithm === "GAUSS"){
                        const sigmaInput = document.querySelector(`.sigma-input[data-endpoint="/smoothed"]`);
                        sigma = sigmaInput ? sigmaInput.value : null;
                    }
                    // Für Gauss: Iterationen und Window Size ignorieren, stattdessen sigma
                }
                if (opt.endpoint === "/background") {
                    const bgSource = document.querySelector('.background-source[data-endpoint="/background"]');
                    backgroundSource = bgSource ? bgSource.value : null;
                }
                let customSource = null;
                if (opt.endpoint === "/custom") {
                    // IDs der ausgewählten Isotope aus dem Set nehmen
                    customIsotopes = Array.from(selectedIsotopeIds);
                    // Source aus dem Select neben Custom holen
                    const customSourceSelect = document.querySelector('.custom-source[data-endpoint="/custom"]');
                    if (customSourceSelect) {
                        customSource = customSourceSelect.value;
                    }
                }
                return fetchSpectrum(opt.endpoint, iterations, windowSize, sigma, backgroundSource, customSource, customIsotopes).then(data => ({
                    ...opt,
                    data
                }));
            })
    ).then(results => {
        const traces = results
            .filter(r => r.data && r.data.counts && r.data.energy_per_channel)
            .map((r, idx) => ({
                x: r.data.energy_per_channel,
                y: r.data.counts,
                type: 'scatter',
                mode: 'lines',
                name: r.name +
                    (r.hasIterations ? ` (Iterationen: ${document.querySelector(`.iteration-input[data-endpoint="${r.endpoint}"]`)?.value || ''})` : '') +
                    (r.endpoint === "/smoothed" ? `, Window: ${document.querySelector('.window-input[data-endpoint="/smoothed"]').value}, Algo: ${document.querySelector('.algorithm-select[data-endpoint="/smoothed"]').value}` : '') +
                    (r.endpoint === "/background" ? ` (${document.querySelector('.background-source[data-endpoint="/background"]').selectedOptions[0].text})` : ''),
                line: { color: r.color, width: 3 }
            }));

        const layout = {
            title: 'Counts Plot',
            plot_bgcolor: '#f8f9fa',
            paper_bgcolor: '#f8f9fa',
            xaxis: { title: 'Energy [keV]', gridcolor: '#e0e1dd' },
            yaxis: { 
                title: 'Counts',
                type: 'log',
                gridcolor: '#e0e1dd'
            },
            font: { family: 'Segoe UI, Arial, sans-serif', color: '#22223b' }
        };

        Plotly.newPlot('plot', traces, layout, {responsive: true});
    });
}

// Initiales Plotten
plotSelectedSpectra();

// Event Listener für Checkboxen
document.querySelectorAll('.spectrum-checkbox').forEach(cb => {
    cb.addEventListener('change', plotSelectedSpectra);
});
document.querySelectorAll('.background-source').forEach(input => {
    input.addEventListener('change', plotSelectedSpectra);
});

// Smoothed Settings Overlay Handling
// Umschalten der Settings je nach Algorithmus
const smoothedAlgorithmSelect = document.getElementById('smoothedAlgorithmSelect');
const sgSettingsDiv = document.getElementById('sgSettings');
const gaussSettingsDiv = document.getElementById('gaussSettings');
if (smoothedAlgorithmSelect && sgSettingsDiv && gaussSettingsDiv) {
    smoothedAlgorithmSelect.addEventListener('change', function() {
        if (this.value === 'SG') {
            sgSettingsDiv.style.display = '';
            gaussSettingsDiv.style.display = 'none';
        } else {
            sgSettingsDiv.style.display = 'none';
            gaussSettingsDiv.style.display = '';
        }
    });
}
const smoothedSettingsBtn = document.getElementById('smoothedSettingsBtn');
const smoothedSettingsOverlay = document.getElementById('smoothedSettingsOverlay');
const smoothedSettingsClose = document.getElementById('smoothedSettingsClose');
if (smoothedSettingsBtn && smoothedSettingsOverlay && smoothedSettingsClose) {
    smoothedSettingsBtn.addEventListener('click', () => {
        smoothedSettingsOverlay.style.display = 'flex';
    });
    smoothedSettingsClose.addEventListener('click', () => {
        smoothedSettingsOverlay.style.display = 'none';
        plotSelectedSpectra();
    });
    // Optional: Schließen bei Klick außerhalb des Dialogs
    smoothedSettingsOverlay.addEventListener('click', (e) => {
        if (e.target === smoothedSettingsOverlay) {
            smoothedSettingsOverlay.style.display = 'none';
        }
    });
}
document.getElementById('clearSelectedIsotopes').addEventListener('click', function() {
    // Alle ausgewählten Isotope zurücksetzen
    selectedIsotopeIds.clear()
    });
document.getElementById('elementSearch').addEventListener('input', function() {
    const searchTerm = this.value.toLowerCase();
    const filteredIsotopes = allIsotopes.filter(iso => iso.symbol.toLowerCase().includes(searchTerm));
    renderIsotopes(filteredIsotopes);
});

function loadAndRenderPeaks() {
    fetch("http://localhost:7000/peaks")
        .then(res => res.json())
        .then(data => {
            renderPeaks(data);
        })
        .catch(() => {
            document.getElementById("peakInfo").innerText = "No Peaks found!";
        });
}

// Beim Seitenwechsel
document.getElementById('pageSelect').addEventListener('change', function() {
    const value = this.value;
    document.getElementById('plotPage').style.display = value === 'plot' ? '' : 'none';
    document.getElementById('isotopePage').style.display = value === 'isotopes' ? '' : 'none';
    document.getElementById('peakPage').style.display = value === 'peaks' ? '' : 'none';
    if (value === 'isotopes') {
        fetch('http://localhost:7000/isotopes')
            .then(res => res.json())
            .then(data => {
                allIsotopes = data;
                renderIsotopes(allIsotopes);
            })
            .catch(() => {
                document.getElementById('isotopeInfo').innerText = 'An Error occurred while loading isotope data.';
            });
    } else if (value === "peaks") {
        loadAndRenderPeaks();
    }
});

// Optional: Auch beim erstmaligen Laden, falls "peaks" vorausgewählt ist
window.addEventListener('DOMContentLoaded', function() {
    const pageSelect = document.getElementById('pageSelect');
    const value = pageSelect.value;
    document.getElementById('plotPage').style.display = value === 'plot' ? '' : 'none';
    document.getElementById('isotopePage').style.display = value === 'isotopes' ? '' : 'none';
    document.getElementById('peakPage').style.display = value === 'peaks' ? '' : 'none';
    if (value === 'plot') {
        plotSelectedSpectra();
    } else if (value === 'isotopes') {
        fetch('http://localhost:7000/isotopes')
            .then(res => res.json())
            .then(data => {
                allIsotopes = data;
                renderIsotopes(allIsotopes);
            })
            .catch(() => {
                document.getElementById('isotopeInfo').innerText = 'An Error occurred while loading isotope data.';
            });
    } else if (value === "peaks") {
        loadAndRenderPeaks();
    }
});

