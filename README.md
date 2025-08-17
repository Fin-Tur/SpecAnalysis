# SpecAnalysis

SpecAnalysis is a web-based gamma spectrum analysis tool designed for isotope identification. It combines powerful visualization and analysis features with a user-friendly interface.

---

## Features

- **Isotope Identification**
  - Automatic detection and assignment of isotopes based on their characteristic energies (keV).
  - Support for manual isotope selection to identify unknown peaks.

- **Spectrum Visualization**
  - Browser-based interactive plots.
  - Options for original, smoothed, and custom spectra.
  - Counts vs. Energy (keV) displayed in logarithmic or linear scale.

- **Signal Processing**
  - Background noise fitting using various industry-standard algorithms (als, arpls, etc).
  - Spectrum smoothing with adjustable parameters (SG, Gauss, etc).
  - Custom graphs with selected isotopes for detailed analysis.

- **Peak Analysis**
  - Automatic peak detection with isotope assignment.
  - **Gaussian peak fitting** for automatically detected peaks.
  - Calculation of the **peak area** for quantitative analysis.
  - Visualization of fitted Gaussian peaks directly on the spectrum plot.

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

4. **Analyze Peaks**  
   Automatically detected peaks are modeled with Gaussian fits, their areas calculated, and the fitted peaks displayed on the plot.

---

## Screenshots

### Plot View
<img width="1206" height="912" alt="PlotDisplay" src="https://github.com/user-attachments/assets/20523da3-89b8-4fca-8e2a-97aa0450ba00" />


### Plot smoothing parameters
<img width="1213" height="898" alt="SmoothSettings" src="https://github.com/user-attachments/assets/56ff9c11-4dae-462d-ad56-37ec97fa209a" />


### Custom Graph with Isotope Selection
<img width="1262" height="838" alt="CustomGraph (2)" src="https://github.com/user-attachments/assets/797ee1f8-4f0a-4285-9bbc-9d2900831c35" />
<img width="1243" height="910" alt="CustomGraph (1)" src="https://github.com/user-attachments/assets/662d93f0-b1cf-472a-ae14-c9b6bd535ffb" />


### Peak Detection and Isotope Assignment
<img width="1269" height="931" alt="Peaks" src="https://github.com/user-attachments/assets/8dd893fd-e419-45a9-9791-2ae09795a9fe" />


---

## Installation & Usage

1. Clone the repository:
```bash
git clone https://github.com/USERNAME/SpecAnalysis.git
cd SpecAnalysis
```
2. Install dependencies:
```bash
pip install -r requirements.txt
```
3. Start the application:
```bash
javac API.java 
```
4. Open in your browser:
```
http://localhost:7000
```

---

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
