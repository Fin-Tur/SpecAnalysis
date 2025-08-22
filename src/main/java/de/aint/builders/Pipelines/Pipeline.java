package de.aint.builders.Pipelines;

import java.util.Objects;



public final class Pipeline<IN, OUT>{

    //Composition of Processes
    private final Process<IN, OUT> currentProcess;
    //Static factory 
    public static <IN, OUT> Pipeline<IN, OUT> of(Process<IN, OUT> first) {
        return new Pipeline<>(first);
    }

    private Pipeline(Process<IN, OUT> currentProcess) {
        this.currentProcess = currentProcess;
    }

    public <NewOut> Pipeline<IN, NewOut> then(Process<OUT, NewOut> newProcess){
        Objects.requireNonNull(newProcess, "New process cannot be null");
        return new Pipeline<>(input -> newProcess.process(currentProcess.process(input)));
    }

    OUT execute(IN input) throws Process.ProcessException {
        return currentProcess.process(input);
    }
}