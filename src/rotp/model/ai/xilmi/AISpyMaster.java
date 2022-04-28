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
package rotp.model.ai.xilmi;

import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.SpyMaster;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SpyNetwork;
import rotp.model.empires.SpyNetwork.Sabotage;
import rotp.model.galaxy.StarSystem;
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

        float paranoia = 0;
        float avgOpponentTechLevel = 0;
        float opponentCount = 0;
        for (EmpireView cv : empire.empireViews()) {
            if ((cv != null) && cv.embassy().contact() && cv.inEconomicRange()) {
                if(empire.allies().contains(cv.empire()))
                    continue;
                avgOpponentTechLevel += cv.empire().tech().maxTechLevel();
                opponentCount++;
            }
        }
        if(opponentCount > 0)
            avgOpponentTechLevel /= opponentCount;
        paranoia = empire.tech().avgTechLevel() - avgOpponentTechLevel;
        if(avgOpponentTechLevel == 0)
            paranoia = 0;
        if (paranoia < 0)
            paranoia = 0;
        //System.out.println(empire.galaxy().currentTurn()+" "+ empire.name()+" counter-espionage: "+paranoia+" mt: "+empire.tech().avgTechLevel()+" ot: "+avgOpponentTechLevel);
        return min(10, (int)Math.round(paranoia)); // modnar: change max to 10, MAX_SECURITY_TICKS = 10
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
        if (!emb.isEnemy() && spies.threatened()) {
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
            maxSpiesNeeded = 0;
        
        if (spies.numActiveSpies() >= spies.maxSpies())
            spies.allocation(0);
        else
        {
            maxSpiesNeeded *= 2;
            //ail: avoid inefficient overspening by adjusting to spy-costs
            float bcPerTick = empire.totalPlanetaryProduction() * empire.spySpendingModifier() / 200.0f;
            float maxTicksNeeded = spies.maxSpies() * empire.baseSpyCost() / bcPerTick;
            maxSpiesNeeded = min(maxSpiesNeeded, (int)Math.ceil(maxTicksNeeded));
            spies.allocation(maxSpiesNeeded);
        }
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
            spies.maxSpies(0);
            return;
        }

        // they are our allies
        if (emb.alliance() || emb.unity()) {
            spies.beginHide();
            spies.maxSpies(0);
            return;
        }

        // we've been warned and they are not our enemy (i.e. no war preparations)
        boolean shouldHide = false;
        if (!v.embassy().anyWar() && (v.spies().maxSpies() > 0)
        && v.otherView().embassy().timerIsActive(DiplomaticEmbassy.TIMER_SPY_WARNING)) {
            //System.out.println(empire.galaxy().currentTurn()+" "+ empire.name()+" we have been recently warned to stop spying by "+v.embassy().empire()+" "+v.otherView().embassy().timers[DiplomaticEmbassy.TIMER_SPY_WARNING]);
            if (!v.spies().isHide()
            || (v.empire().leader().isXenophobic())) {
                shouldHide = true;
            }
        }
        
        //When I'm ruthless or they can't reach me anyways, no reason to listen to their threats
        if(empire.leader().isRuthless() || !v.empire().inShipRange(empire.id))
            shouldHide = false;
        
        //check if they are in trouble. If they are, we don't care about their threat
        if(shouldHide)
        {
            if(empire.diplomatAI().readyForWar(v, false))
                shouldHide = false;
        }
        
        if (!emb.isEnemy() && shouldHide) {
            if(v.empire().leader().isXenophobic())
            {
                spies.shutdownSpyNetworks();
                return;
            }
            spies.beginHide();
            spies.maxSpies(1);
            return;
        }
        
        boolean canEspionage = !spies.possibleTechs().isEmpty();
        Sabotage sabMission = bestSabotageChoice(v);
        boolean canSabotage = spies.canSabotage() && (sabMission != null);
        
        // we are in a pact or at peace
        // ail: according to official strategy-guide two spies is supposedly the ideal number for tech-stealing etc, so always setting it to two except for hiding
        // let's see what happens, if we just non-chalantly spy on everyone regardless of anything considering they won't declare war unless they would do so anyways
        if (emb.pact() || emb.atPeace() || emb.noTreaty()) {
            if(canEspionage)
            {
                spies.beginEspionage();
                spies.maxSpies(2);
            }
            else
            {
                spies.beginHide();
                spies.maxSpies(1);
            }
            return;
        }
        if (emb.anyWar()) {
            if (canEspionage)
            {
                spies.beginEspionage();
                spies.maxSpies(2);
            }
            else if (canSabotage)
            {
                spies.beginSabotage();
                spies.maxSpies(2);
            }
            else
            {
                spies.beginHide();
                spies.maxSpies(1);
            }
            return;
        }
        
        // default for any other treaty state (??) is to hide
       spies.beginHide();
    }
    @Override
    public Sabotage bestSabotageChoice(EmpireView v) {
        // invoked when a Sabotage attempt is successful
        // unfinished - AI needs to choose best sabotage type
        if (!v.spies().rebellionTargets().isEmpty())
            return Sabotage.REBELS;
        else if (!v.spies().baseTargets().isEmpty())
            return Sabotage.MISSILES;
        else if (!v.spies().factoryTargets().isEmpty())
            return Sabotage.FACTORIES;
        else
            return null;
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
        if (v1.empire().powerLevel(v1.empire()) > v2.empire().powerLevel(v2.empire()))
            return e1;
        else
            return e2;
    }
}
