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
package rotp.model.empires;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.FleetPlan;
import rotp.model.colony.Colony;
import rotp.model.events.SystemScoutedEvent;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetType;
import rotp.util.Base;

public class SystemView implements IMappedObject, Base, Serializable {
    private static final long serialVersionUID = 1L;
    protected static final int UNIMPORTANT = 0;
    protected static final int INNER_SYSTEM = 1;
    protected static final int BORDER_SYSTEM = 2;
    protected static final int ATTACK_TARGET = 3;
    
    static final int FLAG_NONE = 0;
    static final int FLAG_WHITE = 1;
    static final int FLAG_RED = 2;
    static final int FLAG_BLUE = 3;

    public static SystemView create(int sysId, int empId) {
        return new SystemView(sysId,empId);
    }

    public final int ownerId;
    public final int sysId;
    private StarSystem relocationSystem;
    private float hostilityLevel = 0;
    private int locationSecurity = INNER_SYSTEM;
    private float scoutTime = 0;
    private float spyTime = 0;

    // viewed variables
    private Empire vEmpire;
    private String vName = "";
    private Planet vPlanet;
    private String vPlanetTypeKey;
    private final List<ShipFleet> vOrbitingFleets = new ArrayList<>();
    private final List<ShipFleet> vExitingFleets = new ArrayList<>();
    private boolean vGuarded = false;
    private int vBases = 0;
    private int vShieldLevel = 0;
    private int vFactories = 0;
    private int vPopulation = 0;
    private int vCurrentSize = 0;
    private int vArtifacts = 0;
    private boolean vStargate = false;
    private int flagColor = FLAG_NONE;

    private transient Empire owner;
    private transient PlanetType vPlanetType;
    private transient FleetPlan fleetPlan;

    public List<ShipFleet> orbitingFleets()  { return vOrbitingFleets; }
    public List<ShipFleet> exitingFleets()   { return vExitingFleets; }
    public StarSystem system()               { return galaxy().system(sysId); }
    public String name()                     { return vName; }
    public void name(String s) {
        system().name(s);
        vName = s;
    }
    public Empire owner() { 
        if (owner == null)
            owner = galaxy().empire(ownerId);
        return owner; 
    }
    public Empire empire()                   { return vEmpire; }
    public int empId()                       { return id(vEmpire); }
    public int population()                  { return vPopulation; }
    public int bases()                       { return vBases; }
    public int shieldLevel()                 { return vShieldLevel; }
    public int factories()                   { return vFactories; }
    public int currentSize()                 { return vCurrentSize; }
    public int artifacts()                   { return vArtifacts; }
    public boolean stargate()                { return vStargate; }
    public Planet planet()                   { return vPlanet; }
    public int locationSecurity()            { return locationSecurity; }
    public float hostilityLevel()            { return hostilityLevel; }
    public float spyTime()                   { return spyTime; }
    public float scoutTime()                 { return scoutTime; }
    public boolean isGuarded()               { return vGuarded; }
    public StarSystem rallySystem()          { return relocationSystem; }
    public void rallySystem(StarSystem sys)  {
        if (canRallyTo(sys)) {
            relocationSystem = (sys == system()) ? null : sys;
            system().rallySprite().clear();
        }
    }
    public void stopRally()                 { rallySystem(system()); }
    public Color flagColor() { 
        switch(flagColor) {
            case FLAG_RED:   return Color.red;
            case FLAG_WHITE: return Color.white;
            case FLAG_BLUE:  return Color.blue;
        }
        return null; 
    }
    public PlanetType planetType() {
        if (vPlanetTypeKey == null)
            return null;
        if (vPlanetType == null)
            vPlanetType = PlanetType.keyed(vPlanetTypeKey);
        return vPlanetType;
    }
    public void clearFleetPlan() {
        if (fleetPlan != null)
            fleetPlan.clear();
    }
    public boolean hasFleetPlan() {
        return (fleetPlan != null) && fleetPlan.needsShips();
    }
    public FleetPlan fleetPlan()  {
        if (fleetPlan == null)
            fleetPlan = new FleetPlan(ownerId, sysId);
        return fleetPlan;
    }
    public void raiseHostility()                   { hostilityLevel++; }
    public void resetSystemData()                  { setLocationSecurity(); }
    public void refreshSystemEntryScan() {
        vGuarded = system().hasMonster();      
        if (vGuarded)
            setName();
    }
    public void refreshFullScan() {
        if (owner().isPlayer() && !scouted()) {
            log("Orbital scan scouts new system: ", system().name());
            session().addSystemScouted(system());
            if (system().empire() != player())
                system().addEvent(new SystemScoutedEvent(player().id));
        }
        scoutTime = galaxy().currentYear();
        spyTime = galaxy().currentYear();
        setName();
        setEmpire();
        setPlanetData();
        setColonyData();
        setOrbitingFleets();
        refreshSystemEntryScan();
                
        if (system().hasBonusTechs())
            owner().plunderAncientTech(system());
    }
    public void refreshLongRangeScan() {
        if (owner().isPlayer() && !scouted()) {
            log("Long range planet scan scouts new system: ", system().name());
            session().addSystemScouted(system());
        }

        scoutTime = galaxy().currentYear();
        setName();
        setEmpire();
        setPlanetData();
        setOrbitingFleets();
    }
    public void refreshSpyScan() {
        spyTime = galaxy().currentYear();
        setName();
        setEmpire();
        setColonyData();
    }
    public void setEmpire() {
        vEmpire = system().empire();
        if (!owner().aggressiveWith(id(vEmpire)))
            clearHostility();
    }
    private void setPlanetData() {
        vArtifacts = system().planet().artifacts();
        vPlanet = system().planet();
        vPlanetTypeKey = system().planet().type().key();
        vCurrentSize = (int) system().planet().currentSize();
    }
    private void setColonyData() {
        Colony col = system().colony();
        vStargate = system().isColonized() && col.hasStargate();
        vBases = 0;
        vFactories = 0;
        vPopulation = 0;
        vShieldLevel = 0;
        if (isColonized()) {
            float actualPop = col.population();
            vBases = col.defense().missileBases();
            vFactories = (int) col.industry().factories();
            vPopulation = actualPop < 1 ? (int) Math.ceil(actualPop) : (int) actualPop;
            vShieldLevel = col.defense().shieldLevel();
        }
    }
    private void setOrbitingFleets() {
        orbitingFleets().clear();
        exitingFleets().clear();
        vGuarded = system().hasMonster();
        vOrbitingFleets.clear();
        vOrbitingFleets.addAll(system().orbitingFleets());
        vExitingFleets.clear();
        vExitingFleets.addAll(system().exitingFleets());
    }
    public void clearFleetInfo() {
        orbitingFleets().clear();
        exitingFleets().clear();
    }

    public float distance()                 { return owner().sv.distance(system().id); }
    public boolean hasRallyPoint()           { return rallySystem() != null; }
    public Colony colony()                   { return system() == null ? null : system().colony(); }
    public Integer deltaPopulation()         { return isColonized() ? colony().deltaPopulation() : 0; }
    public Integer deltaFactories()          { return isColonized() ? colony().industry().deltaFactories() : 0; }
    public Integer deltaBases()              { return isColonized() ? colony().defense().deltaBases() : 0; }
    public BufferedImage planetTerrain()     { return planetType() == null ? null : planetType().terrainImage(); }

    public boolean resourceUltraRich()       { return (planet() != null) && planet().isResourceUltraRich(); }
    public boolean resourceRich()            { return (planet() != null) && planet().isResourceRich(); }
    public boolean resourceNormal()          { return (planet() != null) && planet().isResourceNormal(); }
    public boolean resourcePoor()            { return (planet() != null) && planet().isResourcePoor(); }
    public boolean resourceUltraPoor()       { return (planet() != null) && planet().isResourceUltraPoor(); }
    public boolean artifact()                { return (planet() != null) && planet().isArtifact(); }
    public boolean orionArtifact()           { return (planet() != null) && planet().isOrionArtifact(); }
    
    public boolean environmentHostile()     { return (planet() != null) && planet().isEnvironmentHostile(); }
    public boolean environmentFertile()     { return (planet() != null) && planet().isEnvironmentFertile(); }
    public boolean environmentGaia()        { return (planet() != null) && planet().isEnvironmentGaia(); }

    public void toggleFlagColor() {
        switch(flagColor) {
            case FLAG_NONE:  flagColor = FLAG_WHITE; return;
            case FLAG_WHITE: flagColor = FLAG_RED; return;
            case FLAG_RED:   flagColor = FLAG_BLUE; return;
            case FLAG_BLUE:  flagColor = FLAG_NONE; return;
        }
    }
    public String resourceType() {
        if (artifact() || orionArtifact())
            return "PLANET_ARTIFACTS";
        if (resourceUltraPoor())
            return "PLANET_ULTRA_POOR";
        else if (resourcePoor())
            return "PLANET_POOR";
        else if (resourceUltraRich())
            return "PLANET_ULTRA_RICH";
        else if (resourceRich())
            return "PLANET_RICH";
        else
            return "";
    }
    public String ecologyType() {
        if (environmentHostile())
            return "PLANET_HOSTILE";
        else if (environmentFertile())
            return "PLANET_FERTILE";
        if (environmentGaia())
            return "PLANET_GAIA";
        else
            return "";
    }
    public boolean canSabotageBases()        { return bases() > 0; }
    public boolean canSabotageFactories()    { return factories() > 0; }
    public boolean canInciteRebellion()      { 
        if (!isColonized())
            return false;
        if (colony().inRebellion())
            return false;
        if (colony().isCapital())
            return false;
        // special case: we cannot incite rebellion against a final war enemy 
        // if we are rebelling against the New Republic
        Empire leader = galaxy().council().leader();
        if (leader != null) {
            if (owner().viewForEmpire(vEmpire).embassy().finalWar()) {                   
                if ((vEmpire == leader) || vEmpire.viewForEmpire(leader).embassy().unity())
                    return false;
            }
        }
        return true;
    }

    public boolean scouted()                 { return (owner() == empire()) || (scoutTime() > 0); }
    public boolean spied()                   { return (owner() == empire()) || (spyTime() > 0); }
    public int lastReportYear()              { return (owner() == empire()) ? galaxy().currentYear() : (int) spyTime(); }
    public int spyReportAge()                { return galaxy().currentYear() - lastReportYear(); }

    public boolean canRallyTo(StarSystem sys) { return sys.empire() == owner(); }

    public float distanceTo(SystemView v)   { return system().distanceTo(v.system()); }

    public int desiredMissileBases() {
        return (empire() == owner()) ? colony().defense().maxBases() : 0;
    }
    public boolean innerSystem()             { return locationSecurity() == INNER_SYSTEM; }
    public boolean supportSystem()           { return false; }
    public boolean borderSystem()            { return locationSecurity() == BORDER_SYSTEM; }
    public boolean attackTarget()            { return locationSecurity() == ATTACK_TARGET; }

    public boolean isColonized()             { return (empire() != null) && (colony() != null); }
    public boolean isInEmpire()              { return owner() == system().empire();}
    
    public boolean isAlert() {
        if (vName.isEmpty())
            return false;
        if (scouted() && system().hasEvent())
            return true;
        return (scouted() && isColonized() && colony().inRebellion());
    }
    public String descriptiveName() {
        if (!isColonized()) {
            if (!scouted()) 
                return text("MAIN_UNSCOUTED");
            else if (system().planet().isEnvironmentNone())
                return text("MAIN_NO_PLANETS");
            else
                return text("MAIN_NO_COLONIES");
        }
        if (!scouted())
            return text("PLANET_WORLD",empire().raceName());
        if (empire().isHomeworld(system()))
            return text("PLANET_HOMEWORLD",empire().raceName());
        else if (empire().isColony(system()))
            return text("PLANET_COLONY",empire().raceName());
        else
            return text("PLANET_WORLD",empire().raceName());
    }

    @Override
    public String toString()       { return concat("View: ", name()); }
    @Override
    public float x()               	  			{ return system().x();  }
    @Override
    public float y()             				  { return system().y();  }

    public boolean hasActiveTransport()        { return isColonized() && colony().transport().isActive(); }
    public boolean hasFleetForCiv (Empire c) {
        return system().hasFleetForEmpire(c);
    }
    public boolean inRange(float range) {
        if (distance() <= range)
            return true;
        for (Empire ally: owner().allies()) {
            if (ally.sv.withinRange(system().id, range))
                return true;
        }
        return false;
    }
    public boolean inShipRange()  { return inRange(owner().tech().shipRange()); }

    public float targetPopPct() {
        if (borderSystem()) return .75f;

        if (system().planet().isResourceRich()) return .75f;
        if (system().planet().isResourceUltraRich()) return .75f;
        if (system().planet().isArtifact()) return .75f;
        if (system().planet().isOrionArtifact()) return .75f;
        if (system().planet().currentSize() <= 20) return .75f;

        if (supportSystem()) return .5f;
        if (system().planet().currentSize() <= 40) return .5f;

        return .25f;
    }
    public float popNeeded() {
        return colony().calcPopNeeded(targetPopPct());
    }
    public int maxPopToGive() {
        if (!colony().canTransport())
            return 0;

        int p1 = colony().maxTransportsAllowed();
        int p2 = (int) (colony().population() - (targetPopPct() * system().planet().currentSize()));
        return Math.min(p1,p2);
    }
    public float defenderCombatAdj() {
        if (empire() == owner())
            return colony().defenderCombatAdj();

        EmpireView cv = owner().viewForEmpire(empire());

        if (cv == null)
            return 0;

        return cv.spies().tech().troopCombatAdj(true);
    }
    public int rallyTurnsTo(StarSystem dest) {
            return (int) Math.ceil(system().rallyTimeTo(dest));
    }
    public int maxTransportsToReceive() {
        if (isColonized()) {
            if (owner() == empire())
                return currentSize() - (int) colony().workingPopulation();
            else
                return currentSize();
        }
        return 0;
    }
    public int maxTransportsToSend() {
        return population() <= 1 ? 0 : population() / 2;
    }
    private SystemView(int sId, int empId) {
        ownerId = empId;
        sysId = sId;
    }
    private void setLocationSecurity() {
        if (distance() > owner().scoutRange()) {
            locationSecurity = UNIMPORTANT;
            return;
        }
        locationSecurity = INNER_SYSTEM;
        float dangerRange = owner().shipRange() + 1;
        for (int i=0; i< system().numNearbySystems();i++) {
            StarSystem sys = system().nearbySystem(i);
            if (distanceTo(sys) > dangerRange)
                break;
            int empId = owner().sv.empId(sys.id);
            if (empId != Empire.NULL_ID) {
                if (owner().atWarWith(empId)) {
                    locationSecurity = ATTACK_TARGET;
                    break;
                }
                if (!owner().friendlyWith(empId))
                    locationSecurity = BORDER_SYSTEM;
            }
        }
    }
    private void setName() {
        if (!system().unnamed())
            vName = system().name();
        else if (vName.isEmpty())
            vName = owner().race().randomSystemName(owner());
    }
    private void clearHostility()                   { hostilityLevel = 0; }
}
