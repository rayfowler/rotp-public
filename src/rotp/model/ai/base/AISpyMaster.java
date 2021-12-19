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
package rotp.model.ai.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.Leader;
import rotp.model.empires.SpyNetwork;
import rotp.model.empires.SpyNetwork.Sabotage;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class AISpyMaster implements Base, SpyMaster {
    private final Empire empire;
    public AISpyMaster (Empire c) {
        empire = c;
    }
    @Override
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
            if ((cv != null) && cv.embassy().contact() && cv.inEconomicRange()) {
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
    @Override
    public void setSpyingAllocation(EmpireView v) {
        // invoked after nextTurn() processing is complete on each civ's turn
        // also invoked when contact is made in mid-turn
        // how much allocation for the spyNetwork?
        // each pt of allocatoin represents .005 of total civ production
        // max allocation is 25, or 10% of total civ production

        DiplomaticEmbassy emb = v.embassy();
        SpyNetwork spies = v.spies();

        // situations where no spies are ever needed
        if (!emb.contact() || v.empire().extinct() || !v.inEconomicRange() || emb.unity()) {
            spies.allocation(0);
            return;
        }

        // if we are not in war preparations and we've received threats 
        // about spying, then no spending
        boolean shouldHide = false;
        if (!v.embassy().anyWar() && (v.spies().maxSpies() > 0)
        && v.otherView().embassy().timerIsActive(DiplomaticEmbassy.TIMER_SPY_WARNING)) {
            if (!v.spies().isHide()
            || (v.empire().leader().isXenophobic())) {
                shouldHide = true;
            }
        }
        
        if (!emb.isEnemy() && shouldHide) {
            spies.allocation(0);
            return;
        }
            
        int maxSpiesNeeded = 0;

        if (emb.finalWar())
            maxSpiesNeeded = 3;
        else if (emb.war())
            maxSpiesNeeded = 2;
        else if (emb.noTreaty()) 
            maxSpiesNeeded = 1;
        else if (emb.pact())
            maxSpiesNeeded = 1;
        else if (emb.atPeace())
            maxSpiesNeeded = 1;
        else if (emb.alliance()) 
            maxSpiesNeeded = 1;

        if (spies.numActiveSpies() >= maxSpiesNeeded)
            spies.allocation(0);
        else
            spies.allocation(maxSpiesNeeded * 2);
    }
    @Override
    public void setSpyingMission(EmpireView v) {
        // invoked for each CivView for each civ after nextTurn() processing is complete on each civ's turn
        // also invoked when contact is made in mid-turn
        // 0 = hide; 1 = sabotage; 2 = espionage
        
        DiplomaticEmbassy emb = v.embassy();
        SpyNetwork spies = v.spies();

        // extinct or no contact = hide
        if (v.empire().extinct() || !emb.contact()) {
            spies.beginHide();
            return;
        }

        // they are our allies
        if (emb.alliance() || emb.unity()) {
            spies.beginHide();
            return;
        }

        // we've been warned and they are not our enemy (i.e. no war preparations)
        boolean shouldHide = false;
        if (!v.embassy().anyWar() && (v.spies().maxSpies() > 0)
        && v.otherView().embassy().timerIsActive(DiplomaticEmbassy.TIMER_SPY_WARNING)) {
            if (!v.spies().isHide()
            || (v.empire().leader().isXenophobic())) {
                shouldHide = true;
            }
        }
        
        if (!emb.isEnemy() && shouldHide) {
            spies.beginHide();
            return;
        }
 
        boolean canEspionage = !spies.possibleTechs().isEmpty();
        Leader leader = v.owner().leader();
        float relations = emb.relations();
        
        // we are in a pact or at peace
        if (emb.pact() || emb.atPeace()) {
            if (leader.isTechnologist() && canEspionage)
                spies.beginEspionage();
            else if (leader.isPacifist() || leader.isHonorable())
                spies.beginHide();
            else if ((relations > 30) && canEspionage)
                spies.beginEspionage();
            else
                spies.beginHide();
            return;
        }

        Sabotage sabMission = bestSabotageChoice(v);
        boolean canSabotage = spies.canSabotage() && (sabMission != null);
        
        // we have no treaty, we might want to sabotage!
        if (emb.noTreaty()) {
            if (leader.isAggressive() || leader.isRuthless() || leader.isErratic()) {
                if (canEspionage)
                    spies.beginEspionage();
                else if (canSabotage)
                    spies.beginSabotage();
                else
                    spies.beginHide();
            }
            else if (leader.isXenophobic() && canEspionage)
                spies.beginEspionage();
            else if (relations < -20) 
                spies.beginHide();
            else if (leader.isTechnologist() && canEspionage)
                spies.beginEspionage();
            else if (leader.isPacifist() || leader.isHonorable())
                spies.beginHide();
            else
                spies.beginHide();
            return;
        }
        
        // if at war, defer to war strategy: 1) steal war techs, 2) sabotage, 3) steal techs
        if (emb.anyWar()) {
            List<Tech> warTechs = new ArrayList<>();
            for (String tId: v.spies().possibleTechs()) {
                Tech t = tech(tId);
                if ((t.warModeFactor() > 1) && !t.isObsolete(v.owner()))
                    warTechs.add(t);
            }
            if (!warTechs.isEmpty())
                spies.beginEspionage();
            else if (canSabotage)
                spies.beginSabotage();
            else if (!v.spies().possibleTechs().isEmpty())
                spies.beginEspionage();
            else
                spies.beginHide();
            return;
        }
        
        // default for any other treaty state (??) is to hide
       spies.beginHide();
    }
    @Override
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
    @Override
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
    @Override
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
