package THUIM;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to use the THUIM algorithm from the source code.
 *
 * @author Philippe Fournier-Viger, 2010
 *
 */
public class MainTestTMPHP {

    public static void main(String[] arg) throws IOException {

        // String input = fileToPath("DB_Utility.txt");
        String input = "chess.txt";
        String output = ".//PTHUIM_output.txt";
        Integer[] tarHUISubsume = {7, 52};
        int min_utility = 450000;

        int minPeriodicity = 1;  // minimum periodicity parameter (a number of transactions)
        int maxPeriodicity = 3000;  // maximum periodicity parameter (a number of transactions)
        int minAveragePeriodicity = 1;  // minimum average periodicity (a number of transactions)
        int maxAveragePeriodicity = 1000;  // maximum average periodicity (a number of transactions)


        // Applying the THUIM algorithm
        PTHUIMAlgo thuim = new PTHUIMAlgo();
        thuim.THUIM(input, output, min_utility, tarHUISubsume,
                minPeriodicity, maxPeriodicity, minAveragePeriodicity,
                maxAveragePeriodicity);
        thuim.printStats(input, min_utility, tarHUISubsume);

    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestTMPHP.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
