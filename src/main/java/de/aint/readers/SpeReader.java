package de.aint.readers;

import de.aint.models.Spectrum;

import java.io.File;
import java.util.Scanner;

public class SpeReader extends Reader{

    @Override
    public Spectrum readSpectrum(String src){
        //Initialize Scanner
        Scanner scan = null;
        try {
            scan = new Scanner(new File(src));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //Skip to %DATA
        String curr = "";
        while(!curr.equals("%DATA:")){
            curr = scan.nextLine();
        }

        //Gather data
        curr = scan.nextLine();
        curr = curr.strip();

        int channel_count = 0;

        try{
            channel_count = Integer.parseInt(curr);
        }catch(NumberFormatException ne){
            throw new RuntimeException(ne);
        }

        var counts = new int[channel_count];

        for(int i = 0; i < channel_count; i++){
            curr = scan.nextLine().strip();
            try {
                int count = Integer.parseInt(curr);
                counts[i] = count;
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

        return new Spectrum(counts);

    }

}
