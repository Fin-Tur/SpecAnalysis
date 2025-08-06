package de.aint.operations;


import de.aint.models.Spectrum;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
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

        //Inverse von ATA
        DecompositionSolver solver = new LUDecomposition(ATA).getSolver();
        RealMatrix ATAinv = solver.getInverse();

        //B = (A^T A)^(-1) * A^T
        RealMatrix B = ATAinv.multiply(A.transpose());

        //First row of b are weights for  a0 (x=0)
        double[] weights = B.getRow(0);


        //Normalize weight, so weights[] = 1
        double sum = 0.0;
        for (double w : weights) sum += w;
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }

        return weights;
    }


    public static Spectrum smoothSpectrum(Spectrum spec, int window_size, int polynomial_degree, boolean eraseOutliers){
        //Window size has to be odd to ensure symetry
        if(window_size % 2 == 0){
            return null;
        }
        int half_window = (window_size-1)/2;
        //Declare variables
        double[] weight = savitzkyGolay(window_size, polynomial_degree);
        double[] smoothed_counts = new double[spec.getChannel_count()];
        double[] counts = spec.getCounts();
        //Smooth spectrum
        for(int i = half_window; i<spec.getChannel_count()-half_window; i++){
            double count = 0;
            //Check for false peaks
            if(eraseOutliers && exceedsStandartDerivation(Arrays.copyOfRange(counts, i-half_window, i+half_window+1), 3f)){
                smoothed_counts[i] = counts[i];
                continue;
            }
            double[] window = new double[window_size];
            window = Arrays.copyOfRange(counts, i-half_window, i+half_window+1);
            for(int j = -half_window; j<=half_window; j++){
                count += counts[i+j] * weight[j+half_window];
            }
            smoothed_counts[i] = count;
        }


        return new Spectrum(spec.getEnergy_per_channel(), smoothed_counts);

    }



}
