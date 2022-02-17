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
import java.awt.Image;
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
    
    public static final int FLAG_NONE = 0;
    static final int FLAG_WHITE = 1;
    static final int FLAG_RED = 2;
    static final int FLAG_BLUE = 3;
    static final int FLAG_GREEN = 4;
    static final int FLAG_YELLOW = 5;
    static final int FLAG_AQUA = 6;
    static final int FLAG_ORANGE = 7;
    static final int FLAG_LTBLUE = 8;
    static final int FLAG_PURPLE = 9;
    static final int FLAG_PINK = 10;

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
    private boolean forwardRallies = false;

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
    public void removeStargate()             { vStargate = false;
        if (isColonized())
            colony().removeStargate();
    }
    public Planet planet()                   { return vPlanet; }
    public int locationSecurity()            { return locationSecurity; }
    public float hostilityLevel()            { return hostilityLevel; }
    public float spyTime()                   { return spyTime; }
    public float scoutTime()                 { return scoutTime; }
    public boolean isGuarded()               { return vGuarded; }
    public StarSystem rallySystem()          { return relocationSystem; }
    public float spyTurn()                   { return spyTime - galaxy().beginningYear(); }
    public float scoutTurn()                 { return scoutTime - galaxy().beginningYear(); }
    public void rallySystem(StarSystem sys)  {
        if (canRallyTo(sys)) {
            relocationSystem = (sys == system()) ? null : sys;
            system().rallySprite().clear();
        }
    }
    public boolean forwardRallies()          { return forwardRallies; }
    public void toggleForwardRallies()       { forwardRallies = !forwardRallies; }
    public void stopRally() { 
        relocationSystem = null;
        system().rallySprite().stop(); 
    }
    public void goExtinct() {
        vEmpire = null;
        vBases = 0;
        vShieldLevel = 0;
        vFactories = 0;
        vPopulation = 0;
        vStargate = false;
        clearHostility();
    }
    public int flagColorId()  { return flagColor; }
    public Color flagColor() { 
        switch(flagColor) {
            case FLAG_RED:    return Color.red;
            case FLAG_WHITE:  return Color.white;
            case FLAG_BLUE:   return Color.blue;
            case FLAG_GREEN:  return Color.green;
            case FLAG_YELLOW: return Color.yellow;
        }
        return null; 
    }
    public Image flagImage() {
        switch(flagColor) {
            case FLAG_NONE:   return image("Flag_None");
            case FLAG_RED:    return image("Flag_Red");
            case FLAG_WHITE:  return image("Flag_White");
            case FLAG_BLUE:   return image("Flag_Blue");
            case FLAG_GREEN:  return image("Flag_Green");
            case FLAG_YELLOW: return image("Flag_Yellow");
            case FLAG_AQUA:   return image("Flag_Aqua");
            case FLAG_ORANGE: return image("Flag_Orange");
            case FLAG_LTBLUE: return image("Flag_LtBlue");
            case FLAG_PURPLE: return image("Flag_Purple");
            case FLAG_PINK:   return image("Flag_Pink");
            default:          return image("Flag_White");
        }
    }
    public Image mapFlagImage() {
        switch(flagColor) {
            case FLAG_NONE:   return null;
            case FLAG_RED:    return image("Flag_RedM");
            case FLAG_WHITE:  return image("Flag_WhiteM");
            case FLAG_BLUE:   return image("Flag_BlueM");
            case FLAG_GREEN:  return image("Flag_GreenM");
            case FLAG_YELLOW: return image("Flag_YellowM");
            case FLAG_AQUA:   return image("Flag_AquaM");
            case FLAG_ORANGE: return image("Flag_OrangeM");
            case FLAG_LTBLUE: return image("Flag_LtBlueM");
            case FLAG_PURPLE: return image("Flag_PurpleM");
            case FLAG_PINK:   return image("Flag_PinkM");
            default:          return image("Flag_WhiteM");
        }
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
        return (fleetPlan != null) 
           && (fleetPlan.needsShips() || fleetPlan.isRetreating());
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
        if (!scouted()) {
            log("Orbital scan scouts new system: ", system().name());
            owner().shareSystemInfoWithAllies(this);
            if (owner().isPlayerControlled()) {
                session().addSystemScouted(system());
                if (system().empire() != player())
                    system().addEvent(new SystemScoutedEvent(player().id));
            }
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
    public void refreshAllySharingScan() {
        if (owner().isPlayerControlled() && !scouted()) {
            log("Ally shares new system data: ", system().name());
            session().addSystemScoutedByAllies(system());
        }

        scoutTime = galaxy().currentYear();
        setName();
        setEmpire();
        setPlanetData();
    }
    public void refreshLongRangePlanetScan() {
        if (!scouted()) {
            log("Long range planet scan scouts new system: ", system().name());
            owner().shareSystemInfoWithAllies(this);
            if (owner().isPlayerControlled())
                session().addSystemScoutedByAstronomers(system());
        }

        scoutTime = galaxy().currentYear();
        setName();
        setEmpire();
        setPlanetData();
        setOrbitingFleets();
    }
    public void refreshLongRangeShipScan() {
        if (!scouted()) {
            log("Long range ship scan scouts new system: ", system().name());
            owner().shareSystemInfoWithAllies(this);
            if (owner().isPlayer())
                session().addSystemScouted(system());
        }

        scoutTime = galaxy().currentYear();
        setName();
        setEmpire();
        setPlanetData();
        setOrbitingFleets();
    }
    public void refreshSpyScan() {
        setName();
        setEmpire();
        setColonyData();
        spyTime = galaxy().currentYear();
    }
    public void setEmpire() {
        // if the empire has changed, reset the spy time
        Empire prevEmpire = vEmpire;
        vEmpire = system().empire();
        if (vEmpire != prevEmpire)
            spyTime = 0;
        
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
            vCurrentSize = (int) system().planet().currentSize();
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
    public boolean abandoned()               { return system() == null ? false : system().abandoned(); }
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

    public void resetFlagColor()            { flagColor = FLAG_NONE; }
    public void toggleFlagColor(boolean reverse) {
        if (reverse) {
             switch(flagColor) {
                case FLAG_NONE:   flagColor = FLAG_PINK; return;
                case FLAG_WHITE:  flagColor = FLAG_NONE; return;
                case FLAG_RED:    flagColor = FLAG_WHITE; return;
                case FLAG_BLUE:   flagColor = FLAG_RED; return;
                case FLAG_GREEN:  flagColor = FLAG_BLUE; return;
                case FLAG_YELLOW: flagColor = FLAG_GREEN; return;
                case FLAG_AQUA:   flagColor = FLAG_YELLOW; return;
                case FLAG_ORANGE: flagColor = FLAG_AQUA; return;
                case FLAG_LTBLUE: flagColor = FLAG_ORANGE; return;
                case FLAG_PURPLE: flagColor = FLAG_LTBLUE; return;
                case FLAG_PINK:   flagColor = FLAG_PURPLE; return;
            }
        }
        else {
            switch(flagColor) {
                case FLAG_NONE:   flagColor = FLAG_WHITE; return;
                case FLAG_WHITE:  flagColor = FLAG_RED; return;
                case FLAG_RED:    flagColor = FLAG_BLUE; return;
                case FLAG_BLUE:   flagColor = FLAG_GREEN; return;
                case FLAG_GREEN:  flagColor = FLAG_YELLOW; return;
                case FLAG_YELLOW: flagColor = FLAG_AQUA; return;
                case FLAG_AQUA:   flagColor = FLAG_ORANGE; return;
                case FLAG_ORANGE: flagColor = FLAG_LTBLUE; return;
                case FLAG_LTBLUE: flagColor = FLAG_PURPLE; return;
                case FLAG_PURPLE: flagColor = FLAG_PINK; return;
                case FLAG_PINK:   flagColor = FLAG_NONE; return;
            }
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
    public int lastReportTurn()              { return (int) max(spyTurn(), scoutTurn()); }
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
        if(system().empire() == player())
        {
            for (ShipFleet fl: orbitingFleets()) {
                if (fl.isPotentiallyArmed(player())) {
                    if (player().atWarWith(fl.empId())) { 
                        return true;
                    }  
                }
            }
        }
        return (scouted() && isColonized() && colony().inRebellion());
    }
    public String descriptiveName() {
        if (!isColonized()) {
            if (!scouted()) 
                return text("MAIN_UNSCOUTED");
            else if (system().planet().isEnvironmentNone())
                return text("MAIN_NO_PLANETS");
            else if (abandoned())
                return text("MAIN_ABANDONED");
            else
                return text("MAIN_NO_COLONIES");
        }
        String name;
        if (!scouted())
            name = text("PLANET_WORLD",empire().raceName());
        else if (empire().isHomeworld(system()))
            name = text("PLANET_HOMEWORLD",empire().raceName());
        else if (empire().isColony(system()))
            name = text("PLANET_COLONY",empire().raceName());
        else
            name = text("PLANET_WORLD",empire().raceName());
        
        name = empire().replaceTokens(name, "alien");
        return name;
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
    public int maxPopToGive(float targetPopPct) {
        if (!colony().canTransport())
            return 0;

        int p1 = colony().maxTransportsAllowed();
        int p2 = (int) (colony().population() - (targetPopPct * system().planet().currentSize()));
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
