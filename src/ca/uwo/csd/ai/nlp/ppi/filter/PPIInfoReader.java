
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonatuni
 */
public class PPIInfoReader {
    
    public List<HashMap<String, String[]>> read(File infoFile) {
        List<HashMap<String, String[]>> maps = new ArrayList<HashMap<String, String[]>>();
        HashMap<String, String[]> map;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(infoFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                map = new HashMap<String, String[]>();
                int n = Integer.parseInt(tokens[1]);
                int pos = 0;
                int neg = 0;
                for (int i = 0; i < n; i++) {
                    line = reader.readLine();
                    tokens = line.split("\\s+");
                    String id = tokens[0];
                    String interaction = tokens[3];
                    if (interaction.equals("1")) {                        
                        pos++;
                        interaction = "+" + pos;
                    } else {
                        neg++;
                        interaction = "-" + neg;
                    }
                    map.put(interaction, new String[]{id, tokens[1], tokens[2]});
                }
                maps.add(map);
            }
        } catch (IOException ex) {
            Logger.getLogger(PPIInfoReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return maps;
    }
}
