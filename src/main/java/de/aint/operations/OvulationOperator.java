package de.aint.operations;


import de.aint.models.Spectrum;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;

public class OvulationOperator {

        private static boolean exceedsStandartDerivation(double[] counts, float threshold) {
        //calculate mean and standard deviation
        double mean = 0;
        for(double count : counts) mean += count;
        mean /= counts.length;

        double variance = 0;
        for(double count : counts) variance += Math.pow(count - mean, 2);
        variance /= counts.length;
        double stdDev = Math.sqrt(variance);
        //check if any count exceeds mean + threshold * stdDev
        if(stdDev == 0) return false; //no variation, no outliers
        for(double count : counts){
            if(Math.abs(count - mean) > threshold * stdDev) return true;
        }

        return false;
    }

    //Savitzky-Golay
    //Default window_size = 7, polynomial_degree = 2
    private static double[] savitzkyGolay(int window_size, int polynomial_degree){

        int halfWindow = (window_size - 1) / 2;

        //Create design matrix A
        double[][] Adata = new double[window_size][polynomial_degree+1];
        int row = 0;
        for(int x = -halfWindow; x<=halfWindow; x++){
            for(int j = 0; j <= polynomial_degree; j++){
                Adata[row][j] = Math.pow(x, j);
            }
            row++;
        }

        RealMatrix A = new Array2DRowRealMatrix(Adata);

        //ATA = A^T * A
        RealMatrix ATA = A.transpose().multiply(A);

        //Inverse of ATA
        DecompositionSolver solver = new LUDecomposition(ATA).getSolver();
        RealMatrix ATAinv = solver.getInverse();

        //B = (A^T A)^(-1) * A^T
        RealMatrix B = ATAinv.multiply(A.transpose());

        //First row of b are weights for  a0 (x=0)
        double[] weights = B.getRow(0);

        //Normalize weight, so weights[] = 1
        double sum = 0.0;
        for (double w : weights) sum += w;
        if (sum == 0) sum = 1; // Avoid division by zero
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }

        return weights;
    }

    //Create Gauss Kernel
    private static double[] createGaussKernel(double sigma, int kernelSize) {
        double[] kernel = new double[kernelSize];
        double sum = 0.0;
        for (int i = 0; i < kernelSize; i++) {
            double x = i - ((double) kernelSize / 2);
            kernel[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        //Normalize the kernel
        if(sum == 0) sum = 1; // Avoid division by zero
        for (int i = 0; i < kernelSize; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    //Mirror index / for windows overlapping channel size ( < 0, > channel.size )
    private static int mirrorIndex(int index, int size) {
        if (index < 0) {
            return -index - 1;
        } else if (index >= size) {
            return 2 * size - index -1; 
        }
        return index;
    }


    //Gauss Smoothing
    public static Spectrum smoothSpectrumUsingGauss(Spectrum spec, double sigma){
        if(sigma <= 0) {
            return spec;
        }
        int radius = (int) Math.ceil(3 * sigma);
        int windowSize = 2 * radius + 1;

        double[] counts = spec.getCounts();
        double[] newCounts = new double[counts.length];
        double[] kernel = createGaussKernel(sigma, windowSize);

        for(int i = 0; i < counts.length; i++){
            double smoothedValue = 0.0;
            for(int j = i-radius; j <= i+radius; j++){
                int index = mirrorIndex(j, counts.length);
                smoothedValue += counts[index] * kernel[j - (i-radius)];
            }
            newCounts[i] = smoothedValue;
        }

        return new Spectrum(spec.getEnergy_per_channel(), newCounts);
    }


    public static Spectrum smoothSpectrum(Spectrum spec, int window_size, int polynomial_degree, boolean eraseOutliers, int iter){
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
                return new Spectrum(spec.getEnergy_per_channel(), smoothed_counts);
            }
            //Declare variables

            double[] weight = savitzkyGolay(window_size, polynomial_degree);
            //Smooth spectrum
            for(int i = half_window; i<spec.getChannel_count()-half_window; i++){
                double count = 0;
                //Check for false peaks
                if(eraseOutliers && exceedsStandartDerivation(Arrays.copyOfRange(counts, i-half_window, i+half_window+1), 5f)){
                    smoothed_counts[i] = counts[i];
                    continue;
                }
                double[] window = new double[window_size];
                window = Arrays.copyOfRange(counts, i-half_window, i+half_window+1);
                for(int j = -half_window; j<=half_window; j++){
                    count += counts[i+j] * weight[j+half_window];
                }
                smoothed_counts[i] = count;
                //half_window++;
            }

            //Lessen Winow size to avoid overshooting
            window_size -= 2;
            half_window = (window_size-1)/2;
        }


        return new Spectrum(spec.getEnergy_per_channel(), smoothed_counts);

    }
    //Func to create second derivative matrix for background estimation / curvature of spectrum
    private static RealMatrix createSecondDerivativeMatrix(int n) {
        int rows = n - 2;
        RealMatrix D = new Array2DRowRealMatrix(rows, n);

        for (int i = 0; i < rows; i++) {
            D.setEntry(i, i, 1.0);
            D.setEntry(i, i + 1, -2.0);
            D.setEntry(i, i + 2, 1.0);
        }
        return D;
    }

    //Func to build curvature penalty matrix for background estimation, outsorced to save ram
    private static RealMatrix buildCurvaturePenalty(int n, double lambda) {
    RealMatrix penalty = new Array2DRowRealMatrix(n, n);

    for (int i = 0; i < n; i++) {
        penalty.addToEntry(i, i, 6.0 * lambda);  //Main diagonal
        if (i > 0) penalty.addToEntry(i, i - 1, -4.0 * lambda); //diagonal left
        if (i < n - 1) penalty.addToEntry(i, i + 1, -4.0 * lambda); //diagonal right
        if (i > 1) penalty.addToEntry(i, i - 2, 1.0 * lambda);  //second left
        if (i < n - 2) penalty.addToEntry(i, i + 2, 1.0 * lambda);  //second right
    }

    return penalty;
}


    //arPLS Baseline correction using asymmetrically reweighted penalized least squares smoothing
    public static double[] estimateBackgroundUsingARPLS(Spectrum spec, double lambda, double epsilon, int maxIterations) {
        double[] counts = spec.getCounts();
        int cntLen = counts.length;
        //Create count vector for matrix multiplication
        RealVector CountsVec = new ArrayRealVector(counts);
        //Create second derivative matrix for curvature estimation
        RealMatrix SecondDerivat = createSecondDerivativeMatrix(cntLen);
        //penalty term for curvature
        RealMatrix CurvaturePenalty = buildCurvaturePenalty(cntLen, lambda);
        //Initialize weights w/1 since no peaks r known
        double[] weights = new double[cntLen];
        for (int i = 0; i < cntLen; i++) weights[i] = 1.0;
        //initialize estimated Background
        RealVector background = new ArrayRealVector(cntLen);

        //Iterate until convergence or max iterations reached
        for (int iter = 0; iter < maxIterations; iter++) {
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
            if (diff < epsilon) break;
            weights = newWeights;
        }

        return background.toArray();
    }


    public static double[] estimateBackgroundUsingALS(Spectrum spec, double lambda, double p, int maxIterations) {
        //initialize variables
        double[] counts = spec.getCounts();
        int cntLen = counts.length;
        double[] background = new double[cntLen];

        //initialize weights
        double[] weights = new double[cntLen];
        Arrays.fill(weights, 1.0);
        //Build curvature penalty matrix
        RealMatrix curvaturePenalty = buildCurvaturePenalty(cntLen, lambda);

        for(int iter = 0; iter < maxIterations; iter++) {
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
                newWeights[i] = counts[i] > background[i] ? p : 1.0 - p; //Above background -> weight low and opposite -> "ignore peak"
                delta += Math.abs(newWeights[i] - weights[i]);
            }
            weights = newWeights;
            if (delta < 1e-6) break;

        }

        return background;
    }

}
