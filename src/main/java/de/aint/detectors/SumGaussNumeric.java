package de.aint.detectors;

import de.aint.models.*;
import de.aint.operations.Helper;

import java.util.Arrays;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.Pair;


import org.apache.commons.math3.special.Erf;


public final class SumGaussNumeric {

    //Changed allowed
    static final double muRangeRadius = 0;
    static final double ARangeRadius = 100;

    private static double sigmaMinFromE(double[] E){
        if (E.length < 2) return 1e-8; //Avoid division by zero
        double dE = Math.abs(E[1] - E[0]);
        return Math.max(0.3 * dE, 1e-8);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double[] calculateWeight(double[] E, double[] y, double[] muSet, double alpha, double kreach, double sigma) {
        //Poisson weights
        double[] weight = new double[y.length];
        for(int i = 0; i < y.length; i++) {
            weight[i] = 1/Math.sqrt(Math.max(y[i], 1)); 
        }
         
        //Flank Boost
        double[] g = new double[y.length];
        for(int i = 0; i < y.length; i++) {
            double di = Double.POSITIVE_INFINITY;
            double ei = E[i];
            for (var mu : muSet){
                di = Math.min(di, Math.abs(ei - mu));
            }
            g[i] = 1 + alpha * ( 1 - Math.cos(Math.PI * Math.min(1, di / (kreach*sigma))));
        }

        //Normalize and add weights
        double sum = 0.0;
        for(int i = 0; i < y.length; i++) {
            weight[i] *= g[i];
            sum += weight[i];
        }
        if (sum == 0) sum = 1;
        double mean = sum / y.length;
        for(int i = 0; i < y.length; i++) {
            weight[i] /= mean;
        }
        
        return weight;
    }

    private static double[] project(double[] p, double sigMin, double Bset, double muSet[], double[] Aset){
        double[] q = p.clone();
        q[0] = Bset;
        q[1] = Math.max(p[1], sigMin);

        int nPeaks = (q.length - 2) / 4;
        for(int i = 0; i < nPeaks; i++) {
            int offset = 2 + 4*i;
            q[offset] = clamp(q[offset], Aset[i] - ARangeRadius, Aset[i] + ARangeRadius); // Ensure A doesn't change a lot
            q[offset + 1] = clamp(q[offset + 1], muSet[i] - muRangeRadius, muSet[i] + muRangeRadius); // Ensure mu doesnt change alot
            q[offset + 2] = Math.max(q[offset + 2], 0.3); // Ensure T is non-negative
            q[offset + 3] = Math.max(q[offset + 3], 1e-3); // Ensure G is non-negative
        }

        return q;
    }

    //p = [ B, sigma, A1, mu1, T1, G1, A2, mu2, T2, G2, ..., An, mun, Tn, Gn ]
    //Calculates counts for modelled gauss func
    private static double[] value(double[] E, double[] p){
        double B = p[0];
        double sigma = Math.max(p[1], 1e-8);
        double inv2s2 = 1.0 / (2.0 * sigma * sigma); 
        int numberOfPeaks = (p.length - 2)/4;

        double[] newY = new double[E.length];
        for (int i = 0; i < E.length; i++) {
            double sum = B;
            for (int k = 0; k < numberOfPeaks; k++) {
                double A  = p[2 + 4*k];
                double mu = p[3 + 4*k];
                double T  = p[4 + 4*k]; 
                double G =  p[5 + 4*k];
                double z  = E[i] - mu;
                double delta = Math.sqrt(2)*sigma;

                double core = Math.exp(-z*z * inv2s2);
                double tail = 0.5*T * Math.exp(z / (G * delta)) * Erf.erfc((z / delta) + 1.0 / (2.0 * G));
                //sum += A * core;
                sum += A * (core + tail); //Add peak contribution);

            }
            newY[i] = sum;
        }
        return newY;
    }

    //Numeric Jacobian
    private static MultivariateJacobianFunction numericModel(double[] E, double bSet, double[] muSet, double[] Aset){
        return point -> {
            double[] p = point.toArray(); //Gets parameters for model
            double[] pBase = project(p, sigmaMinFromE(E), bSet, muSet, Aset); //Project parameters to ensure they are valid
            double[] y = value(E, pBase); //Gets values for current gauss modell

            int numberOfPoints = y.length, numberOfParameters = p.length;
            double[][] J = new double[numberOfPoints][numberOfParameters];//Jacobian matrix

            for (int j = 0; j < numberOfParameters; j++) { //IIterates over parameters

                double parameter = p[j];
                double difference  = 1e-6 * (Math.abs(parameter) + 1.0);
                p[j] = parameter + difference; //Change parameter a little bit

                double[] pPerturbed = project(p, sigmaMinFromE(E), bSet, muSet, Aset); //Project parameters to ensure they are valid

                double[] y2 = value(E, pPerturbed); //Calculates values for changed parameter
                p[j] = parameter; //Return to original parameter
                double invDiff = 1.0 / difference;
                for (int i = 0; i < numberOfPoints; i++) J[i][j] = (y2[i] - y[i]) * invDiff; //Numeric partial derivation   (y2-y)/difference
            }
            return new Pair<>(new ArrayRealVector(y, false),
                              new Array2DRowRealMatrix(J, false));
        };
    }

    //returns p = [B, sigma, A1, mu1, T1, B1, ...]
    public static double[] fit(double[] E, double[] y, double[] start, int maxIter, double bCap, double[] muSet, double[] Aset, double[] w){


        System.out.println("Building problem...");
        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(numericModel(E, bCap, muSet, Aset))
                .target(new ArrayRealVector(y, false))
                .start(new ArrayRealVector(start, false))
                .weight(new DiagonalMatrix(w))
                .maxIterations(Math.max(1, maxIter))
                .maxEvaluations(1000 * Math.max(1, maxIter))
                .build();

        Optimum opt = new LevenbergMarquardtOptimizer().optimize(problem);
        double[] p = opt.getPoint().toArray();
        return project(p, sigmaMinFromE(E), bCap, muSet, Aset);
        
    }

    public static double[] fitGaussToROI(ROI roi){

        //Prepare ROI for Gauss fitting
        int channelBeg = Helper.findChannelFromEnergy(roi.getStartEnergy(), roi.getSpectrum().getEnergy_per_channel());
        int channelEnd = Helper.findChannelFromEnergy(roi.getEndEnergy(), roi.getSpectrum().getEnergy_per_channel());
        double[] E = Arrays.copyOfRange(roi.getSpectrum().getEnergy_per_channel(), channelBeg, channelEnd+1);
        double[] y = Arrays.copyOfRange(roi.getSpectrum().getCounts(), channelBeg, channelEnd+1);
        double[] background = Arrays.copyOfRange(roi.getBackgroundSpectrum().getCounts(), channelBeg, channelEnd+1);

        //Guess initial Parameters
        double[] start = new double[2 + 4 * (roi.peaks.length)];
        double[] muSet = new double[roi.peaks.length];
        double[] Aset = new double[roi.peaks.length];
        start[0] = (background[0]+background[background.length-1]) / 2 ; //Baseline
        start[1] = roi.getSpectrum().getFwhmForNumber(Helper.findChannelFromEnergy(roi.peaks[0].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())) / 2.35; //Sigma
        for (int i = 0; i < roi.peaks.length; i++) {
            start[2 + 4 * i] = roi.getSpectrum().getCounts()[Helper.findChannelFromEnergy(roi.peaks[i].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())]-start[0]; //Amplitude
            Aset[i] = roi.getSpectrum().getCounts()[Helper.findChannelFromEnergy(roi.peaks[i].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())]-start[0];
            start[3 + 4 * i] = roi.peaks[i].getPeakCenter(); //Mu
            muSet[i] = roi.peaks[i].getPeakCenter(); //Store mu for projection
            start[4 + 4 * i] = 0.5; //Relative Tailing Amplitude
            start[5 + 4 * i] = 1.5; //Gradient of Tailing
        }

        
        double bSet = (background[0]+background[background.length-1]) / 2;
        int maxIter = 100;

        double[]params = fit(E, y, start, maxIter, bSet, muSet, Aset, calculateWeight(E, y, muSet, 4.5, 2.0, start[1]));
        System.out.println("Starting parameters: " + Arrays.toString(start));
        roi.setFitParams(params);
        System.out.println("Fitted parameters: " + Arrays.toString(params)+"\n");
        return params;
    }
}
