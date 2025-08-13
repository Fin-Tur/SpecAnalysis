package de.aint.readers;

import de.aint.models.Spectrum;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

abstract public class Reader {


    public abstract Spectrum readSpectrum(String src) throws IOException;

    public static Spectrum readFile(String src){
        File f = new File(src);
        if(f.getName().endsWith(".Spe")){
            SpeReader speReader = new SpeReader();
            try{
                return speReader.readSpectrum(src);
            } catch(IOException ioE){
                return null;
            }
        }else if(f.getName().endsWith(".mcnp")){
            //Gather user input (src-mult)
            System.out.println("Reading MCNP File. Please enter base force: ");
            Scanner scanner = new Scanner(System.in);
            float cntMult = scanner.nextFloat();
            McnpReader mncpReader = new McnpReader();
            try{
                Spectrum spec = mncpReader.readSpectrum(src);
                spec.setSrcForce(cntMult);
                return spec;
            } catch(IOException ioE){
                return null;
            }


        }
        return null;
    }


}
