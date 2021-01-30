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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rotp.model.empires.Empire;
import rotp.ui.util.planets.Sphere2D;
import rotp.util.Base;
import rotp.util.ColorMap;

public class PlanetType implements Base {
    private static final int MAX_SHIPS = 4;
    public static final int TERRAIN_MAX = 10000000; // number of possible terrains

    private static final HashMap<String, PlanetType> typeMap = new HashMap<>();
    public static PlanetType keyed(String s)       { return typeMap.get(s); }
    public static Collection<PlanetType> allTypes(){ return typeMap.values(); }
    public static void addType(PlanetType r)       { typeMap.put(r.key(), r); }

    public static final String NONE = "PLANET_NONE";
    public static final String RADIATED = "PLANET_RADIATED";
    public static final String TOXIC = "PLANET_TOXIC";
    public static final String INFERNO = "PLANET_INFERNO";
    public static final String DEAD = "PLANET_DEAD";
    public static final String TUNDRA = "PLANET_TUNDRA";
    public static final String BARREN = "PLANET_BARREN";
    public static final String MINIMAL = "PLANET_MINIMAL";
    public static final String DESERT = "PLANET_DESERT";
    public static final String STEPPE = "PLANET_STEPPE";
    public static final String ARID = "PLANET_ARID";
    public static final String OCEAN = "PLANET_OCEAN";
    public static final String JUNGLE = "PLANET_JUNGLE";
    public static final String TERRAN = "PLANET_TERRAN";

    public static final int HOSTILITY_NONE = 99;
    public static final int HOSTILITY_RADIATED = 12;
    public static final int HOSTILITY_TOXIC = 11;
    public static final int HOSTILITY_INFERNO = 10;
    public static final int HOSTILITY_DEAD = 9;
    public static final int HOSTILITY_TUNDRA = 8;
    public static final int HOSTILITY_BARREN = 7;
    public static final int HOSTILITY_MINIMAL = 6;
    public static final int HOSTILITY_DESERT = 5;
    public static final int HOSTILITY_STEPPE = 4;
    public static final int HOSTILITY_ARID = 3;
    public static final int HOSTILITY_OCEAN = 2;
    public static final int HOSTILITY_JUNGLE = 1;
    public static final int HOSTILITY_TERRAN = 0;

    private String key;
    private String descBiological;
    private String descSilicoid;
    private String terrainKey;
    private String panoramaKey;
    private final List<Integer> terrainSeeds = new ArrayList<>();
    private final int[] shipX = new int[MAX_SHIPS];
    private final int[] shipY = new int[MAX_SHIPS];
    private final int[] shipW = new int[MAX_SHIPS];

    private final List<String> landscapeKeys  = new ArrayList<>();
    private final List<String> cloudKeys      = new ArrayList<>();
    private final List<String> atmosphereKeys = new ArrayList<>();
    private int hostility;
    private int minSize;
    private int maxSize;

    private transient ColorMap colorMap;
    private transient BufferedImage terrainImage;
    private transient BufferedImage panoramaImage;
    private transient Map<Integer, Sphere2D>smallSpheres;
    private transient Map<Integer, Integer> sphereResolution;

    private  Map<Integer,Sphere2D> smallSpheres() {
        if (smallSpheres == null)
            smallSpheres = new HashMap<>();
        return smallSpheres;
    }
    private  Map<Integer,Integer> sphereResolution() {
        if (sphereResolution == null)
            sphereResolution = new HashMap<>();
        return sphereResolution;
    }
    public Sphere2D smallSphere(Planet p)               { return smallSpheres().get(p.terrainSeed());  }
    public void smallSphere(Sphere2D sph, Planet p)     { smallSpheres().put(p.terrainSeed(), sph); }
    public int sphereResolution(Planet p)         {
        if (PlanetType.this.sphereResolution().containsKey(p.terrainSeed()))
            return PlanetType.this.sphereResolution().get(p.terrainSeed());
        else
            return 0;
    }

    public PlanetType() {
        terrainSeeds.add(roll(1,TERRAIN_MAX-1));
    }
    @Override
    public String toString()                  { return concat("PlanetType: ", key); }

    public String key()                       { return key; }
    public void key(String s)                 { key = s; }
    public String descBiological()               { return descBiological; }
    public void descBiological(String s)         { descBiological = s; }
    public String descSilicoid()               { return descSilicoid; }
    public void descSilicoid(String s)         { descSilicoid = s; }
    public int hostility()                    { return hostility; }
    public void hostility(int i)              { hostility = i; }
    public String terrainKey()                { return terrainKey; }
    public void terrainKey(String s)          { terrainKey = s; }
    public String panoramaKey()               { return panoramaKey; }
    public void panoramaKey(String s)         { panoramaKey = s; }
    public int minSize()                      { return minSize; }
    public void minSize(int i)                { minSize = i; }
    public int maxSize()                      { return maxSize; }
    public void maxSize(int i)                { maxSize = i; }
    public void landscapeKeys(String s)       { landscapeKeys.addAll(substrings(s, ',')); }
    public List<String> atmosphereKeys()      { return atmosphereKeys; }
    public List<String> cloudKeys()           { return cloudKeys; }

    public int shipX(int i)                   { return shipX[i]; }
    public void shipX(int i, int val)         { shipX[i] = val; }
    public int shipY(int i)                   { return shipY[i]; }
    public void shipY(int i, int val)         { shipY[i] = val; }
    public int shipW(int i)                   { return shipW[i]; }
    public void shipW(int i, int val)         { shipW[i] = val; }

    public void cloudKeys(String s) {
        cloudKeys.clear();
        if (!s.trim().isEmpty())
            cloudKeys.addAll(substrings(s, ','));
    }
    public void atmosphereKeys(String s) {
        atmosphereKeys.clear();
        if (!s.trim().isEmpty())
            atmosphereKeys.addAll(substrings(s, ','));
    }

    public String name()                      { return text(key); }
    public boolean hostileToTerrans()         { return hostility >= HOSTILITY_BARREN; }
    public int randomTerrainSeed()            { return random(terrainSeeds); }

    public boolean isAsteroids()              { return key.equals(NONE); }

    public String description(Empire emp) {
        if (emp.ignoresPlanetEnvironment())
            return descSilicoid();
        else
            return descBiological();
    }
    public BufferedImage terrainImage()           {
        if (terrainImage == null)
            terrainImage = newBufferedImage(currentFrame(terrainKey));
        return terrainImage;
    }
    public BufferedImage panoramaImage()           {
        if (panoramaKey == null)
            return null;
        if (panoramaImage == null)
            panoramaImage = newBufferedImage(currentFrame(panoramaKey));
        return panoramaImage;
    }
    public String randomLandscapeKey() {
        return random(landscapeKeys);
    }
    public Image randomCloudImage() {
        return cloudKeys.isEmpty() ? null : image(random(cloudKeys));
    }
    public Image atmosphereImage() {
        return atmosphereKeys.isEmpty() ? null : image(atmosphereKeys.get(0));
    }
    public int randomSize() {
        return 5* roll(minSize()/5, maxSize()/5);
    }
    public ColorMap colorMap()  {
        if (colorMap == null)
            colorMap = new ColorMap();
        return colorMap;
    }
    public float randomOceanPct() {
        switch(key()) {
            case PlanetType.OCEAN:
                return random(0.8f,1.0f);
            case PlanetType.JUNGLE:
                return random(0.6f,0.8f);
            case PlanetType.TERRAN:
                return random(0.55f,0.7f);
            case PlanetType.STEPPE:
                return random(0.25f,0.55f);
            case PlanetType.ARID:
                return random(0.1f,0.25f);
            case PlanetType.DESERT:
                return random(0.05f,0.1f);
            case PlanetType.MINIMAL:
                return 0.05f;
            case PlanetType.TOXIC:
                return random(0.2f,0.6f);
            case PlanetType.BARREN:
            case PlanetType.TUNDRA:
            case PlanetType.DEAD:
            case PlanetType.INFERNO:
            case PlanetType.RADIATED:
            default:
                return 0.0f;
        }
    }
    public int randomIceLevel() {
        switch(key()) {
            case PlanetType.OCEAN:
                return roll(10,30);
            case PlanetType.JUNGLE:
                return 0;
            case PlanetType.TERRAN:
                return roll(10,60);
            case PlanetType.STEPPE:
                return roll(10,80);
            case PlanetType.ARID:
                return roll(10,60);
            case PlanetType.DESERT:
                return roll(10,50);
            case PlanetType.MINIMAL:
                return roll(0,40);
            case PlanetType.BARREN:
                return roll(0,20);
            case PlanetType.TUNDRA:
                return 0;
            case PlanetType.DEAD:
                return roll(30,100);
            case PlanetType.INFERNO:
                return 0;
            case PlanetType.TOXIC:
                return roll(0,30);
            case PlanetType.RADIATED:
            default:
                return  roll(0,20);
        }
    }
    public int randomCloudThickness() {
        // inferno 600+
        // none = 0
        // wisps = 300-350
        // thin 350-400
        // terran 400-450
        // heavy 450-500
        switch(key()) {
            case PlanetType.OCEAN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
            case PlanetType.STEPPE:
                return roll(400,450);
            case PlanetType.ARID:
            case PlanetType.DESERT:
            case PlanetType.TUNDRA:
                return roll(350,400);
            case PlanetType.MINIMAL:
            case PlanetType.BARREN:
                return roll(300,350);
            case PlanetType.INFERNO:
                return 700;
            case PlanetType.TOXIC:
                return roll(300,500);
            case PlanetType.DEAD:
            case PlanetType.RADIATED:
            default:
                return  0;
        }
    }
}
