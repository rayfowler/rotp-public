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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.Scientist;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.Tech;
import rotp.model.tech.TechArmor;
import rotp.model.tech.TechAtmosphereEnrichment;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechBattleComputer;
import rotp.model.tech.TechBattleSuit;
import rotp.model.tech.TechBeamFocus;
import rotp.model.tech.TechBiologicalAntidote;
import rotp.model.tech.TechBiologicalWeapon;
import rotp.model.tech.TechBlackHole;
import rotp.model.tech.TechBombWeapon;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechCloning;
import rotp.model.tech.TechCombatTransporter;
import rotp.model.tech.TechControlEnvironment;
import rotp.model.tech.TechDeflectorShield;
import rotp.model.tech.TechDisplacement;
import rotp.model.tech.TechECMJammer;
import rotp.model.tech.TechEcoRestoration;
import rotp.model.tech.TechEnergyPulsar;
import rotp.model.tech.TechEngineWarp;
import rotp.model.tech.TechFuelRange;
import rotp.model.tech.TechFutureComputer;
import rotp.model.tech.TechFutureConstruction;
import rotp.model.tech.TechFutureForceField;
import rotp.model.tech.TechFuturePlanetology;
import rotp.model.tech.TechFuturePropulsion;
import rotp.model.tech.TechFutureWeapon;
import rotp.model.tech.TechHandWeapon;
import rotp.model.tech.TechHyperspaceComm;
import rotp.model.tech.TechImprovedIndustrial;
import rotp.model.tech.TechImprovedTerraforming;
import rotp.model.tech.TechIndustrialWaste;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechPersonalShield;
import rotp.model.tech.TechPlanetaryShield;
import rotp.model.tech.TechRepulsor;
import rotp.model.tech.TechRoboticControls;
import rotp.model.tech.TechScanner;
import rotp.model.tech.TechShipInertial;
import rotp.model.tech.TechShipNullifier;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechSoilEnrichment;
import rotp.model.tech.TechStargate;
import rotp.model.tech.TechStasisField;
import rotp.model.tech.TechStreamProjector;
import rotp.model.tech.TechSubspaceInterdictor;
import rotp.model.tech.TechTeleporter;
import rotp.model.tech.TechTorpedoWeapon;
import rotp.model.tech.TechTree;
import rotp.ui.UserPreferences;
import rotp.ui.notifications.SelectTechNotification;
import rotp.util.Base;

public class AIScientist implements Base, Scientist {
    private static final float NEW_QUINTILE_BONUS = 0.10f;
    private final Empire empire;

    public AIScientist (Empire c) {
        empire = c;
    }
//-----------------------------------
// PUBLIC INTERFACE
//-----------------------------------
    @Override
    public Tech mostDesirableTech(List<Tech> techs) {
        Tech.comparatorCiv = empire;
        Collections.sort(techs, Tech.RESEARCH_VALUE);
        return techs.get(0);
    }
    @Override
    public void setTechTreeAllocations() {
        // invoked after nextTurn() processing is complete on each civ's turn
        setDefaultTechTreeAllocations();
        //ail: This happens at the beginning before we see whether we want to switch this off. But we want accidental excess to go to research so to not waste it.
        if(!empire.divertColonyExcessToResearch() && !empire.tech().researchCompleted())
            empire.toggleColonyExcessToResearch();
        if(empire.divertColonyExcessToResearch() && empire.tech().researchCompleted())
            empire.toggleColonyExcessToResearch();
        //ail: first I stop researching where there's no techs left
        int leftOverAlloc = 0;
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            //System.out.print("\n"+empire.name()+" "+empire.tech().category(j).id()+" alloc before adjust: "+empire.tech().category(j).allocation());
            if (empire.tech().category(j).possibleTechs().isEmpty())
            {
                leftOverAlloc+=empire.tech().category(j).allocation();
                empire.tech().category(j).allocation(0);
            }
        }
        if(leftOverAlloc >= 60)
        {
            setDefaultTechTreeAllocations();
            return;
        }
        while(leftOverAlloc > 0)
        {
            for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                if (!empire.tech().category(j).possibleTechs().isEmpty())
                {
                    empire.tech().category(j).adjustAllocation(1);
                    leftOverAlloc--;
                }
                if(leftOverAlloc <= 0)
                    break;
            }
        }
        //second I stop researching techs with too high of a discovery-chance
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            //System.out.print("\n"+empire.name()+" "+empire.tech().category(j).id()+" "+discoveryChanceOfCategoryIfAllocationWasZero(j)+" > "+empire.tech().category(j).allocation());
            Tech currentTechResearching = empire.tech().category(j).tech(empire.tech().category(j).currentTech());
            boolean researchingSomethingWeDontReallyWant = false;
            if(currentTechResearching != null)
            {
                if(researchPriority(currentTechResearching) == 0)
                {
                    researchingSomethingWeDontReallyWant = true;
                    //System.out.print("\n"+empire.name()+" "+empire.tech().category(j).id()+" reduced because "+currentTechResearching.name()+" is either owned by someone else or not something we want.");
                }
            }
            if (discoveryChanceOfCategoryIfAllocationWasZero(j) > empire.tech().category(j).allocation() || researchingSomethingWeDontReallyWant)
            {
                leftOverAlloc+=empire.tech().category(j).allocation();
                empire.tech().category(j).allocation(0);
            }
        }
        while(leftOverAlloc > 0)
        {
            boolean couldSpend = false;
            for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                Tech currentTechResearching = empire.tech().category(j).tech(empire.tech().category(j).currentTech());
                boolean researchingSomethingWeDontReallyWant = false;
                if(currentTechResearching != null)
                    if(researchPriority(currentTechResearching) == 0)
                        researchingSomethingWeDontReallyWant = true;
                if (!empire.tech().category(j).possibleTechs().isEmpty()
                        && discoveryChanceOfCategoryIfAllocationWasZero(j) <= empire.tech().category(j).allocation()
                        && !researchingSomethingWeDontReallyWant)
                {
                    empire.tech().category(j).adjustAllocation(1);
                    leftOverAlloc--;
                    couldSpend = true;
                }
                if(leftOverAlloc <= 0)
                    break;
            }
            if(!couldSpend)
            {
                for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                    if (!empire.tech().category(j).possibleTechs().isEmpty())
                    {
                        empire.tech().category(j).adjustAllocation(1);
                        leftOverAlloc--;
                    }
                    if(leftOverAlloc <= 0)
                        break;
                }
            }
        }
        //and lastly i stop researching future techs when there's still others
        int futureTechs = 0;
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            if (empire.tech().category(j).studyingFutureTech())
                futureTechs++;
        }
        if(futureTechs == 6)
            return;
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            if (empire.tech().category(j).studyingFutureTech())
            {
                leftOverAlloc+=empire.tech().category(j).allocation();
                empire.tech().category(j).allocation(0);
            }
        }
        while(leftOverAlloc > 0)
        {
            boolean couldSpend = false;
            for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                if (!empire.tech().category(j).possibleTechs().isEmpty()
                        && !empire.tech().category(j).studyingFutureTech()
                        && discoveryChanceOfCategoryIfAllocationWasZero(j) <= empire.tech().category(j).allocation())
                {
                    empire.tech().category(j).adjustAllocation(1);
                    leftOverAlloc--;
                    couldSpend = true;
                }
                if(leftOverAlloc <= 0)
                    break;
            }
            if(!couldSpend)
            {
                for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                    if (!empire.tech().category(j).possibleTechs().isEmpty()
                            && discoveryChanceOfCategoryIfAllocationWasZero(j) <= empire.tech().category(j).allocation())
                    {
                        empire.tech().category(j).adjustAllocation(1);
                        leftOverAlloc--;
                        couldSpend = true;
                    }
                    if(leftOverAlloc <= 0)
                        break;
                }
            }
            if(!couldSpend)
            {
                for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
                    if (!empire.tech().category(j).possibleTechs().isEmpty())
                    {
                        empire.tech().category(j).adjustAllocation(1);
                        leftOverAlloc--;
                    }
                    if(leftOverAlloc <= 0)
                        break;
                }
            }
        }
        /*for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+empire.tech().category(j).key()+": "+empire.tech().category(j).currentTechName()+": "+empire.tech().category(j).allocationPct()+" of "+empire.totalPlanetaryResearch());
        }*/
    }
    @Override
    public void setDefaultTechTreeAllocations() {
        // invoked directly when the TechTree is first created
        if (empire.isPlayerControlled()) {
            empire.tech().computer().allocation(10);
            empire.tech().construction().allocation(10);
            empire.tech().forceField().allocation(10);
            empire.tech().planetology().allocation(10);
            empire.tech().propulsion().allocation(10);
            empire.tech().weapon().allocation(10);
            return;
        }
        
        if (empire.tech().topFuelRangeTech().range() < 4) {
            empire.tech().computer().allocation(0);
            empire.tech().construction().allocation(0);
            empire.tech().forceField().allocation(0);
            empire.tech().planetology().allocation(0);
            empire.tech().propulsion().allocation(60);
            empire.tech().weapon().allocation(0);
            return;
        }

        float totalTechCost = 0;
        
        for(int i = 0; i < 6; ++i)
            if(tech(empire.tech().category(i).currentTech()) != null)
                totalTechCost += tech(empire.tech().category(i).currentTech()).researchCost();
            else
                totalTechCost += empire.tech().category(i).baseResearchCost(Math.round(empire.tech().category(i).techLevel()));
        
        float totalInverse = 0;
        
        for(int i = 0; i < 6; ++i)
            if(tech(empire.tech().category(i).currentTech()) != null)
                totalInverse += totalTechCost / tech(empire.tech().category(i).currentTech()).researchCost();
            else
                totalInverse += totalTechCost / empire.tech().category(i).baseResearchCost(Math.round(empire.tech().category(i).techLevel()));
        
        for(int i = 0; i < 6; ++i)
            if(tech(empire.tech().category(i).currentTech()) != null)
                empire.tech().category(i).allocationPct((totalTechCost / tech(empire.tech().category(i).currentTech()).researchCost()) / totalInverse);
            else
                empire.tech().category(i).allocationPct((totalTechCost / empire.tech().category(i).baseResearchCost(Math.round(empire.tech().category(i).techLevel()))) / totalInverse);
        
        if (empire.fleetCommanderAI().inExpansionMode()) {
            if(empire.ignoresPlanetEnvironment())
            {
                empire.tech().computer().adjustAllocation(-3);
                empire.tech().construction().adjustAllocation(-3);
                empire.tech().forceField().adjustAllocation(-9);
                empire.tech().planetology().adjustAllocation(-3);
                empire.tech().propulsion().adjustAllocation(27);
                empire.tech().weapon().adjustAllocation(-9);
            }
            else
            {
                empire.tech().computer().adjustAllocation(-3);
                empire.tech().construction().adjustAllocation(-3);
                empire.tech().forceField().adjustAllocation(-9);
                empire.tech().planetology().adjustAllocation(6);
                empire.tech().propulsion().adjustAllocation(18);
                empire.tech().weapon().adjustAllocation(-9);
            }
        }
        
        int futureTechs = 0;
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            if (empire.tech().category(j).studyingFutureTech()
                    || empire.tech().category(j).possibleTechs().isEmpty())
                futureTechs++;
        }
        //When we are researching future-techs, weapons and propulsion are much more valuable than the rest
        if(futureTechs == 6)
        {
            empire.tech().computer().adjustAllocation(-5);
            empire.tech().construction().adjustAllocation(-5);
            empire.tech().forceField().adjustAllocation(-5);
            empire.tech().planetology().adjustAllocation(-5);
            empire.tech().propulsion().adjustAllocation(+10);
            empire.tech().weapon().adjustAllocation(+10);
        }
        
        int totalAlloc = empire.tech().computer().allocation() + empire.tech().construction().allocation() 
        + empire.tech().forceField().allocation() + empire.tech().planetology().allocation() 
        + empire.tech().propulsion().allocation() + empire.tech().weapon().allocation();
        
        int roundingFix = 60 - totalAlloc;
        while(roundingFix < 0)
        {
            if(empire.tech().computer().allocation() > 0)
            {
                empire.tech().computer().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
            if(empire.tech().construction().allocation() > 0)
            {
                empire.tech().construction().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
            if(empire.tech().forceField().allocation() > 0)
            {
                empire.tech().forceField().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
            if(empire.tech().planetology().allocation() > 0)
            {
                empire.tech().planetology().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
            if(empire.tech().propulsion().allocation() > 0)
            {
                empire.tech().propulsion().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
            if(empire.tech().weapon().allocation() > 0)
            {            empire.tech().weapon().adjustAllocation(-1);
                roundingFix++;
                if(roundingFix >= 0)
                    break;
            }
        }
        
        while(roundingFix > 0)
        {
            empire.tech().computer().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
            empire.tech().construction().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
            empire.tech().forceField().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
            empire.tech().planetology().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
            empire.tech().propulsion().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
            empire.tech().weapon().adjustAllocation(1);
            roundingFix--;
            if(roundingFix <= 0)
                break;
        }  
    }
    @Override
    public void setTechToResearch(TechCategory cat) {
        // invoked for AI after a tech is learned
        // also invoked for AI & Player when Research BC are allocated during nextTurn() and no
        // Tech has yet been chosen to research

        List<Tech> techs = cat.techsAvailableForResearch();

        // no more techs to research in this category
        if (techs.isEmpty())
            return;

        if (empire.isPlayerControlled() ) {
            Tech firstTech = techs.get(0);
            // we stop asking for user selection once we finished Future Tech 1
            if (firstTech.futureTechLevel() < 2) {
                session().addTurnNotification(new SelectTechNotification(cat));
                return;
            }
        }

        Tech.comparatorCiv = empire;
        Collections.sort(techs, Tech.RESEARCH_PRIORITY);

        // return highest priority
        cat.currentTech(techs.get(0));
        /*for(Tech t : techs)
        {
            System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+cat.id()+" option: "+t.name()+" "+researchPriority(t));
        }
        System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+cat.id()+" picked: "+cat.currentTechName()+" "+researchPriority(techs.get(0)));*/
    }
    //
    //  RESEARCH VALUES for various types of tech
    //
    @Override
    public float researchPriority(Tech t) {
        float ownerFactor = 1.0f;
        for(EmpireView ev : empire.contacts())
        {
            if(!ev.inEconomicRange())
                continue;
            if(!galaxy().options().canTradeTechs(empire, ev.empire()))
                continue;
            if(ev.empire().race().internalSecurityAdj() > empire.race().spyInfiltrationAdj())
                continue;
            if(t.isType(Tech.ENGINE_WARP))
                continue;
            //If others, who we are not at war with, have it, we value it lower because in that case we can try and trade for it
            if(!empire.atWarWith(ev.empId()) && ev.spies().unknownTechs().contains(t))
                ownerFactor /= 2;
            //If we could steal it we don't want to research it ourselves at all
            if(ev.spies().possibleTechs().contains(t.id()) && ev.spies().isEspionage() && ev.spies().hasSpies())
                return 0;
        }
        return max(researchValue(t) * ownerFactor, 1);
    }
    @Override
    public float researchValue(Tech t) {
        //ail: for something that has 0 base-value, we also don't add random
        if (t.isObsolete(empire))
            return 0;
        return t.baseValue(empire);
    }
    @Override
    public float researchBCValue(Tech t) {
        if (t.isObsolete(empire))
            return 0;

        if (empire.generalAI().inWarMode())
            return warTradeBCValue(t);

        if (empire.fleetCommanderAI().inExpansionMode())
            return t.expansionModeFactor() * t.researchCost();

        return t.researchCost();
    }
    @Override
    public float warTradeValue(Tech t) {
        if (t.isObsolete(empire))
            return 0;
        return t.warModeFactor() * (researchValueBonus(t) + t.baseValue(empire));
    }
    @Override
    public float warTradeBCValue(Tech t) {
        return t.warModeFactor() * t.researchCost(); 
    }
    private float researchValueBonus(Tech t) {
        TechCategory cat = empire.tech().category(t.cat.index());
        // if we have not researched a tech in this quintile yet
        // and we are not researching a tech in this quintile,
        // then the perceived value is 10% of the tech level
        Tech currentTech = tech(cat.currentTech());
        if ((cat.maxKnownQuintile() < t.quintile())
        && (currentTech != null)
        && (currentTech.quintile() < t.quintile()))
            return t.level * NEW_QUINTILE_BONUS;
        else
            return 0;
    }
    @Override
    public float baseValue(TechArmor t) {
        TechArmor curr = empire.tech().topArmorTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= sqrt(curr.level());
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechAtmosphereEnrichment t) {
        if (empire.tech().canTerraformHostile())
            return 0;
        // colonized systems that can be improved
        List<StarSystem> possible = new ArrayList<>();
        for (StarSystem colony : empire.allColonizedSystems()) {
            if (empire.isEnvironmentHostile(colony))
                possible.add(colony);
        }
        if (possible.isEmpty())
            return 0;
        TechAtmosphereEnrichment curr = empire.tech().topAtmoEnrichmentTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= sqrt(curr.level());
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechAutomatedRepair t) {
        return 1;
    }
    @Override
    public float baseValue(TechBattleComputer t) {
        TechBattleComputer curr = empire.tech().topBattleComputerTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechBattleSuit t) {
        TechBattleSuit curr = empire.tech().topBattleSuitTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechBeamFocus t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechBiologicalAntidote t) {
        return 1;
    }
    @Override
    public float baseValue(TechBiologicalWeapon t) {
        return 1;
    }
    @Override
    public float baseValue(TechBlackHole t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechBombWeapon t) {
        TechBombWeapon curr = empire.tech().topBombWeaponTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += (t.quintile() - 1) * 5 + t.quintile() * 5 - t.level() + 1;
        return val;
    }
    @Override
    public float baseValue(TechCloaking t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechCloning t) {
        return t.level();
    }
    @Override
    public float baseValue(TechCombatTransporter t) {
        return t.level();
    }
    @Override
    public float baseValue(TechControlEnvironment t) {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        List<StarSystem> possible = empire.uncolonizedPlanetsInRange(empire.shipRange());
        List<StarSystem> newPossible = empire.uncolonizedPlanetsInShipRange(t.environment());
        float newPlanets = newPossible.size() - possible.size();
        if (newPlanets < 1)
            return 0;
        TechControlEnvironment curr = empire.tech().topControlEnvironmentTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= sqrt(curr.level());
        val += t.level();
        if(empire.fleetCommanderAI().inExpansionMode())
        {
            val *= 2;
        }
        return val;
    }
    @Override
    public float baseValue(TechDeflectorShield t) {
        TechDeflectorShield curr = empire.tech().topDeflectorShieldTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechDisplacement t) {
        return 1;
    }
    @Override
    public float baseValue(TechECMJammer t) {
        return 1;
    }
    @Override
    public float baseValue(TechEcoRestoration t) {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        TechEcoRestoration curr = empire.tech().topEcoRestorationTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechEngineWarp t) {
        return t.level();
    }
    @Override
    public float baseValue(TechEnergyPulsar t) {
        return 1;
    }
    @Override
    public float baseValue(TechFuelRange t) {
        TechFuelRange curr = empire.tech().topFuelRangeTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best && !empire.fleetCommanderAI().inExpansionMode())
            val -= curr.level();
        val += t.level();
        if(empire.fleetCommanderAI().inExpansionMode())
        {
            val *= 2;
        }
        return val;
    }
    @Override
    public float baseValue(TechFutureComputer t) {
        return 1;
    }
    @Override
    public float baseValue(TechFutureConstruction t) {
        return 1;
    }
    @Override
    public float baseValue(TechFutureForceField t) {
        return 1;
    }
    @Override
    public float baseValue(TechFuturePlanetology t) {
        return 1;
    }
    @Override
    public float baseValue(TechFuturePropulsion t) {
        return 1;
    }
    @Override
    public float baseValue(TechFutureWeapon t) {
        return 1;
    }
    @Override
    public float baseValue(TechHandWeapon t) {
        TechHandWeapon curr = empire.tech().topHandWeaponTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += (t.quintile() - 1) * 5 + t.quintile() * 5 - t.level() + 1;
        return val;
    }
    @Override
    public float baseValue(TechHyperspaceComm t) {
        return t.level();
    }
    @Override
    public float baseValue(TechImprovedIndustrial t) {
        TechImprovedIndustrial curr = empire.tech().topImprovedIndustrialTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechImprovedTerraforming t) {
        TechImprovedTerraforming curr = empire.tech().topTerraformingTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechIndustrialWaste t) {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        TechIndustrialWaste curr = empire.tech().topIndustrialWasteTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechMissileShield t) {
        return 1;
    }
    @Override
    public float baseValue(TechMissileWeapon t) {
        TechMissileWeapon curr = empire.tech().topBaseMissileTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += (t.quintile() - 1) * 5 + t.quintile() * 5 - t.level() + 1;
        val /= 2.0f;
        return val;
    }
    @Override
    public float baseValue(TechPersonalShield t) {
        TechPersonalShield curr = empire.tech().topPersonalShieldTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechPlanetaryShield t) {
        TechPlanetaryShield curr = empire.tech().topPlanetaryShieldTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= curr.level();
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechRepulsor t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechRoboticControls t) {
        TechRoboticControls curr = empire.tech().topRoboticControlsTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= sqrt(curr.level());
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechScanner t) {
        return 2; //more important than ECM for proper attack-fleet-sizing and avoiding having to retreat
    }
    @Override
    public float baseValue(TechShipInertial t) {
        return 1;
    }
    @Override
    public float baseValue(TechShipNullifier t) {
        return 1;
    }
    @Override
    public float baseValue(TechShipWeapon t) {
        TechShipWeapon curr = empire.tech().topShipWeaponTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best && !t.heavyAllowed)
            val -= curr.level();
        val += (t.quintile() - 1) * 5 + t.quintile() * 5 - t.level() + 1;
        return val;
    }
    @Override
    public float baseValue(TechSoilEnrichment t) {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        TechSoilEnrichment curr = empire.tech().topSoilEnrichmentTech();
        float val = 0;
        Tech best = empire.tech().allTechsOfType(t.techType).get(empire.tech().allTechsOfType(t.techType).size()-1);
        if(curr != null && t != best)
            val -= sqrt(curr.level());
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechStargate t) {
        List<StarSystem> allColonies = empire.allColonizedSystems();
        //Ai hasn't yet learned how to properly use these, too expensive to build everywhere and otherwise would need path-finding
        return 0;
    }
    @Override
    public float baseValue(TechStasisField t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechStreamProjector t) {
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechSubspaceInterdictor t) {
        boolean anyEnemiesHaveTeleporter = false;
        for (EmpireView v: empire.empireViews()) {
            if ((v != null) && v.embassy().anyWar()) {
                if (v.spies().tech().knowsTechOfType(Tech.TELEPORTER))
                        anyEnemiesHaveTeleporter = true;
                if (v.spies().tech().knowsTechOfType(Tech.COMBAT_TRANSPORTER))
                        anyEnemiesHaveTeleporter = true;
            }
        }
        if(!anyEnemiesHaveTeleporter)
            return 0;
        float val = 0;
        val += t.level();
        return val;
    }
    @Override
    public float baseValue(TechTeleporter t) {
        boolean allEnemiesHaveInterdiction = true;
        for (EmpireView v: empire.empireViews()) {
            if ((v != null) && v.embassy().anyWar()) {
                if (v.spies().tech().knowsTechOfType(Tech.SUBSPACE_INTERDICTOR))
                        allEnemiesHaveInterdiction = false;
            }
        }
        if (allEnemiesHaveInterdiction)
            return 0;
        float val = 0;
        val += t.level();
        val /= 2;
        return val;
    }
    @Override
    public float baseValue(TechTorpedoWeapon t) {
        return 1;
    }
    private float discoveryChanceOfCategoryIfAllocationWasZero(int category)
    {
        int allocationBefore = empire.tech().category(category).allocation();
        empire.tech().category(category).allocation(0);
        float chance = (empire.tech().category(category).upcomingDiscoveryChance() - 1) * 60;
        empire.tech().category(category).allocation(allocationBefore);
        return chance;
    }
}
