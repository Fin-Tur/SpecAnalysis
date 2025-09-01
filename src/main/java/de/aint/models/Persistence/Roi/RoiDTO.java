package de.aint.models.Persistence.Roi;

import de.aint.models.Peak;
import de.aint.models.ROI;

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
