package de.aint.models;

public class RoiDTO {
    public double startEnergy;
    public double endEnergy;
    public double areaOverBackground;
    public Peak[] peaks;

    public RoiDTO(ROI roi) {
        this.startEnergy = roi.getStartEnergy();
        this.endEnergy = roi.getEndEnergy();
        this.areaOverBackground = roi.getAreaOverBackground();
        this.peaks = roi.getPeaks();
    }
}
