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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.events.StarSystemEvent;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetFactory;
import rotp.model.ships.Design;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipLibrary;
import rotp.model.tech.TechEngineWarp;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.sprites.ShipRelocationSprite;
import rotp.ui.sprites.SystemTransportSprite;
import rotp.util.Base;

public class StarSystem implements Base, Sprite, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Color shield5C = new Color(64,64,64);
    private static final Color shield10C = new Color(24,24,96);
    private static final Color shield15C = new Color(24,96,24);
    private static final Color shield20C = new Color(160,160,48);
    private static final Color selectionC = new Color(160,160,0);
    public static final int NULL_ID = -1;

    private String name = "";
    private float x, y;
    private Planet planet;
    private final String starTypeKey;
    public final int id;

    private boolean isGuarded = false;
    private boolean piracy = false;
    private final List<Transport> orbitingTransports = new ArrayList<>();
    private int[] nearbySystems;
    private String notes;
    private String eventKey;
    private SpaceMonster monster;
    private final List<StarSystemEvent> events = new ArrayList<>();

    // public so we can access without lazy inits from accessors
    public transient SystemTransportSprite transportSprite;
    public transient ShipRelocationSprite rallySprite;
    private transient StarType starType;
    private transient Rectangle nameBox;
    private transient boolean hovering;
    private transient int twinkleCycle, twinkleOffset, drawRadius;
    private transient boolean displayed = false;

    public SystemTransportSprite transportSprite() {
        if (transportSprite == null)
            transportSprite = new SystemTransportSprite(this);
        return transportSprite;
    }
    public ShipRelocationSprite rallySprite() {
        if (rallySprite == null)
            rallySprite = new ShipRelocationSprite(this);
        return rallySprite;
    }
    public int[] nearbySystems() {
        if (nearbySystems == null)
            initNearbySystems();
        return nearbySystems;
    }
    public int numNearbySystems() {
        return nearbySystems().length;
    }
    public StarSystem nearbySystem(int i) {
        if (nearbySystems == null)
            initNearbySystems();
        if ((i<0) || (i >= nearbySystems.length))
            return null;
        else
            return galaxy().system(nearbySystems[i]);
    }
    private void initNearbySystems() {
        TARGET_SYSTEM = this;
        float maxDist = 2 * TechEngineWarp.MAX_SPEED;
        Galaxy gal = galaxy();
        List<StarSystem> nearSystems = new ArrayList<>();
        for (int n=0;n<gal.numStarSystems();n++) {
            StarSystem other = gal.system(n);
            if (distanceTo(other) < maxDist)
                nearSystems.add(other);
        }
        nearSystems.remove(this);
        Collections.sort(nearSystems, DISTANCE_TO_TARGET_SYSTEM);

        int size = min(10,nearSystems.size());
        nearbySystems = new int[size];
        for (int i=0;i<size;i++)
            nearbySystems[i] = nearSystems.get(i).id;
    }
    public static StarSystem create(String key, Galaxy gal) {
        StarSystem s = new StarSystem(key, gal.systemCount);
        return s;
    }
    private StarSystem(String key, int num) {
        starTypeKey = key;
        id = num;
    }
    @Override
    public float x()                       { return x;  }
    @Override
    public float y()                       { return y;  }
    public void setXY(float x0, float y0) {
        x = x0;
        y = y0;
    }
    @Override
    public String toString()                    { return concat("Star System: ", name()); }

    public boolean unnamed()                    { return name().isEmpty(); }
    public SpaceMonster monster()               { return monster; }
    public void monster(SpaceMonster sm)        { monster = sm; }

    public String name()                        { return name; }
    public void name(String s)                  { name = s; }
    public String longName()                    { return concat(name, ":", str(planet().terrainSeed())); }
    public String ruinsKey()                    { return planet().ruinsKey(); }
    public String notes()                       { return notes == null ? "" : notes; }
    public void notes(String s)                 { notes = s.length() <= 40 ? s : s.substring(0,40); }
    public String eventKey()                    { return eventKey == null ? "": eventKey; }
    public void eventKey(String s)              { eventKey = s; }
    public boolean hasEvent()                   { return eventKey != null; }
    public void clearEvent()                    { eventKey = null; }
    public boolean hasMonster()                 { return monster != null; }
    public void addEvent(StarSystemEvent e)     { events.add(e); }
    public List<StarSystemEvent> events()       { return events; }

    public StarType starType()                  {
        if (starType == null)
            starType = StarType.keyed(starTypeKey);
        return starType;
    }
    public boolean piracy()                     { return piracy; }
    public void piracy(boolean b)               { piracy = b; }

    public List<Transport> transports()         { return orbitingTransports; }
    public int orbitingTransports(int empId) {
        for (Transport tr: orbitingTransports) {
            if (tr.empId() == empId)
                return tr.size();
        }
        return 0;
    }
    public List<ShipFleet> orbitingFleets()     { return galaxy().ships.orbitingFleets(id); }
    public List<ShipFleet> exitingFleets()      { return galaxy().ships.deployedFleets(id); }

    public boolean isColonized()                { return planet().isColonized(); }
    public Colony becomeColonized(String n, Empire e) {
        if (unnamed())
            name = n;
        return planet().becomeColonized(e);
    }
    public float population()   { return isColonized() ? colony().population() : 0.0f; }
    public Planet planet() {
        if (planet == null)
            planet = PlanetFactory.createPlanet(this, session().populationBonus());
        return planet;
    }
    public void planet(Planet p)                { planet = p; }
    public Colony colony()                      { return planet().colony(); };
    public boolean hasBonusTechs()              { return planet().hasBonusTechs(); }
    public Color color()                        { return starType().color(); }

    public boolean inNebula()                   { return galaxy().inNebula(x(), y()); }
    public boolean canStargateTravelTo(StarSystem s) {
        return isColonized() && s.isColonized() && (s.empire() == empire()) && colony().hasStargate() && s.colony().hasStargate();
    }
    public float transportTimeTo(StarSystem s) { 
        if (canStargateTravelTo(s))
            return 1.0f;
        else
            return distanceTo(s) / empire().transportSpeed(this, s); 
    }
    public float rallyTimeTo(StarSystem s) {
        Design d = colony().shipyard().design();
        if (!colony().isBuildingShip())
            return -1;

        if (colony().hasStargate() && s.colony().hasStargate())
            return 0;

        ShipDesign sd = (ShipDesign) d;
        return travelTime(this, s, sd.engine().warp());
    }

    public Empire empire()                      { return planet().empire(); }
    public boolean hasColonyForEmpire(Empire c) { return empire() == c; }
    public boolean hasOrbitingTransports()      { return !orbitingTransports.isEmpty(); }
    public boolean hasStargate(Empire e)        { return isColonized() && colony().hasStargate(e); }

    public boolean hasOrbitingFleetForEmpire(Empire emp) {
        return galaxy().ships.orbitingFleet(emp.id, id) != null;
    }
    public void launchTransports() {
        if (planet().isColonized()) {
            colony().launchTransports();
        }
    }
    public boolean hasFleetForEmpire(Empire emp) {
        return galaxy().ships.anyFleetAtSystem(emp.id, id) != null;
    }
    public ShipFleet orbitingFleetForEmpire(Empire emp) {
        return emp == null ? null : galaxy().ships.orbitingFleet(emp.id, id);
    }
    public ShipFleet retreatingFleetForEmpire(Empire emp, StarSystem s) {
        return galaxy().ships.retreatingFleet(emp.id, id, s.id);
    }
    public void resolveAnyShipConflict() {
        if (orbitingShipsInConflict())
            galaxy().shipCombat().battle(this);
    }
    public void resolvePendingTransports() {
        if (!hasOrbitingTransports())
            return;

        // land orbiting transports in random order
        while (!orbitingTransports.isEmpty()) {
            Transport tr = random(orbitingTransports);
            orbitingTransports.remove(tr);
            tr.land();
        }
    }
    public boolean enemyShipsInOrbit(Empire emp) {
        List<ShipFleet> fleets = orbitingFleets();
        for (ShipFleet fleet1: fleets) {
            if (emp.aggressiveWith(fleet1.empire(), this))
                return true;
        }
        return false;
    }
    public boolean orbitingShipsInConflict() {
        List<ShipFleet> fleets = orbitingFleets();
        if (hasMonster() && !fleets.isEmpty())
            return true;
        int i=0;
        for (ShipFleet fleet1: fleets) {
            // if this planet is colonized, has bases & is aggressive with the fleet
            if (planet().isColonized()
            && planet().colony().defense().isArmed()
            && empire().aggressiveWith(fleet1.empire(), this))
                return true;
            for (int j=i+1;j<fleets.size();j++) {
                ShipFleet fleet2 = fleets.get(j);
                if (fleet1.aggressiveWith(fleet2, this)
                && (fleet1.isArmed() || fleet2.isArmed()))
                    return true;
            }
            i++;
        }
        return false;
    }
    public boolean isArmed(Empire emp) {
        // returns true if empire has an armed presence in this system
        if (isColonized()
        && (colony().empire() == emp)
        && colony().defense().isArmed())
            return true;

        ShipFleet fl = this.orbitingFleetForEmpire(emp);
        return (fl != null) && fl.isArmed();
    }
    public List<Empire> empiresInConflict() {
        List<Empire> emps = new ArrayList<>();
        List<ShipFleet> fleets = orbitingFleets();
        for (ShipFleet fl: fleets)
            emps.add(fl.empire());

        if (isColonized() && !emps.contains(colony().empire()))
            emps.add(colony().empire());

        return emps;
    }
    public ShipFleet acceptFleet(ShipFleet fl) {
        List<ShipFleet> fleets = orbitingFleets();
        for (ShipFleet fleet: fleets) {
            if (fl.empId() == fleet.empId()) {
                fleet.addShips(fl);
                fl.clear();
                return fleet;
            }
        }
        orbitingFleets().add(fl);
        return fl;
    }
    public Transport acceptTransport(Transport tr) {
        // friendly transports always immediately land
        Colony col = colony();
        if ((col != null) && (col.empire() == tr.empire())) {
            if (!colony().inRebellion()) {
                colony().acceptTransport(tr);
                return null;
            }
        }
        for (Transport trans : orbitingTransports) {
            if (tr.empire() == trans.empire()) {
                trans.joinWith(tr);
                return trans;
            }
        }
        session().replaceVarValue(tr, this);
        orbitingTransports.add(tr);
        return tr;
    }
    public String getAttribute(String key) {
        switch (key) {
            case "NAME":             return empire().sv.name(id);
            case "POPULATION":       return str(empire().sv.population(id));
            case "DELTA_POPULATION": return str(empire().sv.deltaPopulation(id));
            case "SIZE":             return str(empire().sv.currentSize(id));
            case "PLANET_TYPE":      return planet().type().name();
            case "NOTES":            return notes();
            case "FACTORIES":        return str(empire().sv.factories(id));
            case "DELTA_FACTORIES":  return str(empire().sv.deltaFactories(id));
            case "WASTE":            return str((int)colony().ecology().waste());
            case "INCOME":           return str((int)colony().totalIncome());
            case "RESERVE":          return str((int)colony().reserveIncome());
            case "BASES":            return str(empire().sv.bases(id));
            case "SHIPYARD":         return colony().shipyardProject();
            case "DELTA_BASES":      return str(empire().sv.deltaBases(id));
            case "SHIELD":           return str(colony().defense().shieldLevel());
            case "TRANSPORT_TURNS":  return str((int)Math.ceil(transportTimeTo(TARGET_SYSTEM)));
            case "RESOURCES":        return text(empire().sv.view(id).resourceType());
        }
        return "";
    }
    public static Comparator<StarSystem> NAME               = (StarSystem o1,   StarSystem o2)   -> o1.name().compareTo(o2.name());
    public static Comparator<StarSystem> PLANET_TYPE        = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.planet().type().hostility(),sys2.planet().type().hostility());
    public static Comparator<StarSystem> NOTES              = (StarSystem sys1, StarSystem sys2) -> sys1.notes().compareTo(sys2.notes());
    public static Comparator<StarSystem> SHIPYARD           = (StarSystem sys1, StarSystem sys2) -> sys1.colony().shipyardProject().compareTo(sys2.colony().shipyardProject());
    public static Comparator<StarSystem> RESOURCES          = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.planet().resourcesSort(),sys2.planet().resourcesSort());
    public static Comparator<StarSystem> INDUSTRY_RESERVE   = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().reserveIncome(),sys2.colony().reserveIncome());
    public static Comparator<StarSystem> FACTORIES          = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().industry().factories(),sys2.colony().industry().factories());
    public static Comparator<StarSystem> BASE_PRODUCTION    = (StarSystem o1,   StarSystem o2)   -> Base.compare(o1.colony().production(),o2.colony().production());
    public static Comparator<StarSystem> WASTE              = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().ecology().waste(),sys2.colony().ecology().waste());
    public static Comparator<StarSystem> INCOME             = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().totalIncome(),sys2.colony().totalIncome());
    public static Comparator<StarSystem> BASES              = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().defense().bases(),sys2.colony().defense().bases());
    public static Comparator<StarSystem> SHIELD             = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().defense().shieldLevel(),sys2.colony().defense().shieldLevel());
    public static Comparator<StarSystem> INVASION_PRIORITY  = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.empire().generalAI().invasionPriority(sys1),sys2.empire().generalAI().invasionPriority(sys2));
    public static Comparator<StarSystem> TRANSPORT_PRIORITY = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.empire().fleetCommanderAI().transportPriority(sys1),sys2.empire().fleetCommanderAI().transportPriority(sys2));
    public static Empire VIEWING_EMPIRE;
    public static Comparator<StarSystem> VDISTANCE = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(VIEWING_EMPIRE.sv.distance(sys1.id),VIEWING_EMPIRE.sv.distance(sys2.id));
    };
    public static Comparator<StarSystem> VPOPULATION = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(VIEWING_EMPIRE.sv.population(sys1.id),VIEWING_EMPIRE.sv.population(sys2.id));
    };
    public static Comparator<StarSystem> POPULATION = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(sys1.population(),sys2.population());
    };
    public static Comparator<StarSystem> CURRENT_SIZE = (StarSystem sys1, StarSystem sys2) -> {
        Empire emp = sys1.empire();
        return Base.compare(emp.sv.currentSize(sys1.id),emp.sv.currentSize(sys2.id));
    };
    public static StarSystem TARGET_SYSTEM;
    public static Comparator<StarSystem> DISTANCE_TO_TARGET_SYSTEM = new Comparator<StarSystem>() {
        @Override
        public int compare(StarSystem sys1, StarSystem sys2) {
            float pr1 = sys1.distanceTo(TARGET_SYSTEM);
            float pr2 = sys2.distanceTo(TARGET_SYSTEM);
            return Base.compare(pr1, pr2);
        }
    };
    //
    // SUPPORTING BEHAVIOR FOR SPRITES
    //
    private Rectangle nameBox() {
        if (nameBox == null)
            nameBox = new Rectangle();
        return nameBox;
    }
    private int twinkleCycle() {
        if (twinkleCycle == 0)
            twinkleCycle = roll(20,50);
        return twinkleCycle;
    }
    private int twinkleOffset() {
        if (twinkleOffset == 0)
            twinkleOffset = roll(0,500);
        return twinkleOffset;
    }
    private int drawRadius() {
        if (drawRadius == 0)
            drawRadius = scaled(roll(4,6));
        return drawRadius;
    }
    @Override
    public IMappedObject source() { return this; }
    @Override
    public boolean persistOnClick()      { return true; }
    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public void repaint(GalaxyMapPanel map)     {
        int r = map.scale(1.0f);
        int x1 = map.mapX(x());
        int y1 = map.mapY(y());
        map.repaint(x1-r,y1-r,r+r,r+r);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        displayed = false;
        if (!map.displays(this))
            return;

        Empire pl = player();
        int s7 = BasePanel.s7;

        displayed = true;
        int x0 = mapX(map);
        int y0 = mapY(map);
        int r0 = drawRadius(map);
        twinkleOffset++;

        Empire emp = pl.sv.empire(id);
        SystemView sv = pl.sv.view(id);
        // draw ownership radius?
        if (map.parent().showOwnerReach(this))
            drawOwnerReach(g2, map, emp, x0, y0);

        if (map.parent().drawStar(this)) {
            Color c0 = map.parent().alertColor(sv);
            if (c0 != null) 
                drawAlert(map, g2, c0, x0, y0);
            drawStar(map, g2, x0, y0);
        }
        
        boolean drewSelectionBox = false;
        // draw selection box if this system is selected or we are transporting from it
        if (map.parent().isClicked(this)
        || map.parent().isClicked(transportSprite())) 
            drawSelection(g2, map, emp, x0, y0);
        else if (map.parent().isHovering(this)) 
            drawHovering(g2, map, x0, y0);

        // draw shield?
        if (map.parent().drawShield(this))
            drawShield(g2, pl.sv.shieldLevel(id), x0, y0, BasePanel.s15);

        // draw stargate icon (AFTER selection box)
        if (pl.sv.isColonized(id) && pl.sv.hasStargate(id)) {
            if (map.scaleX() <= GalaxyMapPanel.MAX_STARGATE_SCALE) {
                float mult = max(4, min(60,map.scaleX()));
                int x1 = x0+(int)(scaled(200)/mult);
                int y1 = y0-(int)(scaled(500)/mult);
                Image img = ShipLibrary.current().stargate.getImage();
                int w = img.getWidth(null);
                int h = img.getHeight(null);
                g2.drawImage(img, x1, y1, x1+BasePanel.s14, y1+BasePanel.s14, 0, 0, w, h, map);
            }
        }
        Color flagColor = map.parent().flagColor(this);
        if (flagColor != null)
            drawBanner(g2, flagColor, Color.white, x0, y0);

        // draw star name
        Rectangle box = nameBox();
        box.width = 0;
        box.height = 0;
        if (map.showSystemNames()) {
            String s1 = map.parent().systemLabel(this);
            String s2 = map.parent().systemLabel2(this);
            if (s2.isEmpty())
                s2 = name2(map);
            if (!s1.isEmpty() || !s2.isEmpty()) {
                Font prevFont = g2.getFont();
                int fontSize = fontSize(map);
                g2.setFont(narrowFont(fontSize));
                g2.setColor(map.parent().systemLabelColor(this));
                int sw = g2.getFontMetrics().stringWidth(s1);
                int boxSize = drewSelectionBox ? r0+s7 : r0;
                int yAdj = scaled(fontSize)+boxSize;
                if (!s1.isEmpty()) {
                    g2.drawString(s1, x0-(sw/2), y0+yAdj);
                    y0 += scaled(fontSize-2);
                }
                if (!s2.isEmpty()) {
                    g2.setFont(narrowFont(fontSize-2));
                    int sw2 = g2.getFontMetrics().stringWidth(s2);
                    g2.drawString(s2, x0-(sw2/2), y0+yAdj);
                }

                g2.setFont(prevFont);
                box.x = x0-(sw/2);
                box.y = y0+yAdj - BasePanel.s20;
                box.width = sw;
                box.height = BasePanel.s20;
            }
        }
    }
    private String name2(GalaxyMapPanel map) {
        IMappedObject obj = map.parent().gridOrigin();
        if (obj == null)
            return "";
        float dist = distanceTo(obj);
        if (dist == 0)
            return "";
        String dist1 = df1.format(dist);
        return text("SYSTEMS_RANGE",dist1);
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        if (!displayed)
            return false;
        if (nameBox().contains(mapX, mapY)) 
            return true;
        int spriteX = map.mapX(x());
        int spriteY = map.mapY(y());
        float clickR = map.scale(map.parent().systemClickRadius());
        float dist = distance(spriteX, spriteY, mapX, mapY);
        return dist <= max(BasePanel.s2, clickR);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean sound) {
//        if (canShowDetail(map)) 
//            RotPUI.instance().selectDisplaySystemPanel(this);
    }
    private boolean canShowDetail(GalaxyMapPanel map)   { return map.parent().isClicked(this) && player().sv.isScouted(id); }
    @Override
    public void mouseEnter(GalaxyMapPanel map) {
        hovering = true;
        if (canShowDetail(map))
            map.setPromptCursor();
    }
    @Override
    public void mouseExit(GalaxyMapPanel map) {
        hovering = false;
        if (map.parent().isClicked(this))
            map.setCrosshairsCursor();
    }
    private float flareSize(GalaxyMapPanel map)  {
        if (!playAnimations())
            return 1.0f;
        //return 0;
        int rem = twinkleOffset() % twinkleCycle(map);
        switch (rem) {
            case 0: return 1.0f;
            case 1: return 2.0f;
            case 2: return 1.5f;
            case 3: return 1.0f;
            default: return 1.0f;
        }
    }
    private int twinkleCycle(GalaxyMapPanel map) {
        // adjust by map scale to avoid excessive
        // twinkling when zoomed way out
        return (int) max(100,(twinkleCycle()*map.scaleY()/20));
    }
    private int drawRadius(GalaxyMapPanel map) {
        return (int) max(BasePanel.s1, (drawRadius() * 60 / map.scaleX()));
    }
    private void drawOwnerReach(Graphics2D g, GalaxyMapPanel map, Empire emp, int x, int y) {
        if (emp == null)
            return;

        float reach = map.parent().ownerReach(this);
        int r  = map.scale(reach);
        g.setColor(emp.reachColor());
        g.fillOval(x-r, y-r, r+r, r+r);
    }
    public void drawBanner(Graphics2D g, Color c0, Color c1, int x, int y) {
        int w=scaled(2);
        int sp=scaled(10);
        int h=scaled(20);
        int flagW=scaled(16);
        int flagH=scaled(8);
        Polygon p = new Polygon();
        p.addPoint(x-flagW, y-h-sp+(flagH/2));
        p.addPoint(x, y-h-sp);
        p.addPoint(x, y-h-sp+flagH);
        
        // if c is null, just draw a black outline (for the colony info panel)
        if (c0 == null) {
            g.setColor(c1);
            g.draw(p);
            g.fillRect(x, y-h-sp, w, h);
        }
        else {
            g.setColor(c0);
            g.fill(p);
            g.setColor(c1);
            g.fillRect(x, y-h-sp, w, h);
        }
    }
    private void drawAlert(GalaxyMapPanel map, Graphics2D g2, Color alertC, int x, int y) {
        int r = map.scale(0.75f);
        int r1 = map.scale(0.25f);
        
        if (!map.parent().animating() ||(map.animationCount() % 20 > 0)) {
            g2.setColor(alertC);
            Area a = new Area(new RoundRectangle2D.Float(x-r, y-(r/6), r+r, r/3, r1/3, r1/3));
            a.add(new Area(new RoundRectangle2D.Float(x-(r/6), y-r, r/3, r+r, r1/3, r1/3)));
            a.subtract(new Area(new Ellipse2D.Float(x-r1,y-r1,r1+r1,r1+r1)));
            g2.fill(a);
        }      
    }
    public void drawStar(GalaxyMapPanel map, Graphics2D g2, int x, int y) {
        int r0 = drawRadius(map);
        if (r0 < BasePanel.s4)
            r0 = (int) (r0 * flareSize(map));

        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver);
        BufferedImage img = starType().image(r0,0);
        int w = img.getWidth();
        g2.drawImage(img,x-(w/2),y-(w/2),null);
        g2.setComposite(prev);
    }
    private void drawSelection(Graphics2D g, GalaxyMapPanel map, Empire emp, int x, int y) {
        int r = map.scale(1.0f);

        Stroke prev = g.getStroke();
        int mod = map.animationCount()%15/5;
        switch(mod) {
            case 0: g.setStroke(BasePanel.stroke2); break;
            case 1: g.setStroke(BasePanel.stroke3); break;
            case 2:
            default: g.setStroke(BasePanel.stroke4); break;
        }

        if (emp == null)
            g.setColor(selectionC);
        else
            g.setColor(emp.color());
        
        int r0 = map.scale(1.0f);
        int r1 = map.scale(0.8f);

        int shape = emp == null ? Empire.SHAPE_CIRCLE : emp.shape();
        switch(shape) {
            case Empire.SHAPE_SQUARE:
                g.drawRect(x-r1, y-r1, r1+r1, r1+r1); break;
            case Empire.SHAPE_DIAMOND:
                Polygon p = new Polygon();
                p.addPoint(x, y-r0);
                p.addPoint(x-r0, y);
                p.addPoint(x, y+r0);
                p.addPoint(x+r0, y);
                g.draw(p); break;
            case Empire.SHAPE_TRIANGLE1:
                Polygon p1 = new Polygon();
                p1.addPoint(x, y-r0);
                p1.addPoint(x-r0, y+r1);
                p1.addPoint(x+r0, y+r1);
                g.draw(p1); break;
            case Empire.SHAPE_TRIANGLE2:
                Polygon p2 = new Polygon();
                p2.addPoint(x, y+r0);
                p2.addPoint(x-r0, y-r1);
                p2.addPoint(x+r0, y-r1);
                g.draw(p2);
                break;
            case Empire.SHAPE_CIRCLE:
            default:
                g.drawOval(x-r0, y-r0, r0+r0, r0+r0); break;
        }
        g.setStroke(prev);
    }
    private void drawHovering(Graphics2D g, GalaxyMapPanel map, int x, int y) {
        int r = map.scale(0.5f);

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke1);
        g.setColor(selectionC);
        g.drawOval(x-r, y-r, r+r, r+r);
        g.setStroke(prev);
    }
    public static void drawShield(Graphics2D g, int shieldLevel, int x, int y, int r) {
        if (shieldLevel == 0)
            return;

        Stroke prevStroke = g.getStroke();
        Stroke shieldStroke = BasePanel.stroke3;
        Stroke shieldBorderStroke = BasePanel.stroke5;
        g.setStroke(shieldStroke);
        switch (shieldLevel) {
            case 5:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 30, 120);
                g.setColor(shield5C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 30, 120);
                break;
            case 10:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r,0, 180);
                g.setColor(shield10C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r,0, 180);
                break;
            case 15:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                g.setColor(shield15C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                break;
            case 20:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                g.setColor(shield20C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                break;
        }
        g.setStroke(prevStroke);
    }
    private int fontSize(GalaxyMapPanel map) {
        int maxFont = 72;
        int minFont = 4;
        return bounds(minFont, (int)(maxFont * 8 / map.scaleX()), maxFont);
    }
}
