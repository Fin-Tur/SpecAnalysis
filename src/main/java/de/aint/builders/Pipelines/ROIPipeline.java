package de.aint.builders.Pipelines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aint.detectors.MatchPeakWithIsotop;
import de.aint.detectors.PeakDetection;
import de.aint.models.*;

import de.aint.readers.IsotopeReader;

public final class ROIPipeline {

    private static final Logger logger = LoggerFactory.getLogger(ROIPipeline.class);

    private ROIPipeline(){}

    public static Process<Spectrum, ROI[]> specToRois(){
        return new Process<>() {
            @Override
            public ROI[] process(Spectrum input) throws ProcessException {
                ROI[] rois = PeakDetection.splitSpectrumIntoRois(input);
                logger.info("ROI Detection Successful!");
                return rois;
            }
        };
    }

    public static Process<ROI[], ROI[]> matchIsotopes(){
        return new Process<>() {
            @Override
            public ROI[] process(ROI[] input) throws ProcessException {
                IsotopeReader isoReader = new IsotopeReader(null);
                isoReader.readIsotopes();
                for (ROI roi : input) {
                    for (Peak peak : roi.getPeaks()) {
                        peak.setEstimatedIsotope(MatchPeakWithIsotop.matchRoiWithIsotop(peak, isoReader, 1));
                    }
                }
                logger.info("Isotope Matching Successful!");
                return input;
            }
        };
    }

    public static Process<ROI[], ROI[]> fitPeaks(){
        return new Process<>() {
            @Override
            public ROI[] process(ROI[] input) throws ProcessException {
                for (ROI roi : input) {
                    roi.fitGaussCurve();
                }
                logger.info("Peak Fitting Successful!");
                return input;
            }
        };
    }

    public static Process<ROI[], ROI[]> setAreaOverBackground(){
        return new Process<>() {
            @Override
            public ROI[] process(ROI[] input) throws ProcessException {
                for (ROI roi : input) {
                    for (Peak peak : roi.getPeaks()) {
                        peak.setAreaOverBackground();
                    }
                }
                logger.info("Area Over Background Calculation Successful!");
                return input;
            }
        };
    }
}