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
import rotp.model.ai.interfaces.Scientist;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.Tech;
import rotp.model.tech.TechArmor;
import rotp.model.tech.TechAtmosphereEnrichment;
import rotp.model.tech.TechBattleComputer;
import rotp.model.tech.TechBiologicalAntidote;
import rotp.model.tech.TechBiologicalWeapon;
import rotp.model.tech.TechBlackHole;
import rotp.model.tech.TechBombWeapon;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechCloning;
import rotp.model.tech.TechCombatTransporter;
import rotp.model.tech.TechControlEnvironment;
import rotp.model.tech.TechDeflectorShield;
import rotp.model.tech.TechECMJammer;
import rotp.model.tech.TechEcoRestoration;
import rotp.model.tech.TechEngineWarp;
import rotp.model.tech.TechFuelRange;
import rotp.model.tech.TechHandWeapon;
import rotp.model.tech.TechImprovedIndustrial;
import rotp.model.tech.TechImprovedTerraforming;
import rotp.model.tech.TechIndustrialWaste;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechPersonalShield;
import rotp.model.tech.TechPlanetaryShield;
import rotp.model.tech.TechRoboticControls;
import rotp.model.tech.TechShipInertial;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechSoilEnrichment;
import rotp.model.tech.TechStargate;
import rotp.model.tech.TechSubspaceInterdictor;
import rotp.model.tech.TechTeleporter;
import rotp.model.tech.TechTree;
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
        int futureTechs = 0;
        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            if (empire.tech().category(j).studyingFutureTech())
                futureTechs++;
        }
        if ((futureTechs == TechTree.NUM_CATEGORIES) || (futureTechs == 0)) {
            setDefaultTechTreeAllocations();
            return;
        }

        float floatAllocation = 100.0f / (TechTree.NUM_CATEGORIES - futureTechs);
        float totalFloatAllocation = 0;
        int intAllocation = (int) floatAllocation;
        int totalIntAllocation = 0;

        for (int j=0; j<TechTree.NUM_CATEGORIES; j++) {
            if (empire.tech().category(j).studyingFutureTech())
                empire.tech().category(j).allocation(0);
            else {
                empire.tech().category(j).allocation(intAllocation);
                if (totalIntAllocation < (int) totalFloatAllocation)
                    empire.tech().category(j).increaseAllocation();
                totalIntAllocation += empire.tech().category(j).allocation();
                totalFloatAllocation += floatAllocation;
            }
        }
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

        if (empire.leader().isDiplomat()) {
            empire.tech().computer().allocation(9);
            empire.tech().construction().allocation(9);
            empire.tech().forceField().allocation(12);
            empire.tech().planetology().allocation(9);
            empire.tech().propulsion().allocation(11);
            empire.tech().weapon().allocation(10);
        }
        else if (empire.leader().isMilitarist()) {
            empire.tech().computer().allocation(11);
            empire.tech().construction().allocation(9);
            empire.tech().forceField().allocation(10);
            empire.tech().planetology().allocation(8);
            empire.tech().propulsion().allocation(10);
            empire.tech().weapon().allocation(12);
        }
        else if (empire.leader().isEcologist()) {
            empire.tech().computer().allocation(10);
            empire.tech().construction().allocation(10);
            empire.tech().forceField().allocation(10);
            empire.tech().planetology().allocation(12);
            empire.tech().propulsion().allocation(9);
            empire.tech().weapon().allocation(9);
        }
        else if (empire.leader().isIndustrialist()) {
            empire.tech().computer().allocation(10);
            empire.tech().construction().allocation(12);
            empire.tech().forceField().allocation(11);
            empire.tech().planetology().allocation(9);
            empire.tech().propulsion().allocation(9);
            empire.tech().weapon().allocation(9);
        }
        else if (empire.leader().isExpansionist()) {
            empire.tech().computer().allocation(10);
            empire.tech().construction().allocation(9);
            empire.tech().forceField().allocation(9);
            empire.tech().planetology().allocation(10);
            empire.tech().propulsion().allocation(12);
            empire.tech().weapon().allocation(10);
        }
        else if (empire.leader().isTechnologist()) {
            empire.tech().computer().allocation(10);
            empire.tech().construction().allocation(10);
            empire.tech().forceField().allocation(10);
            empire.tech().planetology().allocation(10);
            empire.tech().propulsion().allocation(10);
            empire.tech().weapon().allocation(10);
        }
        // if in special mode, change ratios
        if (empire.generalAI().inWarMode()) {
            empire.tech().computer().adjustAllocation(4);
            empire.tech().construction().adjustAllocation(-3);
            empire.tech().forceField().adjustAllocation(1);
            empire.tech().planetology().adjustAllocation(-3);
            empire.tech().propulsion().adjustAllocation(-3);
            empire.tech().weapon().adjustAllocation(4);
        }
        else if (empire.fleetCommanderAI().inExpansionMode()) {
            empire.tech().computer().adjustAllocation(1);
            empire.tech().construction().adjustAllocation(-3);
            empire.tech().forceField().adjustAllocation(-3);
            empire.tech().planetology().adjustAllocation(4);
            empire.tech().propulsion().adjustAllocation(4);
            empire.tech().weapon().adjustAllocation(-3);
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
            session().addTurnNotification(new SelectTechNotification(cat));
            return;
        }

        Tech.comparatorCiv = empire;
        Collections.sort(techs, Tech.RESEARCH_PRIORITY);

        // return highest priority
        cat.currentTech(techs.get(0));
    }
    //
    //  RESEARCH VALUES for various types of tech
    //
    @Override
    public float researchPriority(Tech t) {
        // by raising tech cost to 1.3, we will tend to value researching lower-cost
        // techs that have a similar value/cost ratio as more expensive ones
        // iow, for each 10x in cost, there needs to be 20x value to be same priority
        float costExp = 1.3f;
        return researchValue(t)/(float)Math.pow(t.researchCost(),costExp);
    }
    @Override
    public float researchValue(Tech t) {
        if (t.isObsolete(empire))
            return 0;

        if (empire.generalAI().inWarMode())
            return t.warModeFactor() * (researchValueBonus(t) + t.baseValue(empire));

        if (empire.fleetCommanderAI().inExpansionMode())
            return t.expansionModeFactor() * (researchValueBonus(t) + t.baseValue(empire));

        return researchValueBonus(t) + t.baseValue(empire);
    }
    @Override
    public float warTradeValue(Tech t) {
        if (t.isObsolete(empire))
            return 0;
        return t.warModeFactor() * (researchValueBonus(t) + t.baseValue(empire));
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
        float topHitsAdj = curr == null ? 1 : curr.hitsAdj;

        if (topHitsAdj >= t.hitsAdj)
            return 0;

        // scale so that 4x hits adjustment is best (level 50)
        float val = 12.5f * (t.hitsAdj/topHitsAdj);
        // armor has both offensive & defensive benefits... everyone gets broad adj
        val *= 2;
        // extra important for races with ground attack bonuses
        if (empire.race().groundAttackBonus() > 0)
            val *= 1.5;
        // armor has wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechAtmosphereEnrichment t) {
        // obsolete?
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

        float pctImproved = possible.size()/empire.allColonizedSystems().size();
        // treat as level-50 tech if every planet can benefit

        float val = 50 * pctImproved;
        float adj = 1.0f;
        if (empire.leader().isEcologist())
            adj *= 3;
        if (empire.leader().isExpansionist())
            adj *= 1.5;

        return adj*val;
    }
    @Override
    public float baseValue(TechBattleComputer t) {
        TechBattleComputer curr = empire.tech().topBattleComputerTech();
        int currMark = curr == null ? 0 : curr.mark;

        if (currMark >= t.mark)
            return 0;

        // scale add'l prevention assuming mark 15 is best (level 50)
        // note there is no reduction in value for existing battle computers
        // since older computer levels become worthless
        float val = 50 / 11.0f * t.mark;

        // battle computers more highly valued by more aggressive leader types
        if (empire.leader().isAggressive())
            val *= 1.5;
        else if (empire.leader().isRuthless())
            val *= 1.5;
        else if (empire.leader().isMilitarist())
            val *= 2;

        return val;
    }
    @Override
    public float baseValue(TechBiologicalAntidote t) {
        int currAntidote = 0;
        if (empire.tech().topBiologicalAntidoteTech() != null)
            currAntidote = empire.tech().topBiologicalAntidoteTech().attackReduction;

        if (currAntidote >= t.attackReduction)
            return 0;

        int maxDmg = 0;
        for (EmpireView v: empire.empireViews()) {
            if ((v != null) && v.embassy().anyWar()) {
                TechBiologicalWeapon enemyWeapon = v.spies().tech().topBiologicalWeaponTech();
                if (enemyWeapon != null)
                    maxDmg = max(maxDmg, enemyWeapon.maxDamage);
            }
        }
        // use *30 to get tech value of best-case scenario to 50
        float currDmg = TechBiologicalWeapon.avgDamage(maxDmg, currAntidote);
        float newDmg = TechBiologicalWeapon.avgDamage(maxDmg, t.attackReduction);
        float baseVal  = (newDmg - currDmg) * 30.0f;

        float adj = 1.0f;
        if (empire.leader().isPacifist())
            adj *= 2.0;
        if (empire.leader().isHonorable())
            adj *= 1.5;

        return adj * baseVal;
    }
    @Override
    public float baseValue(TechBiologicalWeapon t) {
        int currMaxDmg = 0;
        if (empire.tech().topBiologicalWeaponTech() != null)
            currMaxDmg = empire.tech().topBiologicalWeaponTech().maxDamage;

        if (currMaxDmg >= t.maxDamage)
            return 0;

        int maxAntidote = 0;
        for (EmpireView v: empire.empireViews()) {
            if ((v != null) && v.embassy().anyWar()) {
                TechBiologicalAntidote enemyAntidote = v.spies().tech().topBiologicalAntidoteTech();
                if (enemyAntidote != null)
                    maxAntidote = max(maxAntidote, enemyAntidote.attackReduction);
            }
        }

        // use *25 to get tech value of best-case scenario to 50
        float currDmg = TechBiologicalWeapon.avgDamage(currMaxDmg,  maxAntidote);
        float newDmg = TechBiologicalWeapon.avgDamage(t.maxDamage, maxAntidote);
        float baseVal = (newDmg - currDmg) * 25.0f;

        float adj = 1.0f;
        if (empire.leader().isMilitarist())
            adj *= 2.0;
        if (empire.leader().isAggressive())
            adj *= 1.5;
        if (empire.leader().isEcologist())
            adj *= 0.75;
        if (empire.leader().isHonorable())
            adj *= 0.5;
        
        return adj * baseVal;
    }
    @Override
    public float baseValue(TechBlackHole t) {
        // scale add'l prevention assuming 70 dmg is best (level 50)
        float val = t.level;

        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // bombs have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechBombWeapon t) {
        TechBombWeapon curr = empire.tech().topBombWeaponTech();

        // scale add'l prevention assuming 70 dmg is best (level 50)
        float val = 0.71f * curr.damageHigh();

        // aggressive/militarists love shields! xenos, too
        if (empire.leader().isRuthless())
            val *= 2;
        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // bombs have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechCloning t) {
        if (empire.tech().populationCost() <= t.growthCost)
            return 0;

        float adj = 1.0f;
        if (empire.leader().isExpansionist())
            adj *= 2;
        if (empire.leader().isTechnologist())
            adj *= 1.5;
        if (empire.tech().topCloningTech() == null)
            return adj * t.level / empire.race().growthRateMod();
        else
            return adj * (t.level - empire.tech().topCloningTech().level) / empire.race().growthRateMod();
    }
    @Override
    public float baseValue(TechCombatTransporter t) {
        float val = t.level;
        // extra important for races with ground attack bonuses
        if (empire.race().groundAttackBonus() > 0)
            val *= 1.5;
        // pacifists love shields! xenos, too
        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // shields have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechControlEnvironment t) {
        if (empire.race().ignoresPlanetEnvironment())
            return 0;
        // obsolete?
        if (t.environment() <= empire.tech().minColonyLevel())
            return 0;
        List<StarSystem> possible = empire.uncolonizedPlanetsInShipRange((int)empire.tech().minColonyLevel());
        List<StarSystem> newPossible = empire.uncolonizedPlanetsInShipRange(t.environment());
        float newPlanets = newPossible.size() - possible.size();
        if (newPlanets < 1)
            return 0;

        float adj = 1.0f;
        if (empire.leader().isExpansionist())
            adj *= 2;
        if (empire.leader().isEcologist())
            adj *= 1.5;

        int numRaces = empire.contactedEmpires().size() + 1;
        // multiply tech level by # new planets possible vs. # known races
        return (newPlanets / numRaces) * adj * t.level;
    }
    @Override
    public float baseValue(TechDeflectorShield t) {
        TechDeflectorShield curr = empire.tech().topDeflectorShieldTech();
        float currVal = curr == null ? 0 : curr.damage;

        if (currVal >= t.damage)
            return 0;

        // scale add'l prevention assuming mark 15 is best (level 50)
        float val = 50 / 15.0f * (t.damage - currVal);
        // pacifists and militarists both like defensive value
        if (empire.leader().isPacifist())
            val *= 1.5;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        return val;
    }
    @Override
    public float baseValue(TechECMJammer t) {
        TechECMJammer curr = empire.tech().topECMJammerTech();
        float currMark = curr == null ? 0 : curr.mark;

        if (currMark >= t.mark)
            return 0;

        // scale add'l prevention assuming mark 10 is best (level 50)
        float val = 5 * (t.mark - currMark);
        // technologists love outsmarting others
        if (empire.leader().isTechnologist())
            val *= 1.5;
        // pacifists like defensive value
        if (empire.leader().isPacifist())
            val *= 1.25;

        return val;
    }
    @Override
    public float baseValue(TechEcoRestoration t) {
        if (empire.race().ignoresPlanetEnvironment())
            return 0;

        float adj = 1.0f;
        if (empire.leader().isEcologist())
            adj *= 2;

        return adj * t.level() * empire.tech().wasteCleanupTechMod();
    }
    @Override
    public float baseValue(TechEngineWarp t) {
        TechEngineWarp curr = empire.tech().topEngineWarpTech();
        // obsolete?
        if (curr.warp() >= t.warp())
            return 0;

        float  val = t.level * t.warp() / curr.warp();
        float adj = 1.0f;
        if (empire.leader().isMilitarist())
            adj *= 1.5;

        return adj * val;
    }
    @Override
    public float baseValue(TechFuelRange t) {
        TechFuelRange curr = empire.tech().topFuelRangeTech();
        int currRange = curr == null ? 3 : curr.range();
        // obsolete?
        if (currRange >= t.range())
            return 0;

        int newRange = min(10,t.range());

        float val = 7 * (newRange-currRange);
        float adj = 1.0f;
        if (empire.leader().isExpansionist())
            adj *= 2;
        if (empire.leader().isDiplomat())
            adj *= 1.5;

        return adj * val;
    }
    @Override
    public float baseValue(TechHandWeapon t) {
        TechHandWeapon curr = empire.tech().topHandWeaponTech();
        int topCombatMod = curr == null ? 0 : curr.combatMod;

        if (topCombatMod >= t.combatMod)
            return 0;

        // scale add'l prevention assuming 30 dmg is best (level 50)
        float val = 1.67f * (t.combatMod-topCombatMod);
        // extra important for races with ground attack bonuses
        if (empire.race().groundAttackBonus() > 0)
            val *= 1.5;
        // pacifists love shields! xenos, too
        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // shields have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechImprovedIndustrial t) {
        TechImprovedIndustrial curr = empire.tech().topImprovedIndustrialTech();
        float currCost = curr == null ? 10 : curr.factoryCost;

        if (currCost <= t.factoryCost)
            return 0;

        // increased value when increased roboticc controls
        float adj = empire.tech().baseRobotControls() / TechRoboticControls.BASE_ROBOT_CONTROLS;
        // scale add'l prevention assuming 5x factories (2BC from starting 10) is best (level 50)
        // with max robot controls (adj = 7/2 = 3.5)
        // these don't compound because the value of this tech plummets when no factories to build
        float val = 10 / 3.5f * (currCost / t.factoryCost);
        // industrialists love factories! economists, too
        if (empire.leader().isIndustrialist())
            val *= 1.5;

        return val*adj;
    }
    @Override
    public float baseValue(TechImprovedTerraforming t) {
        TechImprovedTerraforming curr = empire.tech().topTerraformingTech();
        int topIncrease = curr == null ? 0 : curr.increase();

        if (topIncrease >= t.increase())
            return 0;

        // scale assuming 120 growth is best (level 50)
        float val = 50.0f / 120 * (t.increase()-topIncrease);
        // pacifists love shields! xenos, too
        if (empire.leader().isEcologist())
            val *= 2;
        if (empire.leader().isExpansionist())
            val *= 1.5;

        return val;
    }
    @Override
    public float baseValue(TechIndustrialWaste t) {
        if (empire.race().ignoresPlanetEnvironment())
            return 0;

        float adj = 1.0f;
        if (empire.leader().isEcologist())
            adj *= 2;

        return adj * t.level() * empire.tech().wasteCleanupTechMod();
    }
    @Override
    public float baseValue(TechMissileShield t) {
        TechMissileShield curr = empire.tech().topMissileShieldTech();
        float currVal = curr == null ? 0 : curr.baseBlockPct;

        if (currVal >= t.baseBlockPct)
            return 0;

        // scale add'l prevention assuming 100%  block is best (level 50)
        float val = 0.5f * (t.baseBlockPct - currVal);
        // pacifists and militarists both like defensive value
        if (empire.leader().isTechnologist())
            val *= 2.0;
        if (empire.leader().isPacifist())
            val *= 1.5;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        return val;
    }
    @Override
    public float baseValue(TechMissileWeapon t) {
        TechMissileWeapon curr = empire.tech().topBaseMissileTech();
        
        // turns out this effectiveness formula equals about 50 for the
        // highest value, so no need for a scaling factor to make it 50
        float currVal = curr.damage() * (float) Math.sqrt(curr.attacks);
        float tVal = t.damage() * (float) Math.sqrt(t.attacks);

        if (tVal <= currVal)
            return 0;

        float val = tVal - currVal;

        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // bombs have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
        
        /*
        if (isMissileBaseWeapon()) {
            if ((attacks > 1)  && (c.tech().topBaseScatterPackTech() != null))
                return (float) level - c.tech().topBaseScatterPackTech().level;
            else if (c.tech().topBaseMissileTech() != null)
                return (float) level - c.tech().topBaseMissileTech().level;
        }
        return level;
        */
    }
    @Override
    public float baseValue(TechPersonalShield t) {
        TechPersonalShield curr = empire.tech().topPersonalShieldTech();
        int topBonus = curr == null ? 0 : curr.groundAttackBonus;

        if (topBonus >= t.groundAttackBonus)
            return 0;

        // scale add'l prevention assuming 30 dmg prevented is best (level 50)
        float val = 1.67f * (t.groundAttackBonus-topBonus);
        // extra important for races with ground attack bonuses
        if (empire.race().groundAttackBonus() > 0)
            val *= 1.5;
        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isPacifist())
            val *= 1.5;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // shields have wartime value: multiply by current war enemies
        val *= sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechPlanetaryShield t) {
        TechPlanetaryShield curr = empire.tech().topPlanetaryShieldTech();
        float currDmg = curr == null ? 0 : curr.damage;

        if (currDmg >= t.damage)
            return 0;

        // scale add'l prevention assuming 20 dmg prevented is best (level 50)
        float val = 2.5f * (t.damage-currDmg);
        // shields are just a generally valuable tech
        val *= 1.5;
        // pacifists love shields! xenos, too
        if (empire.leader().isPacifist())
            val *= 2;
        if (empire.leader().isXenophobic())
            val *= 1.5;

        // shields have wartime value: multiply by current war enemies
        val *= sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechRoboticControls t) {
        TechRoboticControls curr = empire.tech().topRoboticControlsTech();
        float currMark = curr == null ? 0 : curr.mark;

        if (currMark >= t.mark)
            return 0;

        // scale add'l prevention assuming Mark 7 (starts with 2) prevented is best (level 50)
        float val = 5 * (t.mark-currMark);
        // robotic controls are just a generally valuable tech
        val *= 1.5;
        // industrialists love factories! economists, too
        if (empire.leader().isIndustrialist())
            val *= 2;
        return val;
    }
    @Override
    public float baseValue(TechShipInertial t) {
        TechShipInertial curr = empire.tech().topShipInertialTech();
        float topDefense = curr == null ? 0 : curr.defenseBonus;

        if (topDefense >= t.defenseBonus)
            return 0;

        // scale add'l prevention assuming 4 defense bonus is best (level 40)
        float val = 10 * (t.defenseBonus - topDefense);
        // ship combat centric races prefer this
        if (empire.race().shipAttackBonus() > 0)
            val *= 2.0;
        if (empire.race().shipDefenseBonus() > 0)
            val *= 2.0;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        return val;
    }
    @Override
    public float baseValue(TechShipWeapon t) {
        TechShipWeapon curr = empire.tech().topShipWeaponTech();

        // turns out this effectiveness formula equals about 50 for the
        // highest value, so no need for a scaling factor to make it 50
        float currVal = (curr.damageHigh() * curr.attacksPerRound) / curr.size;
        float tVal = (t.damageHigh()*t.attacksPerRound) / t.size;

        if (tVal <= currVal)
            return 0;

        float val = tVal - currVal;

        if (empire.leader().isAggressive())
            val *= 2;
        if (empire.leader().isMilitarist())
            val *= 1.5;

        // bombs have wartime value: multiply by current war enemies
        val *= Math.sqrt(empire.numEnemies()+1);

        return val;
    }
    @Override
    public float baseValue(TechSoilEnrichment t) {
        if (empire.race().ignoresPlanetEnvironment())
            return 0;
        TechSoilEnrichment curr = empire.tech().topSoilEnrichmentTech();
        float topIncrease = curr == null ? 0 : curr.planetaryIncrease;

        if (topIncrease >= t.planetaryIncrease)
            return 0;

        // scale assuming 50 growth is best (level 50)
        float val = 1.0f* t.planetaryIncrease-topIncrease;
        float adj = 1.0f;
        if (empire.leader().isEcologist())
            adj *= 2;
        if (empire.leader().isExpansionist())
            adj *= 1.5;
        return adj * val;
    }
    @Override
    public float baseValue(TechStargate t) {
        List<StarSystem> allColonies = empire.allColonizedSystems();
        if (allColonies.size() < 2)
            return 0;
        float maxDistance = 0;
        for (StarSystem pv1: allColonies) {
            for (StarSystem pv2: allColonies)
                maxDistance = max(maxDistance, pv1.distanceTo(pv2));
        }

        if (maxDistance < empire.tech().topSpeed())
            return 0;

        float adj = 1.0f;
        if (empire.leader().isExpansionist())
            adj *= 1.5;
        if (empire.leader().isTechnologist())
            adj *= 1.25;

        return adj * t.level;
    }
    @Override
    public float baseValue(TechSubspaceInterdictor t) {
        boolean anyEnemiesHaveTeleporter = false;
        for (EmpireView v: empire.empireViews()) {
            if ((v != null) && v.embassy().anyWar()) {
                if (v.spies().tech().knowsTechOfType(Tech.TELEPORTER))
                        anyEnemiesHaveTeleporter = true;
            }
        }
        float adj = 1.0f;
        if (empire.leader().isPacifist())
            adj *= 1.5;
        if (empire.leader().isXenophobic())
            adj *= 1.25;

        if (anyEnemiesHaveTeleporter)
            return adj * t.level * 2;

        return adj * t.level / 2;
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

        float adj = 1.0f;
        if (empire.leader().isMilitarist())
            adj *= 1.5;
        if (empire.leader().isAggressive())
            adj *= 1.25;

        return adj * t.level;
    }
}
