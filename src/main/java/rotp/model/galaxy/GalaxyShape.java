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
package rotp.model.galaxy;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.game.IGameOptions;
import rotp.util.Base;

public abstract class GalaxyShape implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    static final int GALAXY_EDGE_BUFFER = 12;
    static final float ORION_BUFFER = 8;
    static final float EMPIRE_BUFFER = 6;
    static final float SYSTEM_BUFFER = 2;
    float[] x;
    float[] y;
    ShapeRegion[][] regions;
    int regionScale = 16;
    int width = 0;
    int height = 0;
    int maxStars = 0;
    int num = 0;
    int homeStars = 0;
    int genAttempt = 0;
    boolean usingRegions = false;
    boolean fullyInit = false;
    List<EmpireSystem> empSystems = new ArrayList<>();
    public Point.Float orionXY;
    IGameOptions opts;

    public int width()          { return width; }
    public int height()         { return height; }
    public boolean fullyInit()  { return fullyInit; }
    protected abstract int galaxyWidthLY();
    protected abstract int galaxyHeightLY();
    public abstract void setRandom(Point.Float p);
    public abstract boolean valid(Point.Float p);
    protected abstract float sizeFactor(String size);

    public float maxScaleAdj()               { return 1.0f; }
    public void coords(int n, Point.Float pt) {
        int i = n;
        if (usingRegions) {
            for (int a=0;a<regionScale;a++) {
                for (int b=0;b<regionScale;b++) {
                    if (i >= regions[a][b].num)
                        i -= regions[a][b].num;
                    else {
                        pt.x = regions[a][b].x[i];
                        pt.y = regions[a][b].y[i];
                        //log("system: "+n+"  is a:"+a+" b:"+b+"  i:"+i);
                        return;
                    }
                }
            }
            throw new RuntimeException("Invalid x index requested: "+i);
        }
        else {
            pt.x = x[i];
            pt.y = y[i];
        }
    }
    public int numberStarSystems()            { return num; }
    public int totalStarSystems()             { return num+homeStars;}
    public List<EmpireSystem> empireSystems() { return empSystems; }
    public int empireSystemStars()            { return homeStars; }
    public float adjustedSizeFactor()        { return sizeFactor(opts.selectedGalaxySize()) + (genAttempt/3); }

    public void fullInit() {
        fullyInit = true;
        init(opts.numberStarSystems());
    }
    public void quickInit() {
        fullyInit = false;
        init(min(5000,opts.numberStarSystems()));
    }
    public void init(int numStars) {
        num = 0;
        homeStars = 0;
        empSystems.clear();
        maxStars = numStars;
        width = galaxyWidthLY() + (2 * galaxyEdgeBuffer());
        height = galaxyHeightLY() + (2 * galaxyEdgeBuffer());
        float minSize = min(width, height);
        usingRegions = minSize > 100;
        if (usingRegions) {
            regionScale = min(64, (int) (minSize / 6.0));
            regions = new ShapeRegion[regionScale][regionScale];
            int regionStars = (int) (2.5*maxStars/regionScale);
            for (int i=0;i<regionScale;i++) {
                for (int j=0;j<regionScale;j++)
                    regions[i][j] = new ShapeRegion(regionStars);
            }
        }
        else {
            x = new float[maxStars];
            y = new float[maxStars];
        }
    }
    public void fullGenerate() {
        generate(true);
    }
    public void quickGenerate() {
        generate(false);
    }
    public void generate(boolean full) {
        log("Galaxy shape: "+maxStars+ " stars"+ "  regionScale: "+regionScale);
        long tm0 = System.currentTimeMillis();
        int numOpps = opts.selectedNumberOpponents()+1;
        genAttempt = 0;
        empSystems.clear();

        // add systems needed for empires
        while (empSystems.size() < numOpps) {
            if (full)
                fullInit();
            else
                quickInit();
            genAttempt++;
            empSystems.clear();
            homeStars = 0;
            num = 0;
            orionXY = addOrion();
            for (int i=0;i<numOpps;i++) {
                EmpireSystem sys = new EmpireSystem(this);
                if (sys.valid) {
                    empSystems.add(sys);
                    homeStars += sys.numSystems();
                }
            }
        }

        // add other systems to fill out galaxy
        int attempts = addUncolonizedSystems();
        long tm1 = System.currentTimeMillis();
        log("Galaxy generation: "+(tm1-tm0)+"ms  Regions: " + usingRegions+"  Attempts: ", str(attempts), "  stars:", str(num), "/", str(maxStars));
    }
    protected int galaxyEdgeBuffer() {
        switch(opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 1;
            case IGameOptions.SIZE_SMALL:     return 1;
            case IGameOptions.SIZE_SMALL2:    return 1;
            case IGameOptions.SIZE_MEDIUM:    return 2;
            case IGameOptions.SIZE_MEDIUM2:   return 2;
            case IGameOptions.SIZE_LARGE:     return 2;
            case IGameOptions.SIZE_LARGE2:    return 2;
            case IGameOptions.SIZE_HUGE:      return 3;
            case IGameOptions.SIZE_HUGE2:     return 3;
            case IGameOptions.SIZE_MASSIVE:   return 3;
            case IGameOptions.SIZE_MASSIVE2:  return 3;
            case IGameOptions.SIZE_MASSIVE3:  return 3;
            case IGameOptions.SIZE_MASSIVE4:  return 4;
            case IGameOptions.SIZE_MASSIVE5:  return 4;
            case IGameOptions.SIZE_INSANE:    return 5;
            case IGameOptions.SIZE_LUDICROUS: return 8;
        }
        return GALAXY_EDGE_BUFFER;
    }
    protected void addColonizedSystems() {

    }
    protected Point.Float addOrion() {
        Point.Float pt = new Point.Float();
        findAnyValidLocation(pt);
        addSystem(pt);
        return pt;
    }
    protected int addUncolonizedSystems() {
        int maxAttempts = maxStars * 10;
        int attempts = 0;
        Point.Float pt = new Point.Float();
        while ((num < maxStars) && (attempts++ < maxAttempts)) {
            findAnyValidLocation(pt);
            if (!isTooNearExistingSystem(pt.x,pt.y,false))
                addSystem(pt);
        }
        return attempts;
    }
    public Point.Float findAnyValidLocation(Point.Float p) {
        setRandom(p);
        while (!valid(p)) 
            setRandom(p);
        
        return p;
    }
    private void addSystem(Point.Float pt) {
        addSystem(pt.x, pt.y);
    }
    private void addSystem(float x0, float y0) {
        if (usingRegions) {
            int xRgn = (int) (regionScale*x0/width);
            int yRgn = (int) (regionScale*y0/height);
            regions[xRgn][yRgn].addSystem(x0,y0);
            num++;
        }
        else {
            x[num] = x0;
            y[num] = y0;
            num++;
        }
    }
    protected boolean isTooNearExistingSystem(float x0, float y0, boolean isHomeworld) {
        if (isHomeworld) {
            if (distance(x0,y0,orionXY.x,orionXY.y) <= ORION_BUFFER)
                return true;
            for (EmpireSystem emp: empSystems) {
                if (distance(x0,y0,emp.colonyX(),emp.colonyY()) <= EMPIRE_BUFFER)
                    return true;
            }
        }
        // not too close to other systems in galaxy
        if (usingRegions) {
            if (isTooNearSystemsInNeighboringRegions(x0,y0))
                return true;
        }
        else {
            if (isTooNearSystemsInEntireGalaxy(x0,y0))
                return true;
        }
        // not too close to other systems in any empire system
        for (EmpireSystem emp: empSystems) {
            for (int i=0;i<emp.num;i++) {
                if (distance(x0,y0,emp.x(i),emp.y(i)) <= SYSTEM_BUFFER)
                    return true;
            }
        }
        return false;
    }
    private boolean isTooNearSystemsInNeighboringRegions(float x0, float y0) {
        int xRgn = (int)(x0*regionScale/width);
        int yRgn = (int)(y0*regionScale/height);
        int yMin = max(0,yRgn-1);
        int yMax = min(regionScale-1,yRgn+1);
        int xMin = max(0,xRgn-1);
        int xMax = min(regionScale-1,xRgn+1);

        for (int x1=xMin;x1<=xMax;x1++) {
            for (int y1=yMin;y1<=yMax;y1++) {
                if (regions[x1][y1].isTooNearSystems(x0,y0))
                    return true;
            }
        }
        return false;
    }
    private boolean isTooNearSystemsInEntireGalaxy(float x0, float y0) {
        for (int i=0;i<num;i++) {
            if (distance(x0,y0,x[i],y[i]) <= SYSTEM_BUFFER)
                return true;
        }
        return false;
    }
    private class ShapeRegion implements Serializable {
        int num = 0;
        float[] x;
        float[] y;
        public ShapeRegion(int maxStars) {
            x = new float[maxStars];
            y = new float[maxStars];
        }
        public boolean isTooNearSystems(float x0, float y0) {
            for (int i=0;i<num;i++) {
                if (distance(x0,y0,x[i],y[i]) <= SYSTEM_BUFFER)
                    return true;
            }
            return false;
        }
        private void addSystem(float x0, float y0) {
            x[num] = x0;
            y[num] = y0;
            num++;
        }
    }
    public final class EmpireSystem implements Serializable {
        float[] x = new float[3];
        float[] y = new float[3];
        int num = 0;
        boolean valid = false;

        public EmpireSystem(GalaxyShape sp) {
            // empire is valid if it can create a valid home system
            // and two valid nearby stars
            valid = addNewHomeSystem(sp);
            valid = valid && addNearbySystem(sp, colonyX(), colonyY(), 3.0f);
            valid = valid && addNearbySystem(sp, colonyX(), colonyY(), 3.0f);
        }
        public int numSystems()   { return num; }
        public float x(int i)    { return x[i]; }
        public float y(int i)    { return y[i]; }
        public float colonyX()   { return x[0]; }
        public float colonyY()   { return y[0]; }

        public boolean inNebula(Nebula neb) {
            for (int i=0;i<num;i++) {
                if (neb.contains(x[i], y[i]))
                    return true;
            }
            return false;
        }

        private boolean addNewHomeSystem(GalaxyShape sp) {
            int attempts = 0;
            Point.Float pt = new Point.Float();
            while (attempts++ < 100) {
                findAnyValidLocation(pt);
                if (!sp.isTooNearExistingSystem(pt.x,pt.y,true)) {
                    addSystem(pt.x,pt.y);
                    return true;
                }
            }
            return false;
        }
        private boolean addNearbySystem(GalaxyShape sh, float x0, float y0, float maxDistance) {
            float x1 = x0-maxDistance;
            float x2 = x0+maxDistance;
            float y1 = y0-maxDistance;
            float y2 = y0+maxDistance;
            int attempts = 0;
            Point.Float pt = new Point.Float();
            while (attempts < 100) {
                attempts++;
                pt.x = random(x1, x2);
                pt.y = random(y1, y2);
                if (sh.valid(pt)) {
                    boolean tooCloseToAny = isTooNearExistingSystem(sh,pt.x,pt.y);
                    boolean tooFarFromRef = distance(x0, y0, pt.x,pt.y) >= maxDistance;
                    if (!tooCloseToAny && !tooFarFromRef) {
                        addSystem(pt.x,pt.y);
                        return true;
                    }
                }
            }
            return false;
        }
        private boolean isTooNearExistingSystem(GalaxyShape sh, float x0, float y0) {
            for (int i=0;i<num;i++) {
                if (distance(x0,y0,x[i],y[i]) <= SYSTEM_BUFFER)
                    return true;
            }
            return sh.isTooNearExistingSystem(x0,y0,false);
        }
        private void addSystem(float x0, float y0) {
            x[num] = x0;
            y[num] = y0;
            num++;
        }
    }
}
