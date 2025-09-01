/**
 * Spectral Analysis Frontend (refactored, English, modular)
 * --------------------------------------------------------
 * - Manages multiple spectra (sidebar: add/rename/remove/select)
 * - Talks to backend endpoints for original/custom/smoothed/background/peaks/isotopes
 * - Renders plot with Plotly and details for isotopes/peaks
 * - Keeps all IDs/classes/data-* hooks used in index.html intact
 */

// -----------------------------
// Global state
// -----------------------------
window.spectra = [];           // [{ id, name, fileName, data }]
window.activeSpectrumIdx = null;
var projectName = "";

//Small helpers
const API_BASE = "http://localhost:7000";
const $ = (sel, root=document) => root.querySelector(sel);
const $$ = (sel, root=document) => Array.from(root.querySelectorAll(sel));

function safeJson(res) { return res.ok ? res.json() : Promise.reject(new Error(res.statusText)); }

function resetAppState(){
    window.spectra = [];
    window.activeSpectrumIdx = null;
     const list = document.querySelector("#spectrumList");
    if (list) list.innerHTML = "";
    const fn = document.querySelector("#fileName");
    if (fn) fn.textContent = "";
    if (window.Plotly) Plotly.purge("plot");
}

function loadProjectInformationAndSpectra(){

    projectName = new URLSearchParams(window.location.search).get('project');

  if (projectName) {
    fetch(API_BASE + "/projects/" + encodeURIComponent(projectName))
      .then(res => res.json())
      .then(spectraList => {
        window.spectra = [];
        spectraList.forEach(spec => {
          // Use backend ID for all further requests
          window.spectra.push({
            name: spec.name || `Spectrum ${window.spectra.length + 1}`,
            fileName: spec.fileName || spec.name || 'Untitled',
            data: spec,
            id: spec.id, // Use backend ID

          });
        });
        if (window.spectra.length > 0) setActiveSpectrum(0);
        renderSpectrumSidebar();
      })
      .catch(() => alert('Error loading project '+projectName));
  }
}

// -----------------------------
// Spectrum list (sidebar)
// -----------------------------
function addSpectrum(spectrum, name, fileName) {

  // Add a new spectrum to the project and use the backend ID for future requests
  fetch(API_BASE + '/projects/addSpectrum?projectName=' + encodeURIComponent(projectName) + '&spectrumName=' + encodeURIComponent(name || fileName), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(spectrum)
  })
  .then(response => {
    if (!response.ok) {
      console.log("Error adding spectrum:", response.statusText);
      return response.text().then(msg => { throw new Error(msg); });
    }
    //Expect backend to return the new spectrum with its ID
    return response.json();
  })
  .then(backendSpec => {
    window.spectra.push({
      name: backendSpec.name || name || fileName || `Spectrum ${window.spectra.length + 1}`,
      fileName: backendSpec.fileName || fileName || `Untitled`,
      data: backendSpec,
      id: backendSpec.id
    });
    console.log("Added spectrum w ID: ", backendSpec.id); //HERE
    setActiveSpectrum(window.spectra.length - 1);
    renderSpectrumSidebar();
  })
  .catch(err => alert('Error: ' + err.message));
}



function setActiveSpectrum(idx) {
  window.activeSpectrumIdx = idx;
  renderSpectrumSidebar();
  updateFileNameLabel();
  // Refresh current page
  const page = $("#pageSelect")?.value || "plot";
  if (page === "plot") plotSelectedSpectra();
  if (page === "isotopes") filterAndRenderIsotopes();
  if (page === "peaks") loadAndRenderPeaks();
}

function removeSpectrum(idx) {
  const current = window.spectra[idx];
  if (idx === window.activeSpectrumIdx) {
    window.spectra.splice(idx, 1);
    window.activeSpectrumIdx = window.spectra.length ? 0 : null;
  } else {
    window.spectra.splice(idx, 1);
    if (window.activeSpectrumIdx !== null && idx < window.activeSpectrumIdx) {
      window.activeSpectrumIdx -= 1;
    }
  }
  console.log("Deleting spectrum with ID:", current.id);
  fetch(API_BASE+"/spectrum/delete", {headers: {"X-Spectrum-Id": current.id}}).then(
    response => {
      if(!response.ok) {
        console.error("Error deleting spectrum on backend: " + response.statusText);
      }
    }
  );
  renderSpectrumSidebar();
  updateFileNameLabel();
  plotSelectedSpectra();
}

function renameSpectrum(idx) {
  const current = window.spectra[idx];
  console.log("Renaming spectrum with ID:", current.id);
  const name = prompt("Rename spectrum:", current?.name ?? "");
  if (!name) return;

  fetch(API_BASE+"/spectrum/rename?newName=" + encodeURIComponent(name), {headers: {"X-Spectrum-Id": current.id}}).then(
    response => {
      if (!response.ok) {
        alert("Error renaming spectrum on backend: " + response.statusText);
      }else{
        current.name = name;
      }
      renderSpectrumSidebar();
      updateFileNameLabel();
    }
  );
}

function renderSpectrumSidebar() {
  const list = $("#spectrumList");
  if (!list) return;
  list.innerHTML = "";
  window.spectra.forEach((spec, idx) => {
    const li = document.createElement("li");
    li.className = "spectrum-list-item" + (idx === window.activeSpectrumIdx ? " active" : "");
    li.style.display = "flex";
    li.style.alignItems = "center";
    li.style.justifyContent = "space-between";
    li.style.padding = "6px 0";
    li.innerHTML = `
      <span class="spectrum-name" style="flex:1; cursor:pointer; ${idx === window.activeSpectrumIdx ? "font-weight:bold;color:#22223b;" : ""}">${spec.name}</span>
      <button class="rename-btn" title="Rename" style="margin-left:6px;">✏️</button>
      <button class="remove-btn" title="Remove" style="margin-left:4px;">🗑️</button>
    `;
    li.querySelector(".spectrum-name").onclick = () => setActiveSpectrum(idx);
    li.querySelector(".remove-btn").onclick = (e) => {
      e.stopPropagation();
      if (confirm("Remove spectrum?")) removeSpectrum(idx);
    };
    li.querySelector(".rename-btn").onclick = (e) => {
      e.stopPropagation();
      renameSpectrum(idx);
    };
    list.appendChild(li);
  });
}

function updateFileNameLabel() {
  const label = $("#fileName");
  if (!label) return;
  const active = window.spectra[window.activeSpectrumIdx] || null;
  label.textContent = active ? (active.fileName || active.name) : "";
}

// -----------------------------
// Upload flow (file -> backend -> addSpectrum)
// -----------------------------
function wireUploadControls() {
  const addBtn = $("#addSpectrumBtn");
  const fileInput = $("#fileInput");
  if (!addBtn || !fileInput) return;

  addBtn.addEventListener("click", () => fileInput.click());
  fileInput.addEventListener("change", (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);

    fetch(API_BASE + "/", { method: "POST", body: formData })
      .then(safeJson)
      .then((data) => addSpectrum(data, file.name.replace(/\.[^/.]+$/, ""), file.fileName))
      .catch((err) => alert("Upload failed: " + err.message));
  });
}

// -----------------------------
/** Fetch a processed spectrum from the backend for the CURRENT active spectrum.
 * Handles all endpoints (original/custom/smoothed/background/smbackground).
 * For non-original endpoints, sends `X-Spectrum-Id` header with current spectrum id.
 */
// -----------------------------
const spectrumOptions = [
  { endpoint: "/", name: "Original", color: "#e63946" },
  { endpoint: "/custom", name: "Custom", color: "#aa1a92ff" },
  { endpoint: "/smoothed", name: "Smoothed", color: "#457b9d", hasIterations: true },
  { endpoint: "/background", name: "Background", color: "#2a9d8f", hasIterations: false },
  { endpoint: "/smbackground", name: "SM Background", color: "#f1faee", hasIterations: false }
];

async function fetchSpectrum(endpoint, iterations, windowSize, sigma, backgroundSource, customSource, customIsotopes) {
  const active = window.spectra[window.activeSpectrumIdx];
  if (!active) {
    return Promise.resolve(null);
  }

  let url = API_BASE + endpoint;
  const params = [];
 

  // Smoothed algorithm & params
  if (endpoint === "/smoothed") {
    const algoSelect = $('.algorithm-select[data-endpoint="/smoothed"]');
    const algorithm = algoSelect ? algoSelect.value : null;
    if (algorithm) params.push("algorithm=" + encodeURIComponent(algorithm));
    if (algorithm === "SG") {
      if (iterations) params.push("iterations=" + iterations);
      if (windowSize) params.push("window_size=" + windowSize);
    } else if (algorithm === "Gauss") {
      const sigmaInput = $('.sigma-input[data-endpoint="/smoothed"]');
      if (sigmaInput) params.push("sigma=" + sigmaInput.value);
    }
  }

  // Background source
  if (endpoint === "/background") {
    if (backgroundSource) params.push("source=" + backgroundSource);
  }

  // Custom: optional isotopes & source (isotopes/peaks)
  if (endpoint === "/custom") {
    if (customIsotopes && customIsotopes.length > 0) params.push("isotopes=" + customIsotopes.join(","));
    if (customSource) params.push("source=" + customSource);
  }

  if (params.length) url += "?" + params.join("&");

  return await fetch(url, { headers: { "X-Spectrum-Id": active.id } }).then(safeJson);
}

// -----------------------------
// Plotting
// -----------------------------
function plotSelectedSpectra() {
  const plotDivId = "plot";
  if (window.activeSpectrumIdx === null || !window.spectra[window.activeSpectrumIdx]) {
    if (window.Plotly) Plotly.purge(plotDivId);
    return;
  }

  const checkedEndpoints = $$(".spectrum-checkbox:checked").map(cb => cb.value);

  const tasks = spectrumOptions
    .filter(opt => checkedEndpoints.includes(opt.endpoint))
    .map(opt => {
      // collect params for each endpoint
      let iterations=null, windowSize=null, sigma=null, backgroundSource=null, customSource=null, customIsotopes=null;
      if (opt.endpoint === "/smoothed") {
        const iter = $('.iteration-input[data-endpoint="/smoothed"]');
        const win = $('.window-input[data-endpoint="/smoothed"]');
        iterations = iter ? parseInt(iter.value, 10) : null;
        windowSize = win ? parseInt(win.value, 10) : null;
      }
      if (opt.endpoint === "/background") {
        const bgSource = $('.background-source[data-endpoint="/background"]');
        backgroundSource = bgSource ? bgSource.value : null;
      }
      if (opt.endpoint === "/custom") {
        customIsotopes = Array.from(selectedIsotopeIds);
        const sel = $('.custom-source[data-endpoint="/custom"]');
        customSource = sel ? sel.value : null;
      }
      return  fetchSpectrum(opt.endpoint, iterations, windowSize, sigma, backgroundSource, customSource, customIsotopes)
        .then(data => ({ ...opt, data }));
    });

  Promise.all(tasks).then(results => {
    const traces = results
      .filter(r => r && r.data)
      .map(r => ({
        x: r.data.energy_per_channel,
        y: r.data.counts,
        type: "scatter",
        mode: "lines",
        name: r.name,
        line: { color: r.color }
      }));

    const layout = {
      margin: { l: 60, r: 30, t: 30, b: 60 },
      xaxis: { title: "Energy (keV)" },
      yaxis: { title: "Counts", type: "log" },
      legend: { orientation: "h" }
    };

    if (window.Plotly) Plotly.newPlot(plotDivId, traces, layout, { responsive: true });
  }).catch(err => {
    console.error("TEST");
    alert("Plot failed: " + err.message);
  });
}

// -----------------------------
// Isotopes (list + selection)
// -----------------------------
let allIsotopes = [];                // array of isotopes from backend
let selectedIsotopeIds = new Set();  // Set<string> of selected isotope IDs

function renderIsotopes(isotopes) {
  const container = $("#isotopeInfo");
  if (!container) return;

  const query = $("#elementSearch")?.value.trim();
  if (!query) {
    container.innerHTML = '<div style="color:#888;">Please enter an element symbol.</div>';
    return;
  }
  if (!isotopes.length) {
    container.innerHTML = '<div style="color:#888;">No isotopes found.</div>';
    return;
  }

  container.innerHTML = isotopes.map(iso => `
    <div class="isotope-card">
      <label style="display:flex;align-items:center;gap:8px;">
        <input type="checkbox" class="isotope-select" value="${iso.isotope_id}" ${selectedIsotopeIds.has(iso.isotope_id) ? "checked" : ""}>
        <div>
          <div><b>${iso.element_symbol}${iso.mass_number ?? ""}</b> <span style="color:#666;">(${iso.isotope_id})</span></div>
          <div style="font-size:12px;color:#666;">Energy lines: ${iso.energy_lines?.length ?? 0}</div>
        </div>
      </label>
    </div>
  `).join("");

  // wire up selection toggles
  $$(".isotope-select", container).forEach(cb => {
    cb.addEventListener("change", () => {
      if (cb.checked) selectedIsotopeIds.add(cb.value);
      else selectedIsotopeIds.delete(cb.value);
      // auto-refresh plot if custom endpoint is shown
      plotSelectedSpectra();
    });
  });
}

function filterAndRenderIsotopes() {
  const active = window.spectra[window.activeSpectrumIdx];
  if (!active) {
    $("#isotopeInfo").innerHTML = '<div style="color:#888;">Load a spectrum first.</div>';
    return;
  }

  const query = $("#elementSearch")?.value.trim().toLowerCase() || "";
  // If we have not fetched isotopes for this spectrum yet, fetch once
  // (depends on your backend; here we fetch by active spectrum id/header)
  fetch(API_BASE + "/isotopes", { headers: { "X-Spectrum-Id": active.id } })
    .then(safeJson)
    .then(list => {
      allIsotopes = list || [];
      const filtered = allIsotopes.filter(iso =>
        (iso.element_symbol ?? "").toLowerCase().includes(query) ||
        (iso.isotope_id ?? "").toLowerCase().includes(query)
      );
      renderIsotopes(filtered);
    })
    .catch(err => {
      console.error(err);
      $("#isotopeInfo").innerHTML = '<div style="color:#c00;">Failed to load isotopes.</div>';
    });
}

// -----------------------------
// Peaks (ROIs + peaks details)
// -----------------------------
function renderPeaks(peaksPayload) {
  const foundEls = $("#foundElements");
  const peakInfo = $("#peakInfo");
  if (!foundEls || !peakInfo) return;

  if (!peaksPayload) {
    foundEls.innerHTML = "";
    peakInfo.innerHTML = '<div style="color:#888;">No peaks found.</div>';
    return;
  }

  // Collect unique isotopes that were matched
  const foundIsotopes = new Set();
  let roisHtml = "";

  (peaksPayload || []).forEach((roi, idx) => {
    let roiHtml = `<div class="roi-card">
      <div><b>ROI #${idx + 1}</b> — <span style="color:#457b9d;">Energy Range:</span> ${roi.energyRange?.join(" - ") ?? "-"}</div>
      <div><span style="color:#457b9d;">Net Area:</span> ${roi.netArea ?? "-"}
      ${roi.areaOverBackground !== undefined ? ` | <b>Area:</b> ${Number(roi.areaOverBackground).toFixed(2)}` : ""}</div>
    `;

    if (Array.isArray(roi.peaks) && roi.peaks.length) {
      roiHtml += roi.peaks.map(peak => {
        if (peak.estimatedIsotope && peak.estimatedIsotope !== "UNK" && peak.estimatedIsotope !== "ANNH") {
          foundIsotopes.add(peak.estimatedIsotope);
        }
        const unk = peak.estimatedIsotope === "UNK";
        return `<div class="peak-card${unk ? "-unk" : ""}">
          <div><b>${peak.estimatedIsotope || "?"}</b></div>
          <div><span style="color:#457b9d;">Energy (keV):</span> ${peak.peakCenter?.toFixed?.(2) ?? peak.peakCenter ?? "-"}</div>
          ${peak.matchedIsotope && peak.matchedIsotope.isotope_id ? `<div><span style="color:#457b9d;">Matched:</span> ${peak.matchedIsotope.isotope_id}</div>` : ""}
          ${peak.matchedIsotope && peak.matchedIsotope.isotope_abundance ? `<div><span style="color:#457b9d;">Abundance:</span> ${peak.matchedIsotope.isotope_abundance}</div>` : ""}
        </div>`;
      }).join("");
    } else {
      roiHtml += `<div class="peak-card-unk">No peaks inside this ROI.</div>`;
    }

    roiHtml += `</div>`;
    roisHtml += roiHtml;
  });

  foundEls.innerHTML = [...foundIsotopes].sort().map(el => `<span class="chip">${el}</span>`).join(" ");
  peakInfo.innerHTML = roisHtml || '<div style="color:#888;">No ROIs available.</div>';
}

function loadAndRenderPeaks() {
  const active = window.spectra[window.activeSpectrumIdx];
  const peakInfo = $("#peakInfo");
  if (!active) { if (peakInfo) peakInfo.innerHTML = '<div style="color:#888;">Load a spectrum first.</div>'; return; }

  fetch(API_BASE + "/peaks", { headers: { "X-Spectrum-Id": active.id } })
    .then(safeJson)
    .then(payload => renderPeaks(payload))
    .catch(err => {
      console.error(err);
      if (peakInfo) peakInfo.innerHTML = '<div style="color:#c00;">Failed to load peaks.</div>';
    });
}

// -----------------------------
// UI wiring (checkboxes, selects, overlay, pages)
// -----------------------------
function wirePlotControls() {
  // spectrum checkboxes
  $$(".spectrum-checkbox").forEach(cb => cb.addEventListener("change", plotSelectedSpectra));

  // inputs for smoothed (iterations, window) and others
  $$(".iteration-input, .window-input, .sigma-input, .background-source, .custom-source").forEach(inp => {
    inp.addEventListener("change", plotSelectedSpectra);
  });

  // Algorithm select toggles SG/Gauss parameter blocks
  const algo = $("#smoothedAlgorithmSelect");
  const sg = $("#sgSettings");
  const gauss = $("#gaussSettings");
  if (algo && sg && gauss) {
    algo.addEventListener("change", () => {
      const val = algo.value;
      sg.style.display = (val === "SG") ? "block" : "none";
      gauss.style.display = (val === "Gauss") ? "block" : "none";
      plotSelectedSpectra();
    });
  }

  // Smoothed overlay open/close
  const settingsBtn = $("#smoothedSettingsBtn");
  const overlay = $("#smoothedSettingsOverlay");
  const closeBtn = $("#smoothedSettingsClose");
  if (settingsBtn && overlay && closeBtn) {
    settingsBtn.addEventListener("click", () => overlay.style.display = "block");
    closeBtn.addEventListener("click", () => overlay.style.display = "none");
    overlay.addEventListener("click", (e) => { if (e.target === overlay) overlay.style.display = "none"; });
  }
}

function wireIsotopeControls() {
  $("#clearSelectedIsotopes")?.addEventListener("click", () => {
    selectedIsotopeIds.clear();
    filterAndRenderIsotopes();
    plotSelectedSpectra();
  });

  $("#elementSearch")?.addEventListener("input", () => filterAndRenderIsotopes());
}

function wirePageSwitch() {
  $("#pageSelect")?.addEventListener("change", function () {
    const val = this.value;
    $("#plotPage").style.display = (val === "plot") ? "block" : "none";
    $("#isotopePage").style.display = (val === "isotopes") ? "block" : "none";
    $("#peakPage").style.display = (val === "peaks") ? "block" : "none";

    if (val === "plot") plotSelectedSpectra();
    if (val === "isotopes") filterAndRenderIsotopes();
    if (val === "peaks") loadAndRenderPeaks();
  });
}

// -----------------------------
// Boot
// -----------------------------


window.addEventListener("DOMContentLoaded", () => {
    resetAppState();
    //Gather specs from project
    loadProjectInformationAndSpectra();
    //Booting functions
  wireUploadControls();
  wirePlotControls();
  wireIsotopeControls();
  wirePageSwitch();

  // Default page: plot
  $("#pageSelect").value = "plot";
  $("#plotPage").style.display = "block";
  $("#isotopePage").style.display = "none";
  $("#peakPage").style.display = "none";

  updateFileNameLabel();
  renderSpectrumSidebar();
  plotSelectedSpectra();
});
