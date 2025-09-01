package de.aint.operations.fitters;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.util.Pair;

public class LMPeakFitting {
    //Allow alghorithm to change variable in x radius
    static final double muRangeRadius = 0;
    static final double ARangeRadius = 0;
    static final double Bradius = 50;

    private static double sigmaMinFromE(double[] E){
        if (E.length < 2) return 1e-8; //Avoid division by zero
        double dE = Math.abs(E[1] - E[0]);
        return Math.max(0.3 * dE, 1e-8);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double[] calculateWeight(double[] E, double[] y, double[] muSet, double alpha, double kreach, double sigma) {
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
        q[0] = clamp(q[0], Bset - Bradius, Bset); // Clamp radius to negative, 0 to positive side (dont want B++++ sigma---- to happen)
        q[1] = Math.max(p[1], sigMin);

        int nPeaks = (q.length - 2) / 5;
        for(int i = 0; i < nPeaks; i++) {
            int offset = 2 + 5*i;
            q[offset] = clamp(q[offset], Aset[i] - ARangeRadius, Aset[i] + ARangeRadius); // Ensure A doesn't change a lot
            q[offset + 1] = clamp(q[offset + 1], muSet[i] - muRangeRadius, muSet[i] + muRangeRadius); // Ensure mu doesnt change alot
            q[offset + 2] = Math.max(q[offset + 2], 0.3); // Ensure T is non-negative
            q[offset + 3] = Math.max(q[offset + 3], 1e-3); // Ensure G is non-negative
            q[offset + 4] = Math.max(q[offset + 4], 1e-3); // Ensure S is non-negative
        }

        return q;
    }

    //p = [ B, sigma, A1, mu1, T1, G1, S1, A2, mu2, T2, G2, S2, ..., An, mun, Tn, Gn, Sn]
    //Calculates counts for modelled gauss func
    private static double[] value(double[] E, double[] p){
        double B = p[0];
        double sigma = Math.max(p[1], 1e-8);
        double inv2s2 = 1.0 / (2.0 * sigma * sigma); 
        int numberOfPeaks = (p.length - 2)/5;

        double[] newY = new double[E.length];
        for (int i = 0; i < E.length; i++) {
            double sum = B;
            for (int k = 0; k < numberOfPeaks; k++) {
                double A  = p[2 + 5*k];
                double mu = p[3 + 5*k];
                double T  = p[4 + 5*k]; 
                double G =  p[5 + 5*k];
                double S =  p[6 + 5*k];
                double z  = E[i] - mu;
                double delta = Math.sqrt(2)*sigma;

                double core = Math.exp(-z*z * inv2s2);
                double tail = 0.5*T * Math.exp(z / (G * delta)) * Erf.erfc((z / delta) + 1.0 / (2.0 * G));
                double step = 0.5*S * Erf.erfc((z / delta));
                //sum += A * core;
                sum += A * (core + tail + step); //Add peak contribution);

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

    //returns p = [B, sigma, A1, mu1, T1, B1, S1, ...]
    public static double[] fit(double[] E, double[] y, double[] start, int maxIter, double bCap, double[] muSet, double[] Aset, double[] w){


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
}
