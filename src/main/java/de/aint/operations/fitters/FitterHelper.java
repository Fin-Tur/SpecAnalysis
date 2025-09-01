package de.aint.operations.fitters;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public abstract class FitterHelper {

    //Func to build curvature penalty matrix for background estimation, outsorced to save ram
    public static RealMatrix buildCurvaturePenalty(int n, double lambda) {
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


    //Func to create second derivative matrix for background estimation / curvature of spectrum
    public static RealMatrix createSecondDerivativeMatrix(int n) {
        int rows = n - 2;
        RealMatrix D = new Array2DRowRealMatrix(rows, n);

        for (int i = 0; i < rows; i++) {
            D.setEntry(i, i, 1.0);
            D.setEntry(i, i + 1, -2.0);
            D.setEntry(i, i + 2, 1.0);
        }
        return D;
    }

    //Mirror index / for windows overlapping channel size ( < 0, > channel.size )
    public static int mirrorIndex(int index, int size) {
        if (index < 0) {
            return -index - 1;
        } else if (index >= size) {
            return 2 * size - index -1; 
        }else{
            return index;
        }
    }

    public static boolean exceedsStandardDeviation(double[] counts, float threshold) {
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
    public static double[] createSavitzkyGolayKernel(int window_size, int polynomial_degree){

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
    public static double[] createGaussKernel(double sigma, int kernelSize) {
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

    


}
