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
package rotp.model.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import rotp.model.ai.base.AIDiplomat;
import rotp.model.ai.base.AIFleetCommander;
import rotp.model.ai.base.AIGeneral;
import rotp.model.ai.base.AIGovernor;
import rotp.model.ai.base.AIScientist;
import rotp.model.ai.base.AIShipCaptain;
import rotp.model.ai.base.AIShipDesigner;
import rotp.model.ai.base.AISpyMaster;
import rotp.model.ai.base.AITreasurer;
import rotp.model.ai.community.AICDiplomat;
import rotp.model.ai.community.AICFleetCommander;
import rotp.model.ai.community.AICGeneral;
import rotp.model.ai.community.AICGovernor;
import rotp.model.ai.community.AICScientist;
import rotp.model.ai.community.AICShipCaptain;
import rotp.model.ai.community.AICShipDesigner;
import rotp.model.ai.community.AICSpyMaster;
import rotp.model.ai.community.AICTreasurer;
import rotp.model.ai.interfaces.Diplomat;
import rotp.model.ai.interfaces.FleetCommander;
import rotp.model.ai.interfaces.General;
import rotp.model.ai.interfaces.Governor;
import rotp.model.ai.interfaces.Scientist;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.ai.interfaces.Treasurer;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.model.ships.ShipDesign;
import rotp.ui.notifications.BombardSystemNotification;
import rotp.ui.notifications.ColonizeSystemNotification;
import rotp.util.Base;

public class AI implements Base {
    private final Empire empire;

    private final Diplomat diplomat;
    private final General general;
    private final FleetCommander fleetCommander;
    private final Governor governor;
    private final Scientist scientist;
    private final ShipCaptain captain;
    private final ShipDesigner shipDesigner;
    private final SpyMaster spyMaster;
    private final Treasurer treasurer;

    public AI (Empire e) {
        empire = e;
        if (options().communityAI()) {
            general = new AICGeneral(empire);
            captain = new AICShipCaptain(empire);
            governor = new AICGovernor(empire);
            scientist = new AICScientist(empire);
            diplomat = new AICDiplomat(empire);
            shipDesigner = new AICShipDesigner(empire);
            fleetCommander = new AICFleetCommander(empire);
            spyMaster = new AICSpyMaster(empire);
            treasurer = new AICTreasurer(empire);
        }
        else {
            general = new AIGeneral(empire);
            captain = new AIShipCaptain(empire);
            governor = new AIGovernor(empire);
            scientist = new AIScientist(empire);
            diplomat = new AIDiplomat(empire);
            shipDesigner = new AIShipDesigner(empire);
            fleetCommander = new AIFleetCommander(empire);
            spyMaster = new AISpyMaster(empire);
            treasurer = new AITreasurer(empire);
        }
    }

    // MISC INTERFACE
    public boolean isAI()     {  return empire.isAI(); }
    public boolean isPlayer() {  return empire.isPlayer(); }
    
    // direct
    public ShipCaptain shipCaptain()                   { return captain; }
    public General general()                           { return general; }
    public Diplomat diplomat()                         { return diplomat; }
    public FleetCommander fleetCommander()             { return fleetCommander; }
    public Governor governor()                         { return governor; }
    public Treasurer treasurer()                       { return treasurer; }
    public Scientist scientist()                       { return scientist; }
    public ShipDesigner shipDesigner()                 { return shipDesigner; }
    public SpyMaster spyMaster()                       { return spyMaster; }

    // uncategorized
    public List<StarSystem> bestSystemsForInvasion(EmpireView v) {
        // invoked when going to war
        List<StarSystem> systems = empire.systemsInShipRange(v.empire());
        Collections.sort(systems,StarSystem.INVASION_PRIORITY);
        return systems;
    }
    public float targetPopPct(StarSystem sys) {
        SystemView sv = empire.sv.view(id(sys));
        if (sv.borderSystem()) return .75f;

        Planet p = sys.planet();
        if (p.isResourceRich()) return .75f;
        if (p.isResourceUltraRich()) return .75f;
        if (p.isArtifact()) return .75f;
        if (p.isOrionArtifact()) return .75f;
        if (p.currentSize() <= 20) return .75f;

        if (sv.supportSystem()) return .5f;
        if (p.currentSize() <= 40) return .5f;

        return .25f;
    }
    private ColonyTransporter createColony(StarSystem sys, int minTransports) {
        int sysId = id(sys);
        int popNeeded = (int) empire.sv.popNeeded(sysId);        
        int maxPopToGive = (int) empire.sv.maxPopToGive(sysId);
        if ((popNeeded < minTransports) && (maxPopToGive < minTransports))
            return null;

        return new ColonyTransporter(sys.colony(), popNeeded, maxPopToGive, minTransports);
    }
    public void sendTransports() {
        long tm0 = System.currentTimeMillis();
        int minTransportSize = 5;
        List<ColonyTransporter> needy = new ArrayList<>();
        List<ColonyTransporter> givey = new ArrayList<>();
        for (StarSystem sys: empire.allColonizedSystems()) {
            ColonyTransporter col = createColony(sys, minTransportSize);
            if (col != null) {
                if ((col.popNeeded >= minTransportSize) && (col.popNeeded >= col.maxPopToGive))
                    needy.add(col);
                else if ((col.maxPopToGive >= minTransportSize) && (col.maxPopToGive > col.popNeeded))
                    givey.add(col);
            }
        }

        if (needy.isEmpty() || givey.isEmpty()) {
            log("sendTransports (NONE): "+empire.raceName()+"   "+(System.currentTimeMillis()-tm0)+"ms");
            return;
        }

        Collections.sort(needy,TRANSPORT_PRIORITY);
        boolean transporting = true;

        while(transporting) {
            transporting = false;
            if (!needy.isEmpty()) {
                if (!givey.isEmpty()) {
                    ColonyTransporter needer = needy.get(0);
                    TARGET_COLONY = needer;
                    Collections.sort(givey,DISTANCE_TO_TARGET);
                    int j = 0;
                    while(!transporting && (j < givey.size())) {
                        ColonyTransporter giver = givey.get(j);
                        j++;
                        if ((giver.maxPopToGive >= minTransportSize) && (giver.transportPriority < needer.transportPriority)) {
                            float needed = needer.popNeeded - ((int) (Math.ceil(giver.transportTimeTo(needer))) * needer.growth);
                            int trPop = (int) min(needed, giver.maxPopToGive);
                            if (trPop >= minTransportSize) {
                                giver.sendTransportsTo(needer, trPop);
                                transporting = true;
                            }
                        }
                    }
                }
            }
        }
        long tm1 = System.currentTimeMillis();
        log("sendTransports: "+empire.raceName()+"   "+(tm1-tm0)+"ms");
    }
    public void checkColonize(StarSystem sys, ShipFleet fl) {
        if (fl.retreating())
            return;
        if (sys.orbitingShipsInConflict())
            return;

        if (empire.sv.isColonized(sys.id))
            return;
        if (!empire.canColonize(sys.planet()))
            return;

        ShipDesign bestDesign = shipDesigner().bestDesignToColonize(fl, sys);
        // if no useable colony design, exit
        if (bestDesign == null)
            return;

        // AT THIS POINT, the fleet can definitely colonize the planet
        // confirm if player controlled
        if (empire.isAIControlled())
            fl.colonizeSystem(sys, bestDesign);
        else
            ColonizeSystemNotification.create(sys.id, fl, bestDesign);
    }
    public boolean promptForBombardment(StarSystem sys, ShipFleet fl) {
        // if player, prompt for decision to bomb instead of deciding here
        if (empire.isPlayerControlled()) {
            BombardSystemNotification.create(id(sys), fl);
            return false;
        }
        
        // estimate bombardment damage and resulting population loss
        float damage = fl.expectedBombardDamage();
        float popLoss = damage / 200;
        float sysPop = empire.sv.population(id(sys));

        // if colony will NOT be destroyed, then bombs away!
        if (popLoss < (sysPop * .9))
            return true;

        Empire fleetEmp = fl.empire();
        // determine number of troops in transit
        int transports = fleetEmp.transportsInTransit(sys);

        // if none in transit, then bombs away!
        if (transports < 1)
            return true;

        // determine population troop loss in combat
        float killRatio = fleetEmp.troopKillRatio(sys);

        // if troops in transit CANNOT capture planet and will die anyway, then bombs away!
        if ((transports * killRatio) < sysPop)
            return true;

        // else don't bomb
        return false;
    }
    class ColonyTransporter implements IMappedObject {
        Colony colony;
        float x, y;
        float transportPriority;
        float growth;
        int popNeeded;
        int maxPopToGive;
        public ColonyTransporter(Colony c, int needs, int gives, int min) {
            colony = c;
            StarSystem sys = c.starSystem();
            x = sys.x();
            y = sys.y();
            popNeeded = needs;
            maxPopToGive = gives;

            // calc these values only for needy colonies
            if ((popNeeded >= min) && (popNeeded >= maxPopToGive)) {
                transportPriority = c.empire().fleetCommanderAI().transportPriority(sys);
                growth = c.normalPopGrowth();
            }
        }
        @Override
        public float x() { return x; }
        @Override
        public float y() { return y; }
        public float transportTimeTo(ColonyTransporter dest) {
            return colony.starSystem().transportTimeTo(dest.colony.starSystem());
        }
        public void sendTransportsTo(ColonyTransporter dest, int trPop) {
            colony.scheduleTransportsToSystem(dest.colony.starSystem(), trPop);
            maxPopToGive = 0;
            dest.popNeeded -= trPop;
        }
    }
    public static Comparator<ColonyTransporter> TRANSPORT_PRIORITY = (ColonyTransporter col1, ColonyTransporter col2) -> Base.compare(col1.transportPriority,col2.transportPriority);
    public static ColonyTransporter TARGET_COLONY;
    public static Comparator<ColonyTransporter> DISTANCE_TO_TARGET = new Comparator<ColonyTransporter>() {
        @Override
        public int compare(ColonyTransporter sys1, ColonyTransporter sys2) {
            float pr1 = sys1.distanceTo(TARGET_COLONY);
            float pr2 = sys2.distanceTo(TARGET_COLONY);
            return Base.compare(pr1, pr2);
        }
    };
}
