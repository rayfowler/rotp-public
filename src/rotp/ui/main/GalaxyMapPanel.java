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
package rotp.ui.main;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Nebula;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathDisplaySprite;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.GridCircularDisplaySprite;
import rotp.ui.sprites.RangeDisplaySprite;
import rotp.ui.sprites.ShipDisplaySprite;
import rotp.ui.sprites.SystemNameDisplaySprite;
import rotp.ui.sprites.ZoomInWidgetSprite;
import rotp.ui.sprites.ZoomOutWidgetSprite;

public class GalaxyMapPanel extends BasePanel implements ActionListener, MouseListener, MouseWheelListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    public static final int SHOW_ALL_FLIGHTPATHS = 0;
    public static final int SHOW_IMPORTANT_FLIGHTPATHS = 1;
    public static final int SHOW_NO_FLIGHTPATHS = 2;

    public static final int SHOW_ALL_SHIPS = 0;
    public static final int SHOW_NO_UNARMED_SHIPS = 1;
    public static final int SHOW_ONLY_ARMED_SHIPS = 2;
    
    public static final int SHOW_RANGES = 0;
    public static final int SHOW_STARS_AND_RANGES = 1;
    public static final int SHOW_STARS = 2;
    public static final int SHOW_NO_STARS_AND_RANGES = 3;

    public static final int MAX_STARGATE_SCALE = 40;
    public static final int MAX_RALLY_SCALE = 100;
    public static final int MAX_FLEET_UNARMED_SCALE = 40;
    public static final int MAX_FLEET_TRANSPORT_SCALE = 60;
    public static final int MAX_FLEET_SMALL_SCALE = 60;
    public static final int MAX_FLEET_LARGE_SCALE = 80;
    public static final int MAX_FLEET_HUGE_SCALE = 100;
    
    private static final Color unreachableBackground = new Color(0,0,0);

    public static Color gridLight = new Color(160,160,160);
    public static Color gridDark = new Color(64,64,64);

    private final IMapHandler parent;

    // static fields shared across all galaxy map panels to keep them 
    // visually in synch
    private static Location center = new Location();
    private static float scaleX, scaleY;
    private static float sizeX, sizeY;
    private static final List<Sprite> baseControls = new ArrayList<>();
    private static boolean showSystemNames = true;
    private static int flightPathDisplay = SHOW_IMPORTANT_FLIGHTPATHS;
    private static int shipDisplay = SHOW_ALL_SHIPS;
    private static int showShipRanges = SHOW_STARS_AND_RANGES;
    private static boolean showGridCircular = false;

    private float desiredScale;
    private Image mapBuffer;
    private Image rangeMapBuffer;
    public static BufferedImage sharedStarBackground;
    private final float zoomBase = 1.1f;
    boolean dragSelecting = false;
    private int selectX0, selectY0, selectX1, selectY1;
    private int lastMouseX, lastMouseY;
    private boolean redrawRangeMap = true;
    private Sprite hoverSprite;

    private final Timer zoomTimer;

    public IMapHandler parent()                 { return parent; }

    public void toggleSystemNameDisplay()       { showSystemNames = !showSystemNames; }
    public void toggleFlightPathDisplay(boolean reverse)       {
        if (reverse) {
            switch(flightPathDisplay) {
                case SHOW_ALL_FLIGHTPATHS:       flightPathDisplay = SHOW_NO_FLIGHTPATHS; break;
                case SHOW_IMPORTANT_FLIGHTPATHS: flightPathDisplay = SHOW_ALL_FLIGHTPATHS; break;
                case SHOW_NO_FLIGHTPATHS:        flightPathDisplay = SHOW_IMPORTANT_FLIGHTPATHS; break;
            }
        }
        else {
            switch(flightPathDisplay) {
                case SHOW_ALL_FLIGHTPATHS:       flightPathDisplay = SHOW_IMPORTANT_FLIGHTPATHS; break;
                case SHOW_IMPORTANT_FLIGHTPATHS: flightPathDisplay = SHOW_NO_FLIGHTPATHS; break;
                case SHOW_NO_FLIGHTPATHS:        flightPathDisplay = SHOW_ALL_FLIGHTPATHS; break;
            }
        }
    }
    public void toggleShipDisplay(boolean reverse)       {
        if (reverse) {
            switch(shipDisplay) {
                case SHOW_ALL_SHIPS:        shipDisplay = SHOW_ONLY_ARMED_SHIPS; break;
                case SHOW_NO_UNARMED_SHIPS: shipDisplay = SHOW_ALL_SHIPS; break;
                case SHOW_ONLY_ARMED_SHIPS: shipDisplay = SHOW_NO_UNARMED_SHIPS; break;
            }
        }
        else {
            switch(shipDisplay) {
                case SHOW_ALL_SHIPS:        shipDisplay = SHOW_NO_UNARMED_SHIPS; break;
                case SHOW_NO_UNARMED_SHIPS: shipDisplay = SHOW_ONLY_ARMED_SHIPS; break;
                case SHOW_ONLY_ARMED_SHIPS: shipDisplay = SHOW_ALL_SHIPS; break;
            }
        }
    }
    public void toggleShipRangesDisplay(boolean reverse)       {
        if (reverse) {
            switch(showShipRanges) {
                case SHOW_RANGES:              showShipRanges = SHOW_NO_STARS_AND_RANGES; break;
                case SHOW_STARS_AND_RANGES:    showShipRanges = SHOW_RANGES; break;
                case SHOW_STARS:               showShipRanges = SHOW_STARS_AND_RANGES; break;
                case SHOW_NO_STARS_AND_RANGES: showShipRanges = SHOW_STARS; break;
            }
        }
        else {
            switch(showShipRanges) {
                case SHOW_RANGES:              showShipRanges = SHOW_STARS_AND_RANGES; break;
                case SHOW_STARS_AND_RANGES:    showShipRanges = SHOW_STARS; break;
                case SHOW_STARS:               showShipRanges = SHOW_NO_STARS_AND_RANGES; break;
                case SHOW_NO_STARS_AND_RANGES: showShipRanges = SHOW_RANGES; break;
            }
        }
    }
    public void toggleGridCircularDisplay()     { showGridCircular = !showGridCircular; }
    public boolean showGridCircular()           { return showGridCircular; }
    public boolean showFleetsOnly()             { return flightPathDisplay == SHOW_NO_FLIGHTPATHS; }
    public boolean showImportantFlightPaths()   { return flightPathDisplay != SHOW_NO_FLIGHTPATHS; }
    public boolean showAllFlightPaths()         { return flightPathDisplay == SHOW_ALL_FLIGHTPATHS; }
    public boolean showFriendlyTransports()     { return shipDisplay != SHOW_ONLY_ARMED_SHIPS; }
    public boolean showUnarmedShips()           { return shipDisplay == SHOW_ALL_SHIPS; }
    public boolean showArmedShips()             { return true; }
    public boolean showSystemNames()            { return showSystemNames; }
    public boolean showShipRanges()             { return (showShipRanges == SHOW_RANGES) || (showShipRanges == SHOW_STARS_AND_RANGES); }
    public boolean showStars()                  { return (showShipRanges == SHOW_STARS)  || (showShipRanges == SHOW_STARS_AND_RANGES); }

    public Location currentFocus()        { return parent.mapFocus();  }
    public void currentFocus(IMappedObject o)  { parent.mapFocus(o); }
    private Location center()              { return center; }
    public void center(Location o)        { center = o; }
    public float mapMinX()                { return center.x()-(scaleX*2/5); }
    public float mapMinY()                { return center.y()-(scaleY*2/5); }
    public float mapMaxX()                { return center.x()+(scaleX*3/5); }
    public float mapMaxY()                { return center.y()+(scaleY*3/5); }
    public void centerX(float x)       { center.x(x); }
    public void centerY(float y)       { center.y(y); }
    public float centerX()                { return center.x(); }
    public float centerY()                { return center.y(); }
    public float sizeX()            { return sizeX; }
    public float sizeY()            { return sizeY; }
    public void sizeX(float s)      { sizeX = s; }
    public void sizeY(float s)      { sizeY = s; }

    public float scaleX()            { return scaleX; }
    public float scaleY()            { return scaleY; }
    public void scaleX(float s)      { scaleX = s; }
    public void scaleY(float s)      { scaleY = s; }

    public boolean displays(IMappedObject obj) {
        if (center() == null)
            return true;

        float x = obj.x();
        float y = obj.y();
        return (x >= (mapMinX()*0.9f)) && (x <= (mapMaxX()*1.1f)) && (y >= (mapMinY()*0.9f)) && (y <= (mapMaxY()*1.1f));
    }
    public GalaxyMapPanel(IMapHandler p) {
        parent = p;
        zoomTimer = new Timer(10, this);
        init();
    }
    private void init() {
        setBackground(Color.BLACK);
        setOpaque(true);

        flightPathDisplay = parent.defaultFleetDisplay();
        shipDisplay = SHOW_ALL_SHIPS;
        showShipRanges = parent.defaultShipRangesDisplay();
        showGridCircular = parent.defaultGridCircularDisplay();

        if (baseControls.isEmpty()) {
            baseControls.add(new ZoomOutWidgetSprite(20,270,30,30));
            baseControls.add(new ZoomInWidgetSprite(20,235,30,30));
            baseControls.add(new RangeDisplaySprite(20,200,30,30));
            baseControls.add(new GridCircularDisplaySprite(20,165,30,30));
            baseControls.add(new FlightPathDisplaySprite(20,130,30,30));
            baseControls.add(new ShipDisplaySprite(20,95,30,30));
            baseControls.add(new SystemNameDisplaySprite(20,60,30,30));
        }
        
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
    }
    // scale(float) will translate any arbitrary "real" distancce
    // into a map distance in pixels
    public int scale(float d) {
        return (int) (d*this.getSize().width/scaleX());
    }
    // objX(int) and objY(int) will translate any arbitrary on-screen
    // map coordinates into "real" coordinates
    public float objX(int x) {
        int w = getSize().width;
        return mapMinX()+(scaleX()*x/w);
    }
    public float objY(int y) {
        int h = getSize().height;
        return mapMinY()+(scaleY()*y/h);
    }
    // mapX(float) and mapY(float) will translate any arbitrary "real"
    // coordinates into map coordinates
    public int mapX(float x) {
        float minX = mapMinX();
        float maxX = mapMaxX();
        int w = getSize().width;

        float rX = (x-minX)/(maxX-minX);
        int res = (int) (rX*w);
        //int res =  (w/2)+(int)(rX*w);
        return res;
    }
    public int mapY(float y) {
        float minY = mapMinY();
        float maxY = mapMaxY();
        int h = getSize().height;
        float rY = (y-minY)/(maxY-minY);
        int res = (int) (rY*h);
        return res;
    }
    public void setBounds(float x1, float x2, float y1, float y2) {
        float scaleFromY = y2-y1;
        float scaleFromX = (x2-x1)*getSize().height/getSize().width;

        setScale(max(scaleFromY, scaleFromX));
        center().x((x1+x2)/2);
        center().y((y1+y2)/2);
    }
    public void setScale(float scale) {
        int mapSizeX = getSize().width;
        int mapSizeY = getSize().height;
        if (scaleY != scale)
            clearRangeMap();
        scaleY(scale);
        scaleX(scale*mapSizeX/mapSizeY);
    }
    public float maxScale() {
        float largestAxis = Math.max(sizeX(), sizeY());
        return galaxy().maxScaleAdj()*largestAxis;
    }
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        parent.checkMapInitialized();
        paintToImage(mapBuffer());
        g.drawImage(mapBuffer,0,0,null);
        parent.paintOverMap(this, g2);
    }
    public void paintToImage(Image img) {
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        super.paintComponent(g2); //paint background

        setScale(scaleY());
        //log("map scale:", fmt(scaleX(),2), "@", fmt(scaleY(),2), "  center:", fmt(center().x(),2), "@", fmt(center().y(),2), "  x-rng:", fmt(mapMinX()), "-", fmt(mapMaxX(),2), "  y-rng:", fmt(mapMinY()), "-", fmt(mapMaxY(),2));
        drawBackground(g2);
        if ((scaleX() < 200) && showStars())
            drawBackgroundStars(g2);
        drawGrids(g2);

        drawNebulas(g2);
        drawStarSystems(g2);
        drawShips(g2);
        drawWorkingFlightPaths(g2);
        parent.drawYear(g2);
        parent.drawTitle(g2);
        parent.drawAlerts(g2);
        drawControlSprites(g2);
        drawNextTurnSprites(g2);
        drawRangeSelect(g2);
        g2.dispose();
    }
    private Image mapBuffer() {
        if ((mapBuffer == null) || (mapBuffer.getWidth(this) != getWidth()) || (mapBuffer.getHeight(this) != getHeight())) 
            generateNewMapBuffers();
        
        return mapBuffer;
    }
    private void generateNewMapBuffers() {
        int w = getWidth();
        int h = getHeight();
        mapBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        rangeMapBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (sharedStarBackground == null) {
            sharedStarBackground = new BufferedImage(RotPUI.instance().getWidth(), RotPUI.instance().getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawBackgroundStars(sharedStarBackground, this);
        }
    }
    public void clearRangeMap() {   redrawRangeMap = true; }
    private void setFocusToCenter() {
        currentFocus(new Location());
        currentFocus().setXY(sizeX()/2, sizeY()/2);
        center(currentFocus());
    }
    public void recenterMapOn(IMappedObject obj) {
        recenterMap(obj.x(), obj.y());
    }
    private void recenterMap(float x, float y) {
        float bestX = bounds(0, x, sizeX());
        float bestY = bounds(0, y, sizeY());

        currentFocus().setXY(bestX, bestY);
        center(parent.mapFocus());
        clearRangeMap();
    }
    public void adjustZoom(int z) {
        int MIN_SCALE = 4;
        if (parent.canChangeMapScales()) {
            float multiplier = pow(zoomBase,z);
            float newScale = scaleY()*multiplier;
            newScale = max(MIN_SCALE, min(2*maxScale(), newScale));
            desiredScale = newScale;
            zoomTimer.start();
        }
    }
    public void initializeMapData() {
        sizeX(galaxy().width());
        sizeY(galaxy().height());
        clearRangeMap();

        setFocusToCenter();

        // try to set starting scale to what parent UI wants
        float largestAxis = max(sizeX(), sizeY());
        float parentStartingScale = parent.startingScalePct()*largestAxis;
        // however, if parent allows us to adjust the scale, then the
        // starting scale must be capped at the maximum adjustable scale
        if (parent.canChangeMapScales()) 
            parentStartingScale = min(parentStartingScale, maxScale());
        
        setScale(parentStartingScale);
    }
    private void drawBackground(Graphics2D g) {
        if (showShipRanges()) {
            if (redrawRangeMap) {
                redrawRangeMap = false;
                Graphics2D g0 =  (Graphics2D) rangeMapBuffer.getGraphics();
                setFontHints(g0);
                g0.setColor(unreachableBackground);
                g0.fillRect(0,0,getWidth(),getHeight());
                drawExtendedRangeDisplay(g0);
                drawOwnershipDisplay(g0);
            }
            g.drawImage(rangeMapBuffer,0,0,null);
        }
    }

    private void drawGrids(Graphics2D g) {
        if (showGridCircular) {
            drawGridCircularDisplayDark(g);
            //drawGridCircularDisplayLight(g);
        }
    }
    private void drawOwnershipDisplay(Graphics2D g) {
        int r0 = scale(1.0f);
        int r1 = scale(0.8f);

        Galaxy gal = galaxy();
        Empire pl = player();
        for (int id=0; id < pl.sv.count(); id++) {
            Empire emp = pl.sv.empire(id);
            StarSystem sys = gal.system(id);
            //Shape sysCircle = new Ellipse2D.Float(mapX(sys.x())-ownerR, mapY(sys.y())-ownerR, 2*ownerR, 2*ownerR);
            if ((emp != null) && parent.showOwnership(sys)) {
                int shape = emp.shape();
                g.setColor(emp.ownershipColor());
                int x = mapX(sys.x());
                int y = mapY(sys.y());
                switch(shape) {
                    case Empire.SHAPE_SQUARE:
                        g.fillRect(x-r1, y-r1, r1+r1, r1+r1); break;
                    case Empire.SHAPE_DIAMOND:
                        Polygon p = new Polygon();
                        p.addPoint(x, y-r0);
                        p.addPoint(x-r0, y);
                        p.addPoint(x, y+r0);
                        p.addPoint(x+r0, y);
                        g.fill(p); break;
                    case Empire.SHAPE_TRIANGLE1:
                        Polygon p1 = new Polygon();
                        p1.addPoint(x, y-r0);
                        p1.addPoint(x-r0, y+r1);
                        p1.addPoint(x+r0, y+r1);
                        g.fill(p1); break;
                    case Empire.SHAPE_TRIANGLE2:
                        Polygon p2 = new Polygon();
                        p2.addPoint(x, y+r0);
                        p2.addPoint(x-r0, y-r1);
                        p2.addPoint(x+r0, y-r1);
                        g.fill(p2);
                        break;
                    case Empire.SHAPE_CIRCLE:
                    default:
                        g.fillOval(x-r0, y-r0, r0+r0, r0+r0); break;
                }
                //g.fill(sysCircle);
            }
        }
    }
    private void drawExtendedRangeDisplay(Graphics2D g) {
        Empire emp = parent.empireBoundaries();
        float shipRange = emp.shipRange();
        int scoutRange = emp.scoutRange();
        if (shipRange > galaxy().width())
            return;

        Empire pl = player();
        Color emptyBackground = Color.black;
        Color normalBorder = emp.shipBorderColor();
        Color extendedBorder = emp.scoutBorderColor();
        Color normalBackground = emp.empireRangeColor();
        // draw extended range
        List<StarSystem> systems = player().systemsForCiv(emp);
        List<StarSystem> alliedSystems = new ArrayList<>();
        // only show range for allied systems when player is selected
        if (emp.isPlayer()) {
            for (Empire ally: emp.allies())
                for (StarSystem sys: ally.allColonizedSystems()) {
                    if (pl.sv.empId(sys.id) == ally.id)
                        alliedSystems.add(sys);
                }
        }

        float scale = getWidth()/scaleX();

        int extR = (int) (scoutRange*scale);
        int baseR = (int) (shipRange*scale);
        Area clusterArea = new Area();
                       
        for (StarSystem sv: alliedSystems)
            clusterArea.add(new Area( new Ellipse2D.Float(mapX(sv.x())-extR, mapY(sv.y())-extR, 2*extR, 2*extR) ));       
        
        for (StarSystem sv: systems)
            clusterArea.add(new Area( new Ellipse2D.Float(mapX(sv.x())-extR, mapY(sv.y())-extR, 2*extR, 2*extR) ));       
           
        g.setColor(extendedBorder);
        g.setStroke(new BasicStroke(4));
        g.draw(clusterArea);   
        g.setColor(emptyBackground);
        g.fill(clusterArea);
        clusterArea.reset();
        
        
        for (StarSystem sv: alliedSystems)
            clusterArea.add(new Area( new Ellipse2D.Float(mapX(sv.x())-baseR, mapY(sv.y())-baseR, 2*baseR, 2*baseR) ));       
        
        for (StarSystem sv: systems)
            clusterArea.add(new Area( new Ellipse2D.Float(mapX(sv.x())-baseR, mapY(sv.y())-baseR, 2*baseR, 2*baseR) ));       
                   
        g.setColor(normalBorder);
        g.setStroke(new BasicStroke(4));
        g.draw(clusterArea);   
        g.setColor(normalBackground);
        g.fill(clusterArea);

    }
    private void drawGridCircularDisplayDark(Graphics2D g) {
        Galaxy gal = galaxy();
        Sprite clicked = parent.clickedSprite();
        if (clicked == null)
            return;

        float x = clicked.source().x();
        float y = clicked.source().y();

        Stroke prevStroke = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(gridDark);

        Empire pl = player();
        int rng1 = pl.shipRange();
        int rng2 = pl.scoutRange();
        
        for (int r=1;r<=gal.width();r++) {
            int x0 = mapX(x-r);
            int y0 = mapY(y-r);
            int diam = Math.abs(x0-mapX(x+r));
            if ((clicked instanceof StarSystem) && ((r == rng1) || (r == rng2))) {
                g.setColor(gridLight);
                g.drawOval(x0, y0, diam, diam);
                g.setColor(gridDark);
            }
            else
                g.drawOval(x0, y0, diam, diam);
        }
        g.setStroke(prevStroke);
    }
    private void drawBackgroundStars(Graphics2D g) {
        g.drawImage(sharedStarBackground, 0, 0, null);
    }
    public void drawNebulas(Graphics2D g) {
        for (Nebula neb: galaxy().nebulas()) {
            if (parent.shouldDrawSprite(neb))
                neb.draw(this, g);
        }
    }
    public void drawStarSystems(Graphics2D g) {
        Galaxy gal = galaxy();
        for (int id=0; id< gal.numStarSystems();id++) {
            StarSystem sys = gal.system(id);
            if (parent.shouldDrawSprite(sys))
                sys.draw(this, g);
            if (parent.shouldDrawSprite(sys.transportSprite))
                sys.transportSprite().draw(this, g);
            if (parent.shouldDrawSprite(sys.rallySprite()))
                sys.rallySprite().draw(this, g);
        }
    }
    public void drawShips(Graphics2D g) {
        Empire pl = player();
        // comodification exception here without this copy
        List<Ship> visibleShips = new ArrayList<>(pl.visibleShips());
        for (Ship sh: visibleShips) {
            sh.setDisplayed(this);
            if (sh.displayed()) {
                Sprite spr = (Sprite) sh;
                // if we are drawing the ship, then check if its flight path should be drawn first
                if (pl.knowETA(sh) && (sh.deployed() || sh.retreating() || sh.inTransit() || sh.isRallied())
                && parent.shouldDrawSprite(sh.pathSprite())) {
                    sh.pathSprite().draw(this,g);
                }
                spr.draw(this, g);
            }
        }
    }
    public void drawWorkingFlightPaths(Graphics2D g) {
        for (FlightPathSprite spr: FlightPathSprite.workingPaths()) {
            if (parent.shouldDrawSprite(spr)) 
                spr.draw(this, g);
        }
    }
    public void drawControlSprites(Graphics2D g) {
        for (Sprite sprite: baseControls) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    public void drawNextTurnSprites(Graphics2D g) {
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    public void drawRangeSelect(Graphics2D g) {
        if ((selectX0 == selectX1) || (selectY0 == selectY1))
            return;
        
        int x = min(selectX0, selectX1);
        int y = min(selectY0, selectY1);
        int w = abs(selectX0-selectX1);
        int h = abs(selectY0-selectY1);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(Color.white);
        g.drawRect(x,y,w,h);
        
        g.setStroke(prev);
    }
    private Sprite spriteAt(int x1, int y1) {
        // In order of hovering priority:
        // 1. Next Turn Sprites
        // 2. Map Control Sprites
        // 3. Ships & Flight Paths
        // 4. Systems
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: baseControls) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        // is there any overlay covering the map and preventing a search
        if (parent.masksMouseOver(x1,y1))
            return null;
        Galaxy gal = galaxy();
        Empire pl = player();
        List<Ship> ships = null;
        if (scaleX() <= MAX_FLEET_HUGE_SCALE) {
            if (parent.hoverOverFleets()) {
                ships = new ArrayList<>(pl.visibleShips());
                float minDistance = Float.MAX_VALUE;
                Sprite closestShip = null;
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (parent.shouldDrawSprite(spr)
                        && spr.isSelectableAt(this, x1, y1)) {
                            float dist = spr.selectDistance(this, x1, y1);
                            if (dist == 0)
                               return spr;
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestShip = spr;
                            }
                        }
                    }
                }
                if (closestShip != null)
                    return closestShip;
            }
        }
        if (parent.hoverOverSystems()) {
            for (int id=0;id<gal.numStarSystems();id++) {
                if (gal.system(id).isSelectableAt(this, x1, y1))
                    return gal.system(id);
            }
        }
        // working flight paths are always displayed and can always be selected
        List<FlightPathSprite> workingPaths = FlightPathSprite.workingPaths();
        for (FlightPathSprite path: workingPaths) {
            if (path.isSelectableAt(this,x1,y1))
                return path;
        }
        if (scaleX() <= MAX_FLEET_HUGE_SCALE) {
            if (parent.hoverOverFlightPaths()) {
                if (ships == null)
                    ships = new ArrayList<>(pl.visibleShips());
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        FlightPathSprite fpSpr = sh.pathSprite();
                        if (parent.shouldDrawSprite(fpSpr)
                        && fpSpr.isSelectableAt(this, x1, y1))
                            return fpSpr;
                    }
                }
            }
        }
        return null;
    }
    public void dragMap(int deltaX, int deltaY) {
        int focusX = mapX(parent.mapFocus().x());
        int focusY = mapY(parent.mapFocus().y());
        recenterMap(objX(focusX-deltaX), objY(focusY-deltaY));
        clearRangeMap();
        parent.repaint();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        float currentScale = scaleY();
        float zoomAdj = desiredScale/currentScale;
        if (zoomAdj == 1.0) {
            zoomTimer.stop();
            return;
        }

        float zoomAmt = zoomBase;
        if (zoomAdj > zoomAmt)
            zoomAdj = zoomAmt;
        else if (zoomAdj < (1.0f/zoomAmt))
            zoomAdj = 1.0f/zoomAmt;

        setScale(currentScale*zoomAdj);
        parent.repaint();
    }
    @Override
    public void animate() {
        if (session().performingTurn() && parent.suspendAnimationsDuringNextTurn())
            return;
        if (zoomTimer.isRunning())
            return;

        if (playAnimations() && (animationCount() % 5 == 0)) 
            parent.repaint();	
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (parent.forwardMouseEvents())
            parent.mouseWheelMoved(e);
        else
            adjustZoom(e.getWheelRotation());
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        int deltaX = x - lastMouseX;
        int deltaY = y - lastMouseY;

        if (rightClick) {
            int prevSelectX0 = selectX0;
            int prevSelectY0 = selectY0;
            int prevSelectX1 = selectX1;
            int prevSelectY1 = selectY1;
            int boundsX0 = min(prevSelectX0, prevSelectX1, x);
            int boundsY0 = min(prevSelectY0, prevSelectY1, y);
            int boundsX1 = max(prevSelectX0, prevSelectX1, x);
            int boundsY1 = max(prevSelectY0, prevSelectY1, y);
            if (x > selectX0) {
                selectX1 = x;
            }
            else if (x < selectX0) {
                selectX0 = x; selectX1 = prevSelectX1; 
            }
            if (y > selectY0) {
                selectY1 = y;
            }
            else if (y < selectY0) {
                selectY0 = y; selectY1 = prevSelectY1; 
            }
            paintImmediately(boundsX0, boundsY0, s5+boundsX1-boundsX0, s5+boundsY1-boundsY0);
        }
        else if (parent.canChangeMapScales()) {
            // find the xy map coords of current focus, adjust by the
            // dragged deltas, then recenter the map on the new focus
            dragMap(deltaX, deltaY);
            lastMouseX = x;
            lastMouseY = y;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        int x = e.getX();
        int y = e.getY();

        Sprite prevHover = hoverSprite;
        hoverSprite = spriteAt(x,y);
        if (hoverSprite != prevHover)
            parent.hoveringOverSprite(hoverSprite);
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent arg0) {
        setCrosshairsCursor();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        if (parent.hoveringSprite() != null)
            parent.hoveringSprite().mouseExit(this);
        parent.hoveringOverSprite(null);
        setDefaultCursor();
    }
    @Override
    public void mousePressed(MouseEvent e) {
        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        if (rightClick) {
            dragSelecting = true;
            selectX0 = selectX1 = e.getX();
            selectY0 = selectY1 = e.getY();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragSelecting) {
            if ((selectX0 != selectX1) && (selectY0 != selectY1)) 
                parent.dragSelect(selectX0, selectY0, selectX1, selectY1);
            selectX0 = selectX1 = selectY0 = selectY1 = 0;
            dragSelecting = false;
            repaint();
            return;               
        }       

        if (e.getButton() > 3)
            return;
        int cnt = e.getClickCount();
        if (cnt > 1)
            return;
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        int x1 = e.getX();
        int y1 = e.getY();
        Sprite newSelection = spriteAt(x1,y1);
        
        if (newSelection == null) 
            parent.clickingNull(cnt, rightClick);
        else 
            parent.clickingOnSprite(newSelection, cnt, rightClick, true);

        parent.hoveringOverSprite(newSelection);
    }
}
