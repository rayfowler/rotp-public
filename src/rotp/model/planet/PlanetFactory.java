/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.planet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import rotp.model.empires.Race;
import rotp.model.galaxy.StarSystem;
import rotp.util.Base;

public class PlanetFactory implements Base {
    private static final PlanetFactory instance = new PlanetFactory();
    public static PlanetFactory current() { return instance; }
    private static final String planetListFile = "data/planets/listing.txt";
    private static final String planetDataDir  = "data/planets/";

    static { instance.loadDataFiles(); }

    public static Planet createPlanet(StarSystem sys, float bonus) {
        Planet p = instance.options().randomPlanet(sys);
        
        if (p.type().isAsteroids()) {
            p.baseSize(0);
            return p;
        }
        float size = p.type().randomSize();
        if (size == p.type().maxSize()) {
            while (p.random() < .2f)
                size += 5.0f;
        }
        else if (size == p.type().minSize()) {
            while (p.random() < .2f)
                size -= 5.0f;
        }
        
        // IMPORTANT: bound the size between 10 and 120
        size = Math.max(10, Math.min(size, 120));
       
        p.baseSize(size);
        return p;
    }
    public static Planet createOrion(StarSystem sys, float bonus) {
        Planet p = instance.options().orionPlanet(sys);
        p.setOrionArtifact();
        p.makeEnvironmentFertile();
        p.baseSize(120*bonus);
        return p;
    }
    public static Planet createHomeworld(Race r, StarSystem sys, float bonus) {
        Planet p = instance.options().randomPlayerPlanet(r, sys);
        p.baseSize(r.homeworldSize*bonus);
        if (r.homeworldKey() > 0)
            p.terrainSeed(r.homeworldKey());
        return p;
    }
    private void loadDataFiles() {
        log("Loading Planet Types...");
        BufferedReader in = reader(planetListFile);
        if (in == null)
            return;

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadPlanetDataFile(input);
            in.close();
        }
        catch (IOException e) {
            err("PlanetFactory.loadDataFiles -- IOException: " + e);
        }
    }
    private void loadPlanetDataFile(String line) {
        if (isComment(line))
            return;

        BufferedReader in = reader(planetDataDir+line.trim());
        if (in == null)
            return;

        try {
            PlanetType newType = new PlanetType();
            String input;
            while ((input = in.readLine()) != null)
                loadPlanetDataLine(newType, input);
            in.close();
            PlanetType.addType(newType);
        }
        catch (IOException e) {
            err("PlanetFactory.loadPlanetDataFile(", line, ") -- IOException: ", e.toString());
        }
    }
    private void loadPlanetDataLine(PlanetType type, String input) {
        if (isComment(input))
            return;

        List<String> keyVal = substrings(input, ':');
        if (keyVal.size() < 2)
            return;

        String key = keyVal.get(0);
        String val = keyVal.get(1);

        switch(key) {
            case "key"            : type.key(val); return;
            case "descBiological" : type.descBiological(val); return;
            case "descSilicoid"   : type.descSilicoid(val); return;
            case "hostility"      : type.hostility(parseInt(val)); return;
            case "terrainImage"   : type.terrainKey(val); return;
            case "panoramaImage"  : type.panoramaKey(val); return;
            case "landscapeImage" : type.landscapeKeys(val); return;
            case "atmoImage"      : type.atmosphereKeys(val); return;
            case "cloudImage"     : type.cloudKeys(val); return;
            case "minSize"        : type.minSize(parseInt(val)); return;
            case "maxSize"        : type.maxSize(parseInt(val)); return;
            case "color"          : parseColorEntry(type, substrings(val, ',')); return;
            case "ship0"          : parseShipValues(type, 0, substrings(val, ',')); return;
            case "ship1"          : parseShipValues(type, 1, substrings(val, ',')); return;
            case "ship2"          : parseShipValues(type, 2, substrings(val, ',')); return;
            case "ship3"          : parseShipValues(type, 3, substrings(val, ',')); return;
        }
        err("unknown key->", input);
    }
    private void parseShipValues(PlanetType pt, int shipNum, List<String> vals) {
        pt.shipX(shipNum, parseInt(vals.get(0)));
        pt.shipY(shipNum, parseInt(vals.get(1)));
        pt.shipW(shipNum, parseInt(vals.get(2)));
    }
    private void parseColorEntry(PlanetType pt, List<String> vals) {
        if (vals.size() < 4)
            return;
        float pct = parseFloat(vals.get(0));
        int r = parseInt(vals.get(1));
        int g = parseInt(vals.get(2));
        int b = parseInt(vals.get(3));
        pt.colorMap().addColorMark(pct, r, g, b);
    }
}
