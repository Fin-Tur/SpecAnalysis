package de.aint.operations.fitters;


import java.util.Arrays;
import java.util.ArrayList;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aint.libraries.SmoothingLib;
import de.aint.models.ROI;
import de.aint.models.Spectrum;
import de.aint.operations.Helper;



public abstract class Fitter {

    private static final Logger logger = LoggerFactory.getLogger(Fitter.class);

//INTERFACE FOR FITTING ALGORITHMS
 interface FitAlgo {
    double[] fit(FittingData data);
}

//INTERFACE FOR PEAK-FITTING ALGORITHMS
interface PeakFitAlgo {
    double[] fit(ROI roi);
}


//FITTING-ALGORITHMS-BACKGROUND
public enum BackgroundFitAlgos implements FitAlgo {

    ALS{
        @Override
        public double[] fit(FittingData data) {
            return RunAlgos.estimateBackgroundUsingALS(data);
        }
    },
    ARPLS{
        @Override
        public double[] fit(FittingData data) {
            return RunAlgos.estimateBackgroundUsingARPLS(data);
        }
    },
    ALS_FAST{
        @Override
        public double[] fit(FittingData data) {
            return RunAlgos.estimateBackgroundUsingFastALS(data);
        }
    }


}

//FITTING-ALGORITHMS-SMOOTHING
public enum SmoothingFitAlgos implements FitAlgo {

    SG{
        @Override
        public double[] fit(FittingData data) {
            return RunAlgos.smoothSpectrumUsingSG(data);
        }
    },
    GAUSS{
        @Override
        public double[] fit(FittingData data) {
            return RunAlgos.smoothSpectrumUsingGauss(data);
        }
    }

}

//PEAK-FITTING-ALGOS

public enum PeakFitAlgos implements PeakFitAlgo {

    GAUSSLM{
        @Override
        public double[] fit(ROI roi) {
            return RunAlgos.fitGaussToROIUsingLM(roi);
        }
    }

}

private static class RunAlgos{


    //=================================================LM-GAUSS-PEAK-FITTER==================================================
    // !!! return params [B, sigma, A1, mu1, T1, G1, ..., An, mun, Tn, Gn] !!!
    public static double[] fitGaussToROIUsingLM(ROI roi){

        //Prepare ROI for Gauss fitting
        int channelBeg = Helper.findChannelFromEnergy(roi.getStartEnergy(), roi.getSpectrum().getEnergy_per_channel());
        int channelEnd = Helper.findChannelFromEnergy(roi.getEndEnergy(), roi.getSpectrum().getEnergy_per_channel());
        double[] E = Arrays.copyOfRange(roi.getSpectrum().getEnergy_per_channel(), channelBeg, channelEnd+1);
        double[] y = Arrays.copyOfRange(roi.getSpectrum().getCounts(), channelBeg, channelEnd+1);
        double[] background = Arrays.copyOfRange(roi.getBackgroundSpectrum().getCounts(), channelBeg, channelEnd+1);

        //Guess initial Parameters
        double[] start = new double[2 + 4 * (roi.getPeaks().length)];
        double[] muSet = new double[roi.getPeaks().length];
        double[] Aset = new double[roi.getPeaks().length];
        start[0] = (background[0]+background[background.length-1]) / 2 ; //Baseline
        start[1] = roi.getSpectrum().getFwhmForNumber(Helper.findChannelFromEnergy(roi.getPeaks()[0].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())) / 2.35; //Sigma
        for (int i = 0; i < roi.getPeaks().length; i++) {
            start[2 + 4 * i] = roi.getSpectrum().getCounts()[Helper.findChannelFromEnergy(roi.getPeaks()[i].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())]-start[0]; //Amplitude
            Aset[i] = roi.getSpectrum().getCounts()[Helper.findChannelFromEnergy(roi.getPeaks()[i].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())]-start[0];
            start[3 + 4 * i] = roi.getPeaks()[i].getPeakCenter(); //Mu
            muSet[i] = roi.getPeaks()[i].getPeakCenter(); //Store mu for projection
            start[4 + 4 * i] = 0.5; //Relative Tailing Amplitude
            start[5 + 4 * i] = 1.5; //Gradient of Tailing
        }

        
        double bSet = (background[0]+background[background.length-1]) / 2;
        int maxIter = 100;

        logger.info("Fitting Gaussian to ROI with {} peaks", roi.getPeaks().length);

        return LMPeakFitting.fit(E, y, start, maxIter, bSet, muSet, Aset, LMPeakFitting.calculateWeight(E, y, muSet, 4.5, 2.0, start[1]));

    }

    //==================================================GAUSS================================================================

      private static double[] smoothSpectrumUsingGauss(FittingData data) {

        Spectrum spec = data.spectrum;
        double sigma = data.gaussSigma;

        if(sigma <= 0) {
            return spec.getCounts(); // Return original counts if sigma is not positive
        }
        int radius = (int) Math.ceil(3 * sigma);
        int windowSize = 2 * radius + 1;

        double[] counts = spec.getCounts();
        double[] newCounts = new double[counts.length];
        double[] kernel = FitterHelper.createGaussKernel(sigma, windowSize);

        for(int i = 0; i < counts.length; i++){
            double smoothedValue = 0.0;
            for(int j = i-radius; j <= i+radius; j++){
                int index = FitterHelper.mirrorIndex(j, counts.length);
                smoothedValue += counts[index] * kernel[j - (i-radius)];
            }
            newCounts[i] = smoothedValue;
        }
        logger.info("Smoothed spectrum using Gaussian with sigma {}", sigma);
        return newCounts;
    }

    //=======================================================SG=============================================================

    private static double[] smoothSpectrumUsingSG(FittingData data) {
        Spectrum spec = data.spectrum;
        int window_size = data.sgWindowSize;
        int polynomial_degree = data.sgPolynomialDegree;
        boolean eraseOutliers = data.sgEraseOutliers;
        int iter = data.sgIters;

        //Window size has to be odd to ensure symetry
        if(window_size % 2 == 0){
            window_size++;
        }
        //Declare some variables
        double[] smoothed_counts = new double[spec.getChannel_count()];
        double[] counts = spec.getCounts();
        int half_window = (window_size-1)/2;
        //Iterations begin
        for(int iters = 0; iters < iter; iters++){
            //If window size is too small, return original spectrum
            if(window_size < 3) {
                return smoothed_counts;
            }
            //Declare variables

            double[] weight = FitterHelper.createSavitzkyGolayKernel(window_size, polynomial_degree);
            //Smooth spectrum
            for(int i = 0; i<spec.getChannel_count(); i++){
                double count = 0;
                //Check for false peaks
                if(eraseOutliers && FitterHelper.exceedsStandardDeviation(Arrays.copyOfRange(counts, Math.max(i-half_window, 0), Math.min(i+half_window+1, counts.length-1)), 5f)){
                    smoothed_counts[i] = counts[i];
                    continue;
                }
                double[] window = new double[window_size];
                window = Arrays.copyOfRange(counts, Math.max(i-half_window, 0), Math.min(i+half_window+1, counts.length-1));
                for(int j = -half_window; j<=half_window; j++){
                    count += counts[FitterHelper.mirrorIndex(i+j, counts.length)] * weight[FitterHelper.mirrorIndex(j+half_window, weight.length)];
                }
                smoothed_counts[i] = count;
                //half_window++;
            }

            //Lessen Winow size to avoid overshooting
            window_size -= 2;
            half_window = (window_size-1)/2;
        }

        logger.info("Smoothed spectrum using Savitzky-Golay with window size {} and polynomial degree {}", window_size, polynomial_degree);
        return smoothed_counts;

    }

    //========================================================ALS===========================================================

     private static double[] estimateBackgroundUsingALS(FittingData data) {
        //initialize variables
        double[] counts = data.spectrum.getCounts();
        int cntLen = counts.length;
        double[] background = new double[cntLen];

        //initialize weights
        double[] weights = new double[cntLen];
        Arrays.fill(weights, 1.0);
        //Build curvature penalty matrix
        RealMatrix curvaturePenalty = FitterHelper.buildCurvaturePenalty(cntLen, data.lambda);

        for(int iter = 0; iter < data.maxIter; iter++) {
            //Create diagonal matrix W with weights
            RealMatrix Weights = MatrixUtils.createRealDiagonalMatrix(weights);
            // A = Weights + curvature penalty
            RealMatrix A = Weights.add(curvaturePenalty);
            //Calculate counts * weight
            double[] weighted_counts = new double[cntLen];
            for (int i = 0; i < cntLen; i++){ 
                weighted_counts[i] = weights[i] * counts[i];
                
            }
            RealVector weighted_counts_Vec = new ArrayRealVector(weighted_counts);
            //Solve the system of equations cholesky cuz A is symmetric and positive for sure
            DecompositionSolver solver = new CholeskyDecomposition(A).getSolver();
            RealVector background_Vec = solver.solve(weighted_counts_Vec);
            background = background_Vec.toArray();
            //Calculate new weights based on the difference between estimated background and counts
            //Break if weights do not change significantly
            double delta = 0.0;
            double[] newWeights = new double[cntLen];
            for (int i = 0; i < cntLen; i++) {
                newWeights[i] = counts[i] > background[i] ? data.p : 1.0 - data.p; //Above background -> weight low and opposite -> "ignore peak"
                delta += Math.abs(newWeights[i] - weights[i]);
            }
            weights = newWeights;
            if (delta < 1e-6) break;

        }
        logger.info("Estimated background using ALS");
        return background;
    }

    //============================================ARPLS==============================================================================

    private static double[] estimateBackgroundUsingARPLS(FittingData data) {
        double[] counts = data.spectrum.getCounts();
        int cntLen = counts.length;
        //Create count vector for matrix multiplication
        RealVector CountsVec = new ArrayRealVector(counts);
        //Create second derivative matrix for curvature estimation
        RealMatrix SecondDerivat = FitterHelper.createSecondDerivativeMatrix(cntLen);
        //penalty term for curvature
        RealMatrix CurvaturePenalty = FitterHelper.buildCurvaturePenalty(cntLen, data.lambda);
        //Initialize weights w/1 since no peaks r known
        double[] weights = new double[cntLen];
        for (int i = 0; i < cntLen; i++) weights[i] = 1.0;
        //initialize estimated Background
        RealVector background = new ArrayRealVector(cntLen);

        //Iterate until convergence or max iterations reached
        for (int iter = 0; iter < data.maxIter; iter++) {
            //Diagonal matrix W with weights // peaks are weighted less
            RealMatrix W = MatrixUtils.createRealDiagonalMatrix(weights);
            //Create the system of equations : aims to balance penalty minimization and fit to data
            RealMatrix A = W.add(CurvaturePenalty);
            //Takes forever!!!! ~2min each iteration :(
            DecompositionSolver solver = new CholeskyDecomposition(A).getSolver();
            RealVector Wy = CountsVec.ebeMultiply(new ArrayRealVector(weights));
            //Estimates new background
            background = solver.solve(Wy);
            //Calculate new weights based on the difference between estimated background and counts
            //d = CountsVec - background so, where is spectrum above background, d is positive
            RealVector d = CountsVec.subtract(background);
            double[] differenceCountsToBackground = d.toArray();
            //Calculate weights based on the difference
            //initialize mean and std for negative values
            double meanNeg = 0.0;
            double stdNeg = 0.0;
            int countNeg = 0;
            //Only consider negative values for std and mean // weights are calculated based on the negative values since peaks are ignored
            for (double v : differenceCountsToBackground) {
                if (v < 0) {
                    meanNeg += v;
                    countNeg++;
                }
            }
            //Calculate sum of all negative values
            if (countNeg > 0) {
                meanNeg /= countNeg;
                //Calculate standard deviation of negative values
                for (double v : differenceCountsToBackground) {
                    if (v < 0) {
                        stdNeg += Math.pow(v - meanNeg, 2);
                    }
                }
                stdNeg = Math.sqrt(stdNeg / countNeg);
            }
            //Initialize new weights
            //If stdNeg is 0, it means no negative values, so we can skip weight calculation
            double[] newWeights = new double[cntLen];
            if (stdNeg == 0) { newWeights = Arrays.copyOf(weights, weights.length); }
            else {
                for (int i = 0; i < cntLen; i++) {
                    //If the difference is negative it means the count is below the background, so we increase the weight
                    //oppsoite for positive values
                    newWeights[i] = 1.0 / (1.0 + Math.exp(2.0 * (differenceCountsToBackground[i] - (2 * stdNeg - meanNeg)) / stdNeg));
                }
            }
            //Check breakoff term : if values of weights do not change significantly, we can stop iterating
            double diff = 0.0;
            for (int i = 0; i < cntLen; i++) {
                diff += Math.abs(weights[i] - newWeights[i]);
            }
            diff /= cntLen;
            if (diff < data.p) break;
            weights = newWeights;
        }
        logger.info("Estimated background using ARPLS");
        return background.toArray();
    }


    //==========================================FAST_ALS===================================================================

    private static double[] estimateBackgroundUsingFastALS(FittingData data) {
        double[] background = new double[data.spectrum.getCounts().length];
        SmoothingLib.INSTANCE.estimate_background_als(data.spectrum.getCounts(), data.spectrum.getCounts().length, data.lambda, data.p, data.maxIter, background);
        //Cancel weird formation since y axis is log in display
        double[] energy = data.spectrum.getEnergy_per_channel();
        for(int i = 0; i < background.length; i++) {
            if (background[i] < 0.2 && energy[i] > 10000) {
                background[i] = 0; // Avoid log(0)
            }
        }
        logger.info("Estimated background using Fast ALS");
        return background;
    }

//===============================================================================================================================================
    
}


}


