package de.aint.detectors;

import de.aint.builders.SpectrumBuilder;
import de.aint.models.*;
import de.aint.operations.Helper;

import java.util.Arrays;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.Pair;

public final class SumGaussNumeric {

    //p = [ B, sigma, A1, mu1, A2, mu2, ..., An, mun ]
    //Calculates counts for modelled gauss func
    private static double[] value(double[] E, double[] p){
        double B = p[0];
        double sigma = Math.max(p[1], 1e-8);
        double inv2s2 = 1.0 / (2.0 * sigma * sigma); 
        int numberOfPeaks = (p.length - 2)/2;

        double[] newY = new double[E.length];
        for (int i = 0; i < E.length; i++) {
            double sum = B;
            for (int k = 0; k < numberOfPeaks; k++) {
                double A  = p[2 + 2*k];
                double mu = p[3 + 2*k];
                double z  = E[i] - mu;
                sum += A * Math.exp(-z*z * inv2s2);
            }
            newY[i] = sum;
        }
        return newY;
    }

    //Numeric Jacobian
    private static MultivariateJacobianFunction numericModel(double[] E, double bCap){
        return point -> {
            double[] p = point.toArray(); //Gets parameters for model
            double[] y = value(E, p); //Gets values for current gauss modell

            int numberOfPoints = y.length, numberOfParameters = p.length;
            double[][] J = new double[numberOfPoints][numberOfParameters];//Jacobian matrix

            for (int j = 0; j < numberOfParameters; j++) { //IIterates over parameters

                double parameter = p[j];
                double difference  = 1e-6 * (Math.abs(parameter) + 1.0);
                p[j] = parameter + difference; //Change parameter a little bit

                //Dont change mu to constraint peak melting
                if(j >= 3 && j % 2 == 1) { // mu
                    p[j] = parameter;
                }

                //Pretend B from going too high up in high peaks (=/ overfitting)
                if (j == 0) {
                    p[j] = bCap;
                }

                double[] y2 = value(E, p); //Calculates values for changed parameter
                p[j] = parameter; //Return to original parameter
                double invDiff = 1.0 / difference;
                for (int i = 0; i < numberOfPoints; i++) J[i][j] = (y2[i] - y[i]) * invDiff; //Numeric partial derivation   (y2-y)/difference
            }
            return new Pair<>(new ArrayRealVector(y, false),
                              new Array2DRowRealMatrix(J, false));
        };
    }

    //returns p = [B, sigma, A1, mu1, ...]
    public static double[] fit(double[] E, double[] y, double[] start, int maxIter, double bCap){

        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .model(numericModel(E, bCap))
                .target(new ArrayRealVector(y, false))
                .start(new ArrayRealVector(start, false))
                .maxIterations(Math.max(1, maxIter))
                .maxEvaluations(1000 * Math.max(1, maxIter))
                .build();

        Optimum opt = new LevenbergMarquardtOptimizer().optimize(problem);
        double[] p = opt.getPoint().toArray();
        p[1] = Math.max(p[1], 1e-8); //Sigma > 0
        return p;
    }

    public static double[] fitGaussToROI(ROI roi){

        //Prepare ROI for Gauss fitting
        int channelBeg = Helper.findChannelFromEnergy(roi.getStartEnergy(), roi.getSpectrum().getEnergy_per_channel());
        int channelEnd = Helper.findChannelFromEnergy(roi.getEndEnergy(), roi.getSpectrum().getEnergy_per_channel());
        double[] E = Arrays.copyOfRange(roi.getSpectrum().getEnergy_per_channel(), channelBeg, channelEnd+1);
        double[] y = Arrays.copyOfRange(roi.getSpectrum().getCounts(), channelBeg, channelEnd+1);
        double[] background = Arrays.copyOfRange(roi.getBackgroundSpectrum().getCounts(), channelBeg, channelEnd+1);

        //Guess initial Parameters
        double[] start = new double[2 + 2 * (roi.peaks.length)];
        start[0] = (background[0]+background[background.length-1]) / 2 ; //Baseline
        start[1] = roi.getSpectrum().getFwhmForNumber(Helper.findChannelFromEnergy(roi.peaks[0].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())) / 2.35; //Sigma
        for (int i = 0; i < roi.peaks.length; i++) {
            start[2 + 2 * i] = roi.getSpectrum().getCounts()[Helper.findChannelFromEnergy(roi.peaks[i].getPeakCenter(), roi.getSpectrum().getEnergy_per_channel())]-start[0]; //Amplitude
            start[3 + 2 * i] = roi.peaks[i].getPeakCenter(); //Mu
        }

        
        double bCap = start[0];
        //for(var num : y) bCap += num;
        //bCap /= Math.max(1, y.length);

        int maxIter = 100;

        double[]params = fit(E, y, start, maxIter, bCap);
        System.out.println("Starting parameters: " + Arrays.toString(start));
        roi.setFitParams(params);
        System.out.println("Fitted parameters: " + Arrays.toString(params)+"\n");
        return params;
    }
}
