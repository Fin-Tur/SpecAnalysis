package de.aint.operations.fitters;

import de.aint.models.*;

public class FittingData{

    //STANDART VALUES
    private static class GenericOpts {
        //BACKGROUND-VALUES
        static final double lambda = 2e4;
        static final double p = 8e-4;
        static final int maxIter = 50;
        //SMOOTHING-VALUES
        static final int sgWindowSize = 11;
        static final int sgIters = 1;
        static final boolean sgEraseOutliers = true;
        static final int sgPolynomialDegree = 2;
        static final double gaussSigma = 3.0;
        

    }

    //VALUES
    final Spectrum spectrum;
    //BG
    double lambda;
    double p;
    int maxIter;
    //SM
    int sgWindowSize;
    int sgIters;
    boolean sgEraseOutliers;
    int sgPolynomialDegree;
    double gaussSigma;

    //Contructor w all musts
   public FittingData(Spectrum spectrum) {
        this.spectrum = spectrum;
        this.lambda = GenericOpts.lambda;
        this.p = GenericOpts.p;
        this.maxIter = GenericOpts.maxIter;
        this.sgWindowSize = GenericOpts.sgWindowSize;
        this.sgIters = GenericOpts.sgIters;
        this.gaussSigma = GenericOpts.gaussSigma;
        this.sgEraseOutliers = GenericOpts.sgEraseOutliers;
        this.sgPolynomialDegree = GenericOpts.sgPolynomialDegree;
    }


    //Setters

    //BG
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public void setP(double p) {
        this.p = p;
    }

    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    //SM
    public void setSgWindowSize(int sgWindowSize) {
        this.sgWindowSize = sgWindowSize;
    }

    public void setSgIters(int sgIters) {
        this.sgIters = sgIters;
    }

    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;
    }

    public void setSgEraseOutliers(boolean sgEraseOutliers) {
        this.sgEraseOutliers = sgEraseOutliers;
    }

    public void setSgPolynomialDegree(int sgPolynomialDegree) {
        this.sgPolynomialDegree = sgPolynomialDegree;
    }

}
