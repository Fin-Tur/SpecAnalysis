package de.aint.operations.calculators;

import de.aint.models.*;

public class CalculatorData {

    public enum OperationType{
        NUMERIC,
        AREA
    }

    final Spectrum spectrum1;
    final Spectrum spectrum2;
    final OperationType operationType;
    final ROI roi;


    public CalculatorData(OperationType operationType, Spectrum spectrum1, Spectrum spectrum2) {
        if(operationType != OperationType.NUMERIC){
            throw new IllegalArgumentException("This constructor is only for NUMERIC operations, your arguments do not match the expected types.");
        }
        this.spectrum1 = spectrum1;
        this.spectrum2 = spectrum2;
        this.roi = null;
        this.operationType = operationType;
    }

    public CalculatorData(OperationType operationType, ROI roi) {
        if(operationType != OperationType.AREA){
            throw new IllegalArgumentException("This constructor is only for AREA operations, your arguments do not match the expected types.");
        }
        this.roi = roi;
        this.spectrum1 = null;
        this.spectrum2 = null;
        this.operationType = operationType;
    }
}
