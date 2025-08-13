# SpecAnalysis

SpecAnalysis is a web-based gamma spectrum analysis tool designed for isotope identification. It combines powerful visualization and analysis features with a user-friendly interface.

---

## Features

- **Isotope Identification**
  - Automatic detection and assignment of isotopes based on their characteristic energies (keV).
  - Support for manual isotope selection to identify unknown peaks (custom Spectrum).

- **Spectrum Visualization**
  - Browser-based interactive plots.
  - Options for original, smoothed, and custom spectra.
  - Counts vs. Energy (keV) displayed in logarithmic or linear scale.

- **Signal Processing**
  - Background noise fitting using various industry-standard algorithms using own C++ lib for faster usage (e.g ALS, arpLS).
  - Spectrum smoothing with adjustable parameters (e.g Savitzky-Golay, Gauss).
  - Custom graphs with selected isotopes for detailed analysis.

- **File Format Support**
  - Import spectra in **SPE format** and **MCNP format**.

---

## Usage Example

1. **Upload Spectrum**  
   Upload an SPE or MCNP file to display the spectrum.

2. **Adjust Visualization**  
   Choose between Original, Smoothed, or Custom view.

3. **Identify Isotopes**  
   Let the tool automatically detect isotopes or manually select peaks for analysis.

4. **Peak Analysis**  
   Unknown peaks are highlighted for easier manual assignment.

---

## Screenshots

### Plot View
<img width="1226" height="913" alt="ORgininalNew" src="https://github.com/user-attachments/assets/8af7b9c6-120b-4c3b-a605-36a45a63a573" />


### Smoothing Options
<img width="1213" height="898" alt="SmoothSettings" src="https://github.com/user-attachments/assets/00fa7a77-e29b-49eb-a815-21ce8cfdc0be" />


### Custom Graph with Isotope Selection
<img width="1262" height="838" alt="CustomGraph (2)" src="https://github.com/user-attachments/assets/0f959de7-5a1d-4231-a022-e7686b2f17c8" />
<img width="1243" height="910" alt="CustomGraph (1)" src="https://github.com/user-attachments/assets/2d638a5c-9ff6-42ad-a405-5d3bef65442b" />


### Peak Detection and Isotope Assignment
<img width="1269" height="931" alt="Peaks" src="https://github.com/user-attachments/assets/dfd5442a-91ab-4dc2-af7c-6ee1cf3ede88" />


---



## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
