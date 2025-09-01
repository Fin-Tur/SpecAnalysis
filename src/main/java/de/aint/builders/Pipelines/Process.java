package de.aint.builders.Pipelines;

public interface Process<I, O> {
    public static class ProcessException extends RuntimeException {
        public ProcessException(Throwable t) {
            super(t);
        }
    }
    public O process(I input) throws ProcessException;
}