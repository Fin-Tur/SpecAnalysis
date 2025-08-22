package de.aint.models;


public class Peak {

        private final double peakCenter;

        private String estimatedIsotope = null;
        private Isotop matchedIsotope = null;


        public Peak(double peakCenter) {
            this.peakCenter = peakCenter;
        }

        //Getters
        public double getPeakCenter() {
            return peakCenter;
        }

        public String getEstimatedIsotope() {
        return estimatedIsotope;}

        public Isotop getMatchedIsotope(){
            return matchedIsotope;
        }

        //Setters

        public void setEstimatedIsotope(Isotop isotope) {
            if(isotope == null) {
                this.estimatedIsotope="unk"; 
            }else{
                this.estimatedIsotope = isotope.symbol;
                this.matchedIsotope = isotope;
            }
        }
}
