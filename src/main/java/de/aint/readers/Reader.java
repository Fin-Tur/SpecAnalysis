package de.aint.readers;

import de.aint.models.Spectrum;

import java.io.IOException;

abstract public class Reader {
    public abstract Spectrum readSpectrum(String src) throws IOException;


}
