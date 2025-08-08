const spectrumOptions = [
    { endpoint: "/", name: "Original", color: "#e63946" },
    { endpoint: "/smoothed", name: "Geglättet", color: "#457b9d", hasIterations: true },
    { endpoint: "/background", name: "Hintergrund", color: "#2a9d8f", hasIterations: true },
    { endpoint: "/smbackground", name: "SM Hintergrund", color: "#f1faee", hasIterations: false }
];

function fetchSpectrum(endpoint, iterations, windowSize, backgroundSource) {
    let url = 'http://localhost:7000' + endpoint;
    const params = [];
    if (iterations) params.push('iterations=' + iterations);
    if (windowSize) params.push('window_size=' + windowSize);
    if (backgroundSource) params.push('source=' + backgroundSource);
    if (params.length > 0) url += '?' + params.join('&');
    return fetch(url)
        .then(response => response.json())
        .catch(() => null);
}

function plotSelectedSpectra() {
    const checkedBoxes = Array.from(document.querySelectorAll('.spectrum-checkbox:checked'));
    const selectedEndpoints = checkedBoxes.map(cb => cb.value);

    Promise.all(
        spectrumOptions
            .filter(opt => selectedEndpoints.includes(opt.endpoint))
            .map(opt => {
                let iterations = null;
                let windowSize = null;
                let backgroundSource = null;
                if (opt.hasIterations) {
                    const input = document.querySelector(`.iteration-input[data-endpoint="${opt.endpoint}"]`);
                    iterations = input ? input.value : null;
                }
                if (opt.endpoint === "/smoothed") {
                    const winInput = document.querySelector(`.window-input[data-endpoint="/smoothed"]`);
                    windowSize = winInput ? winInput.value : null;
                }
                if (opt.endpoint === "/background") {
                    const bgSource = document.querySelector('.background-source[data-endpoint="/background"]');
                    backgroundSource = bgSource ? bgSource.value : null;
                }
                return fetchSpectrum(opt.endpoint, iterations, windowSize, backgroundSource).then(data => ({
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
                    (r.endpoint === "/smoothed" ? `, Window: ${document.querySelector('.window-input[data-endpoint="/smoothed"]').value}` : '') +
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

// Event Listener für Checkboxen und Inputfelder
document.querySelectorAll('.spectrum-checkbox').forEach(cb => {
    cb.addEventListener('change', plotSelectedSpectra);
});
document.querySelectorAll('.iteration-input').forEach(input => {
    input.addEventListener('input', plotSelectedSpectra);
});
document.querySelectorAll('.window-input').forEach(input => {
    input.addEventListener('input', plotSelectedSpectra);
});
document.querySelectorAll('.background-source').forEach(input => {
    input.addEventListener('change', plotSelectedSpectra);
});

let allIsotopes = [];

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
            <b>${iso.symbol}</b> (${iso.energy} keV)
            <br><span style="color:#457b9d;">Intensity:</span> ${iso.intensity}
            <br><span style="color:#457b9d;">Abundance:</span> ${iso.isotope_abundance}
        </div>`
    ).join('');
}

// Beim Seitenwechsel Isotope laden
document.getElementById('pageSelect').addEventListener('change', function() {
    const value = this.value;
    document.getElementById('plotPage').style.display = value === 'plot' ? '' : 'none';
    document.getElementById('isotopePage').style.display = value === 'isotopes' ? '' : 'none';
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
    }
});

// Filter bei Eingabe
document.getElementById('elementSearch').addEventListener('input', function() {
    const search = this.value.trim().toLowerCase();
    if (!search) {
        renderIsotopes(allIsotopes);
        return;
    }
    const filtered = allIsotopes.filter(iso => iso.symbol.toLowerCase().startsWith(search));
    renderIsotopes(filtered);
});