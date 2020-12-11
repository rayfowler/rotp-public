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
package rotp.model.ai.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SpyNetwork.Sabotage;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class AICSpyMaster implements Base, SpyMaster {
    private final Empire empire;
    public AICSpyMaster (Empire c) {
        empire = c;
    }
    public int suggestedInternalSecurityLevel() {
        // invoked after nextTurn() processing is complete on each civ's turn
        // also invoked when contact is made in mid-turn
        // return from 0 to 40, which translates to 0% to 20% of total prod
		//
		// modnar: is it not 0% to 10% of total prod, for a max of +20% security bonus, with 0 to 10 ticks/clicks?
		// MAX_SECURITY_TICKS = 10 in model/empires/Empire.java

        int paranoia = 0;
        boolean alone = true;
        for (EmpireView cv : empire.empireViews()) {
            if ((cv != null) && cv.embassy().contact()) {
                alone = false;
                if (cv.embassy().anyWar())
                    paranoia += 3; // modnar: more internal security paranoia
                if (cv.embassy().noTreaty())
                    paranoia += 2; // modnar: more internal security paranoia
            }
        }
        if ((paranoia == 0) && !alone)
            paranoia++;
        if (empire.leader().isXenophobic())
            paranoia *= 2;
        return min(10, paranoia); // modnar: change max to 10, MAX_SECURITY_TICKS = 10
    }
    public void setSpyingAllocation(EmpireView v) {
        // invoked after nextTurn() processing is complete on each civ's turn
        // also invoked when contact is made in mid-turn
        // how much allocation for the spyNetwork?
        // each pt of allocatoin represents .005 of total civ production
        // max allocation is 25, or 10% of total civ production
        if (!v.embassy().contact() || v.empire().extinct()) {
            v.spies().allocation(0);
            return;
        }

        int maxSpiesNeeded;

        if (v.embassy().finalWar())
			// modnar: reduce spies needed for large number of active empires
            maxSpiesNeeded = galaxy().numActiveEmpires() > 20 ? 2 : 3;
        else if (v.embassy().war())
			// modnar: reduce spies needed for large number of active empires
            maxSpiesNeeded = galaxy().numActiveEmpires() > 20 ? 1 : 2;
        else if (v.embassy().noTreaty()) {
			// modnar: check if empire is in range or not
            if (v.empire().inEconomicRange(id(empire)))
				maxSpiesNeeded = 1;
			else
				maxSpiesNeeded = 0; // modnar: no spies if not in range
		}
        else if (v.embassy().pact())
			// modnar: reduce spies needed for large number of active empires
            maxSpiesNeeded = galaxy().numActiveEmpires() > 20 ? 0 : 1;
        else   // unity() or alliance()
            maxSpiesNeeded = 0;

        // modnar: reduce allocation to 1 tick per spy needed, better for larger games with more empires
		// 0.5% (1 tick) spending for each spy needed
        if (v.spies().numActiveSpies() >= maxSpiesNeeded)
            v.spies().allocation(0);
        else
            v.spies().allocation(maxSpiesNeeded * 1);
    }
    public void setSpyingMission(EmpireView v) {
        // invoked for each CivView for each civ after nextTurn() processing is complete on each civ's turn
        // also invoked when contact is made in mid-turn
        // 0 = hide; 1 = sabotage; 2 = espionage

        Sabotage sabMission = bestSabotageChoice(v);
        boolean canSabotage = v.spies().canSabotage() && (sabMission != null);

        float relations = v.embassy().relations();
        if (v.empire().extinct())
            v.spies().beginHide();
        else if (!v.embassy().contact())
            v.spies().beginHide();
        else if (v.embassy().alliance() || v.embassy().unity())
            v.spies().beginHide();
        else if (v.embassy().pact()) {
            if ((relations > 0) && !v.spies().possibleTechs().isEmpty())
                v.spies().beginEspionage();
            else
                v.spies().beginHide();
        }
         else if (v.embassy().noTreaty()) {
            if (relations < 0)
                v.spies().beginHide();
            else if (v.spies().possibleTechs().isEmpty() && canSabotage) 
                    v.spies().beginSabotage();
            else
                v.spies().beginEspionage();
        }
         // if at war, defer to war strategy: 1) steal war techs, 2) sabotage, 3) steal techs
        else if (v.embassy().anyWar()) {
            List<Tech> warTechs = new ArrayList<>();
            for (Tech t: v.spies().possibleTechs()) {
                if ((t.warModeFactor() > 1) && !t.isObsolete(v.owner()))
                    warTechs.add(t);
            }
            if (!warTechs.isEmpty())
                v.spies().beginEspionage();
            else if (canSabotage)
                v.spies().beginSabotage();
            else if (!v.spies().possibleTechs().isEmpty())
                v.spies().beginEspionage();
            else
                v.spies().beginHide();
        }
        else
            // default is to hide
            v.spies().beginHide();
    }
    public Sabotage bestSabotageChoice(EmpireView v) {
        // invoked when a Sabotage attempt is successful
        // unfinished - AI needs to choose best sabotage type

        if (v.embassy().anyWar()) {
            if (!v.spies().baseTargets().isEmpty())
                return Sabotage.MISSILES;
            else if (!v.spies().factoryTargets().isEmpty())
                return Sabotage.FACTORIES;
            else if (!v.spies().rebellionTargets().isEmpty())
                return Sabotage.REBELS;
            else
                return null;
        }
        else {
            if (!v.spies().rebellionTargets().isEmpty())
                return Sabotage.REBELS;
            else if (!v.spies().factoryTargets().isEmpty())
                return Sabotage.FACTORIES;
            else if (!v.spies().baseTargets().isEmpty())
                return Sabotage.MISSILES;
            else
                return null;
        }
    }
    public StarSystem bestSystemForSabotage(EmpireView v, Sabotage choice) {
        // invoked when a Sabotage attempt is successful
        // choice: 1 - factories, 2 - missiles, 3 - rebellion

        List<StarSystem> targets = v.empire().allColonizedSystems();

        switch(choice){
            case FACTORIES:
                StarSystem.VIEWING_EMPIRE = empire;
                Collections.sort(targets, StarSystem.VDISTANCE);
                for (StarSystem tgt: targets) {
                    if (empire.sv.canSabotageFactories(tgt.id))
                        return tgt;
                }
                return null;
            case MISSILES:
                StarSystem.VIEWING_EMPIRE = empire;
                Collections.sort(targets, StarSystem.VDISTANCE);
                for (StarSystem tgt: targets) {
                    if (empire.sv.canSabotageBases(tgt.id))
                        return tgt;
                }
                return null;
            case REBELS:
            default:
                StarSystem.VIEWING_EMPIRE = empire;
                Collections.sort(targets, StarSystem.VPOPULATION);
                for (int i=(targets.size()-1);i>=0;i--) {
                    StarSystem tgt = targets.get(i);
                    if (empire.sv.canInciteRebellion(tgt.id))
                        return tgt;
                }
                return null;
        }
    }
    public Empire suggestToFrame(List<Empire> empires) {
        if (empires.size() < 2)
            return null;
        Empire e1 = empires.get(0);
        Empire e2 = empires.get(1);
        EmpireView v1 = empire.viewForEmpire(e1);
        EmpireView v2 = empire.viewForEmpire(e2);

        // throw enemies under the bus first
        if (v1.embassy().anyWar() && !v2.embassy().anyWar())
            return e1;
        if (!v1.embassy().anyWar() && v2.embassy().anyWar())
            return e2;

        // throw allies under the bus last
        if (v1.embassy().alliance() && !v2.embassy().alliance())
            return e2;
        if (!v1.embassy().alliance() && v2.embassy().alliance())
            return e1;

        // throw the stronger guy under the bus
        if (v1.empirePower() > v2.empirePower())
            return e1;
        else
            return e2;
    }
}
