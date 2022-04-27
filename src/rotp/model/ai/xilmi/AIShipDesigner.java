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

import java.util.Arrays;
import java.util.List;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.Ships;
import rotp.model.galaxy.StarSystem;
import static rotp.model.game.IGameOptions.RESEARCH_SLOW;
import static rotp.model.game.IGameOptions.RESEARCH_SLOWER;
import static rotp.model.game.IGameOptions.RESEARCH_SLOWEST;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
import static rotp.model.ships.ShipDesign.maxSpecials;
import static rotp.model.ships.ShipDesign.maxWeapons;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipSpecialColony;
import rotp.model.ships.ShipWeapon;
import rotp.util.Base;

public class AIShipDesigner implements Base, ShipDesigner {
    private static final int OBS_DESTROYER_TURNS = 20;
    //ail: I have more slots available for bombers and fighters, so I can increase the time until they are obsolete
    private static final int OBS_FIGHTER_TURNS = 32;
    private static final int OBS_BOMBER_TURNS = 24;
    private static final int OBS_COLONY_TURNS = 8;
    private static final int OBS_SCOUT_TURNS = 1;

    private final Empire empire;
    private int[] shipCounts;

    public AIShipDesigner (Empire c) {
        empire = c;
    }
    @Override
    public String toString()   { return concat("ShipDesigner: ", empire.raceName()); }
    @Override
    public Empire empire()     { return empire; }
    @Override
    public ShipDesignLab lab() { return empire.shipLab(); }
    @Override
    public void nextTurn() {
        if (empire.isAIControlled()) {
            log(this+": nextTurn");
            shipCounts = galaxy().ships.shipDesignCounts(empire.id);
            // designs are updated in a specific order in order to prioritize
            // use of empty design slots
            //ail: slightly hacky way to prevent accidentally building stargates as it keeps happening
            if(empire.tech().canBuildStargate())
                empire.tech().canBuildStargate(false);
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" Fighter: "+BestDesignToFight().name());
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" Bomber: "+lab().bomberDesign().name());
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" Colo: "+lab().colonyDesign().name());
            boolean wantHybrid = wantHybrid();
            MarkObsolete();
            updateFighterDesign();
            if(!wantHybrid)
                updateBomberDesign();
            updateColonyDesign();
            if(empire.shipLab().needScouts)
                updateScoutDesign();
            countdownObsoleteDesigns();
        }
    }
    @Override
    public ShipDesign bestDesignToColonize(ShipFleet fl, StarSystem sys) {
        // in case there is more than one colony ship in this fleet,
        // find the best one to use at this planet. And by "best" we
        // mean the ship that we want to most get rid of by using 
        // it to colonize the planet... pick the slowest one first.
        // if that ties, pick the one with the worst colony module.
        ShipDesignLab lab = lab();
        ShipDesign bestDesign = null;
        for (int i=0;i<fl.num.length;i++) {
            if (fl.num[i] > 0) {
                ShipDesign design = lab.design(i);
                ShipSpecialColony special = design.colonySpecial();
                if (special != null) {
                    PlanetType pt = sys.planet().type();
                    if (empire.ignoresPlanetEnvironment()
                    || (empire.canColonize(pt) && special.tech().canColonize(pt)) ) {
                        if ((bestDesign == null)
                        || (design.engine().warp() < bestDesign.engine().warp()))
                            bestDesign = design;
                        else if (design.engine().warp() == bestDesign.engine().warp()) {
                            if (special.tech().environment() < bestDesign.colonySpecial().tech().environment())
                                bestDesign = design;
                            else if (special.tech().environment() == bestDesign.colonySpecial().tech().environment())
                                if(design.range() < bestDesign.range())
                                    bestDesign = design;
                                else if(design.range() == bestDesign.range())
                                    if(design.cost() > bestDesign.cost())
                                        bestDesign = design;
                        }
                    }
                }
            }
        }
        return bestDesign;
    }
    private void countdownObsoleteDesigns() {
        if(((empire.shipMaintCostPerBC() > (empire.fleetCommanderAI().maxShipMaintainance() * 1.5) && empire.enemies().isEmpty() && !empire.fleetCommanderAI().inExpansionMode()))
                || (empire.netIncome() <= 0 && !empire.atWar()))
        {
            //System.out.print("\n"+empire.name()+" scrapWorstDesign max-maint: "+empire.fleetCommanderAI().maxShipMaintainance());
            scrapWorstDesign(empire.netIncome() <= 0);
        }
    }
    private void scrapWorstDesign(boolean forceCosting) {
        ShipDesignLab lab = lab();
        ShipDesign designToScrap = null;
        float lowestKeepScore = Float.MAX_VALUE;
        boolean shouldScrap = true;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+d.name()+" obsolete: "+d.obsolete());
            if (d.obsolete() || forceCosting) {
                if(forceCosting && shipCounts[d.id()] == 0)
                {
                    continue;
                }
                float keepScore = 1;
                //ail: for colony-ship we don't care about available space but engine- and colony-base-discrepancy
                if(d.isColonyShip())
                {
                    keepScore = ((float)d.engine().warp() / (float)lab().fastestEngine().warp()) * ((float)(d.special(0).tech()).level / (float)bestColonySpecial().tech().level);
                }
                else if(d.isScout())
                {
                    keepScore = (float)d.engine().warp() / (float)lab().fastestEngine().warp();
                }
                else
                {
                    keepScore = (1 - d.availableSpace()/d.totalSpace()) * (float)d.engine().warp() / (float)lab().fastestEngine().warp();
                }
                keepScore *= keepScore;
                float keepBeforeCounts = keepScore;
                keepScore *= shipCounts[d.id()] * d.cost();
                //we can scrap all that we don't need at all
                //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+d.name()+" keepScore: "+keepScore+" before counts: "+keepBeforeCounts+" role: "+d.mission());
                if(keepScore == 0 || forceCosting)
                {
                    ScrapDesign(d);
                    //if we do this, we don't have to scrap the design with the lowest keepscore anymore because a slot is now free
                    shouldScrap = false;
                    if(!forceCosting)
                        break;
                }
                else if(keepScore < lowestKeepScore)
                {
                    lowestKeepScore = keepScore;
                    designToScrap = d;
                    //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+designToScrap.name()+" keepScore: "+keepScore+" shouldScrap: "+shouldScrap);
                }
            }
        }
        if(designToScrap != null && shouldScrap)
        {
            //System.out.print("\n"+empire.name()+" "+designToScrap.name()+" is scrapped.");
            ScrapDesign(designToScrap);
        }
    }

    public void updateScoutDesign() {
        ShipDesignLab lab = lab();
        // if we are not using scouts anymore, quit
        ShipDesign currDesign = BestDesignToScout();

        ShipDesign newScout = newScoutDesign();
        
        int slot = lab.availableDesignSlot();
        
        if(currDesign == null)
        {
            if(slot < 0)
                scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setScoutDesign(newScout, slot);
            return;
        }
        
        if (currDesign.engine() == lab.fastestEngine() && currDesign.range() == newScout.range())
            return;

        if (newScout.matchesDesign(currDesign, false) && currDesign.active())
            return;

        // NEW DESIGN IS BETTER THAN CURRENT
        if (slot >= 0) {
            lab.setScoutDesign(newScout, slot);
            currDesign.becomeObsolete(OBS_SCOUT_TURNS);
            //System.out.print("\n"+empire.name()+" "+newDesign.name()+" put in slot "+slot);
        }
        else
        {
            //if there is no slot available I push the old one to obsolete so a new slot will be freed up soon
            currDesign.becomeObsolete(OBS_SCOUT_TURNS);
            scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            //System.out.print("\n"+empire.name()+" "+newDesign.name()+" put in slot "+slot+" after scrapping something.");
            lab.setScoutDesign(newScout, slot);
        }
    }
    public void updateColonyDesign() {
        ShipDesignLab lab = lab();
        ShipDesign currDesign = BestDesignToColonize();
        
        // weapons needed on colony ships if we've made AI contact
        boolean weaponsNeeded = !empire.contactedEmpires().isEmpty();
        // current design is "properlyArmed" if it is armed &weapons needed or unarmed & weapons unneeded
        //always try to make extended, if possible, additional-cost is worth it and once it fits on large, the costs won't matter much anyways
        boolean extendedFuelNeeded = !empire.tech().topFuelRangeTech().unlimited;

        ShipDesign newDesign = newColonyDesign(weaponsNeeded, extendedFuelNeeded);
        
        if(currDesign == null)
        {
            int slot = lab.availableDesignSlot();
            if(slot < 0)
                scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setColonyDesign(newDesign, slot);
            return;
        }
        
        if (currDesign.matchesDesign(newDesign, true) && currDesign.active())
        {
            //System.out.print("\n"+empire.name()+" "+newDesign.name()+" size: "+newDesign.size()+" matches with "+currDesign.name()+" size: "+currDesign.size()); 
            //When adding a weapon for the first time, we want to redesign regardless of otherwise not caring about weapons
            boolean currCanFight = fightingAdapted(currDesign) > 0;
            boolean newCanFight = fightingAdapted(newDesign) > 0;
            if(currCanFight == newCanFight)
                return;
        }
        
        int slot = lab.availableDesignSlot();
        
        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            lab.setColonyDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_COLONY_TURNS);
            //System.out.print("\n"+empire.name()+" "+newDesign.name()+" put in slot "+slot);
            return;
        }
        else
        {
            //if there is no slot available I push the old one to obsolete so a new slot will be freed up soon
            currDesign.becomeObsolete(OBS_COLONY_TURNS);
            scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            //System.out.print("\n"+empire.name()+" "+newDesign.name()+" put in slot "+slot+" after scrapping something.");
            lab.setColonyDesign(newDesign, slot);
        }
    }
    public void updateBomberDesign() {
        ShipDesignLab lab = lab();
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = BestDesignToBomb();
        
        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // if we don't have any faster engines
        /*if (currDesign.engine() == lab.fastestEngine() && currDesign.active()) {
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.1f) && slot < 0) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better            
                currDesign.remainingLife++;
                return;
            }
        }*/
        //System.out.print("\n"+empire.name()+" should design new bomber since old one has "+currDesign.availableSpace()/currDesign.totalSpace()+"free space");

        // find best hypothetical design vs current targets
        ShipDesign newDesign = newBomberDesign(0);
        
        if(currDesign == null)
        {
            if(slot < 0)
                scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setBomberDesign(newDesign, slot);
            return;
        }
        
        // WE HAVE BOMBERS IN USE... VERIFY THIS UPGRADE JUSTIFIES SWITCHING OVER
        boolean betterComputer = (newDesign.computer().level() > currDesign.computer().level());
        boolean betterEngine = (newDesign.engine().warp() > currDesign.engine().warp());
        boolean betterArmor = (newDesign.armor().sequence() > currDesign.armor().sequence());
        boolean betterSpecial = false;
        boolean oldHasCloaking = false;
        boolean newHasCloaking = false;
        boolean oldHasBHG = false;
        boolean newHasBHG = false;
        boolean maintenanceLimitReached = MaintenanceLimitReached(currDesign);
        boolean oldIsBomber = bombingAdapted(currDesign) > 0.5;
        boolean bioWeaponObsolete = false;
        boolean sameWeapon = MainWeapon(currDesign) == MainWeapon(newDesign);
        
        float antiDote = 0;
        for(EmpireView ev : empire().contacts())
            if(ev.spies().tech().antidoteLevel() > antiDote)
                antiDote = ev.spies().tech().antidoteLevel();
        
        for (int i=0; i<maxWeapons(); i++)
            if(currDesign.weapon(i).isBioWeapon() && currDesign.wpnCount(i) > 0)
                if(currDesign.weapon(i).maxDamage() - antiDote <= 0)
                    bioWeaponObsolete = true;
        
        for (int i=0;i<maxSpecials();i++) {
            if(currDesign.special(i).allowsCloaking() == true)
                oldHasCloaking = true; 
            if(newDesign.special(i).allowsCloaking() == true)
                newHasCloaking = true;
            if(currDesign.special(i).createsBlackHole())
                oldHasBHG = true;
            if(newDesign.special(i).createsBlackHole())
                newHasBHG = true;
        }
        
        if(!oldHasCloaking && newHasCloaking)
            betterSpecial = true;
        if(!oldHasBHG && newHasBHG)
            betterSpecial = true;

        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        float upgradeThreshold = 1.25f;
        
        float upgradeChance = 1 + currDesign.availableSpace() / currDesign.totalSpace();
        
        //System.out.print("\n"+galaxy().currentYear()+" "+empire.name()+" Bomber upgrade "+currDesign.name()+" val: "+upgradeChance+" DPBC: "+newDPBC / currentDPBC+" better-Engine: "+betterEngine+" betterArmor: "+betterArmor);
        
        if(oldIsBomber || currDesign.matchesDesign(newDesign))
            if (sameWeapon && !maintenanceLimitReached && !bioWeaponObsolete && !betterComputer && !betterSpecial && !betterEngine && !betterArmor && (upgradeChance < upgradeThreshold) && currDesign.active() )
                return;
        
        //System.out.print("\n"+empire.name()+" designed new bomber which is "+upgradeChance+" better and should go to slot: "+slot);

        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            log("Slot available: Bomber upgrade chance:"+upgradeChance);
            lab.setBomberDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_BOMBER_TURNS);
        }
        else
        {
            //if there is no slot available I push the old one to obsolete so a new slot will be freed up soon
            currDesign.becomeObsolete(OBS_BOMBER_TURNS);
            scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setBomberDesign(newDesign, slot);
        }
    }
    public void updateFighterDesign() {
        ShipDesignLab lab = lab();
        
        boolean needRange = false;
        
        for(EmpireView ev : empire().contacts())
        {
            for(ShipDesign enemyDesign : ev.empire().shipLab().designs())
            {
                if(enemyDesign.scrapped())
                {
                    continue;
                }
                if(enemyDesign.repulsorRange() > 0)
                {
                    needRange = true;
                }
            }
        }
       
        ShipDesign currDesign = BestDesignToFight();

        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        /*if (currDesign.engine() == lab.fastestEngine() && currDesign.active() && !needRange) {
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.1f) && slot < 0) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better
                currDesign.remainingLife++;
                return;
            }
        }*/

        // find best hypothetical design vs current targets
        ShipDesign newDesign = newFighterDesign(0);
        
        if(currDesign == null)
        {
            if(slot < 0)
                scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setFighterDesign(newDesign, slot);
            return;
        }
        
        for (int j=0;j<maxSpecials();j++)
            if(currDesign.special(j).beamRangeBonus() > 0)
                needRange = false;
        for(int j=0;j<maxWeapons();j++)
            if(currDesign.weapon(j).range() > 1)
                needRange = false;
        if(currDesign.allowsCloaking())
            needRange = false;
        
        // WE HAVE FIGHTERS IN USE... VERIFY THIS UPGRADE JUSTIFIES SWITCHING OVER

        // factor in speed improvements when determining if new design is better
        // i.e. wrp 5, 100 dmg is better than wrp 3, 120 dmg
        boolean betterComputer = (newDesign.computer().level() > currDesign.computer().level());
        boolean betterEngine = (newDesign.engine().warp() > currDesign.engine().warp());
        boolean betterArmor = (newDesign.armor().sequence() > currDesign.armor().sequence());
        boolean betterSpecial = false;
        boolean oldHasCloaking = false;
        boolean newHasCloaking = false;
        boolean oldHasBHG = false;
        boolean newHasBHG = false;
        boolean maintenanceLimitReached = MaintenanceLimitReached(currDesign);
        boolean sameWeapon = MainWeapon(currDesign) == MainWeapon(newDesign);
        
        boolean betterShield = false;
        if(currDesign.size() == newDesign.size())
            if(newDesign.shieldLevel() > currDesign.shieldLevel())
                betterShield = true;
        
        for (int i=0;i<maxSpecials();i++) {
            if(currDesign.special(i).allowsCloaking() == true)
                oldHasCloaking = true; 
            if(newDesign.special(i).allowsCloaking() == true)
                newHasCloaking = true;
            if(currDesign.special(i).createsBlackHole())
                oldHasBHG = true;
            if(newDesign.special(i).createsBlackHole())
                newHasBHG = true;
        }
        
        if(!oldHasCloaking && newHasCloaking)
            betterSpecial = true;
        if(!oldHasBHG && newHasBHG)
            betterSpecial = true;
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        float upgradeThreshold = 1.25f;
        
        float upgradeChance = 1 + currDesign.availableSpace() / currDesign.totalSpace();
        
        //System.out.print("\n"+galaxy().currentYear()+" "+empire.name()+" Fighter upgrade "+currDesign.name()+" val: "+upgradeChance+" better-Engine: "+betterEngine+" betterArmor: "+betterArmor+" curr-id: "+currDesign.id());
        
        //System.out.print("\n"+empire.name()+" designed new fighter which is "+upgradeChance+" better and should go to slot: "+slot);
        if (sameWeapon && !betterShield && !maintenanceLimitReached && !betterSpecial && !betterComputer && !betterEngine && !betterArmor && (upgradeChance < upgradeThreshold) && !needRange && currDesign.active())
            return;
        //System.out.print("\n"+empire.name()+" designed new fighter and it went through.");

        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            log("Slot available: Fighter upgrade chance:"+upgradeChance);
            lab.setFighterDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_FIGHTER_TURNS);
        }
        else
        {
            currDesign.becomeObsolete(OBS_FIGHTER_TURNS);
            scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setFighterDesign(newDesign, slot);
        }
    }
    public void updateDestroyerDesign() {
        ShipDesignLab lab = lab();
        ShipDesign currDesign = lab.destroyerDesign();
        if(currDesign.active() && currDesign.isDestroyer())
        {
            currDesign.becomeObsolete(OBS_DESTROYER_TURNS);
            ScrapDesign(currDesign);
        }
    }
    @Override
    public ShipDesign newScoutDesign() {
        ShipDesignLab lab = lab();
        ShipDesign design = lab.newBlankDesign(ShipDesign.SMALL);
        design.engine(lab.fastestEngine());
        ShipSpecial special = lab.specialReserveFuel();
        design.special(0, special);
        design.setSmallestSize();
        design.mission(ShipDesign.SCOUT);
        design.maxUnusedTurns(OBS_SCOUT_TURNS);
        lab.nameDesign(design);
        //NewShipTemplate.nameShipDesign(this, design);
        lab.iconifyDesign(design);
        return design;
    }
    public ShipDesign newColonyDesign() {
        return newColonyDesign(!empire.contactedEmpires().isEmpty(), false);
    }
    public ShipDesign newColonyDesign(boolean weaponNeeded, boolean extendedRangeNeeded) {
        ShipDesignLab lab = lab();
        ShipDesign design = lab.newBlankDesign(ShipDesign.LARGE);
        lab().nameDesign(design);
        //NewShipTemplate.nameShipDesign(this, design);
        design.special(0, bestColonySpecial());
        design.engine(lab.fastestEngine());
        design.mission(ShipDesign.COLONY);
        design.maxUnusedTurns(OBS_COLONY_TURNS);

        boolean allowHuge = false;
        boolean unexploredInRange = false;
        
        for(StarSystem unexplored:empire.unexploredSystems())
        {
            if(unexplored.monster() != null)
                continue;
            if(empire.sv.inShipRange(unexplored.id))
            {
                unexploredInRange = true;
                break;
            }
        }
        
        float rangeTechLevelThreshold = 9;
        
        rangeTechLevelThreshold /= max(1.0f, session().researchMapSizeAdjustment());
        
        //System.out.print("\n"+empire.name()+" rangeTechLevelThreshold for galaxysize/empires: "+rangeTechLevelThreshold);
        
        if(session().options().selectedResearchRate().equals(RESEARCH_SLOW))
            rangeTechLevelThreshold /= sqrt(9/3.0f);
        else if(session().options().selectedResearchRate().equals(RESEARCH_SLOWER))
            rangeTechLevelThreshold /= sqrt(9);
        else if(session().options().selectedResearchRate().equals(RESEARCH_SLOWEST))
            rangeTechLevelThreshold /= sqrt(9*5);
            
        if(empire.uncolonizedPlanetsInRange(empire.shipRange()).isEmpty() 
                && empire.enemies().isEmpty()
                && !unexploredInRange
                && !empire.uncolonizedPlanetsInRange(empire.scoutRange()).isEmpty()
                && (empire.tech().propulsion().techLevel() >= rangeTechLevelThreshold && empire.tech().researchingShipRange() <= empire.shipRange() || empire.tech().propulsion().techLevel() >= 2 * rangeTechLevelThreshold))
            allowHuge = true;
            
        //System.out.print("\n"+empire.name()+" colonizable in normal range: "+empire.uncolonizedPlanetsInRange(empire.shipRange()).size()+" colonizable in extended-range: "+empire.uncolonizedPlanetsInRange(empire.scoutRange()).size()+" unexplored in range: "+unexploredInRange+" huge allowed: "+allowHuge+" rtlt: "+rangeTechLevelThreshold+" prop-lvl: "+empire.tech().propulsion().techLevel()+" res-Range: "+empire.tech().researchingShipRange()+" unexplored in range: "+unexploredInRange+" uncolonized in range: "+empire.uncolonizedPlanetsInRange(empire.scoutRange()).size());
        // if we don't need regular-range colony ship
        if (extendedRangeNeeded) {
            ShipSpecial special = lab.specialReserveFuel();
            ShipSpecial prevSpecial = design.special(1);
            design.special(1, special);
            design.setSmallestSize();
            if (design.size() > ShipDesign.LARGE
                    && !allowHuge)
                design.special(1, prevSpecial);
        }
        design.setSmallestSize();
        //ail: only if we now still have room, we put a weapon on it. Extended range and smaller size is way more important than a weapon without computer
        if (weaponNeeded) {
            ShipWeapon bestWpn = lab.bestUnlimitedShotWeapon(design, 1);
            if (bestWpn != null && design.availableSpace() >= bestWpn.space(design))
                design.addWeapon(bestWpn, 1);
        }
        ShipSpecial cloak = lab.specialCloak();
        if(cloak != null && cloak.space(design) <= design.availableSpace())
            design.special(2, cloak);
        lab.iconifyDesign(design);
        return design;
    }
    @Override
    public int optimalShipFighterSize() {
        int preferredSize = empire.preferredShipSize();
        if (preferredSize == ShipDesign.SMALL)
            return preferredSize;

        List<StarSystem> systems = empire.allColonizedSystems();
        float maxProd = 0;
        for (StarSystem sys: systems)
            maxProd = max(sys.colony().production(), maxProd);

        int maxSize = ShipDesign.SMALL;
        if (maxProd >= 1500)
            maxSize = ShipDesign.HUGE;
        else if (maxProd >= 300)
            maxSize = ShipDesign.LARGE;
        else if (maxProd >= 60)
            maxSize = ShipDesign.MEDIUM;
        return min(preferredSize, maxSize);
    }
    @Override
    public int optimalShipBomberSize() {
        int preferredSize = empire.preferredShipSize()+1;
        if (preferredSize == ShipDesign.LARGE)
            return ShipDesign.LARGE;

        List<StarSystem> systems = empire.allColonizedSystems();
        float maxProd = 0;
        for (StarSystem sys: systems)
            maxProd = max(sys.colony().production(), maxProd);

        int maxSize = ShipDesign.MEDIUM;
        if (maxProd >= 1000)
            maxSize = ShipDesign.HUGE;
        else if (maxProd >= 200)
            maxSize = ShipDesign.LARGE;
        return min(preferredSize, maxSize);
    }
    @Override
    public int optimalShipDestroyerSize() {
        int preferredSize = empire.preferredShipSize()+1;
        if (preferredSize == ShipDesign.LARGE)
            return ShipDesign.LARGE;

        List<StarSystem> systems = empire.allColonizedSystems();
        float maxProd = 0;
        for (StarSystem sys: systems)
            maxProd = max(sys.colony().production(), maxProd);

        int maxSize = ShipDesign.MEDIUM;
        if (maxProd >= 1000)
            maxSize = ShipDesign.HUGE;
        else if (maxProd >= 200)
            maxSize = ShipDesign.LARGE;
        return min(preferredSize, maxSize);
    }
    @Override
    public ShipDesign newFighterDesign(int size) {
    //    ShipDesign design = ShipFighterTemplate.newDesign(this);
        ShipDesign design = NewShipTemplate.newFighterDesign(this);
        design.mission(ShipDesign.FIGHTER);
        design.maxUnusedTurns(OBS_FIGHTER_TURNS);
        return design;
    }
    @Override
    public ShipDesign newBomberDesign(int size) {
    //    ShipDesign design = ShipBomberTemplate.newDesign(this);
        ShipDesign design = NewShipTemplate.newBomberDesign(this);
        design.mission(ShipDesign.BOMBER);
        design.maxUnusedTurns(OBS_BOMBER_TURNS);
        return design;
    }
    @Override
    public ShipDesign newDestroyerDesign(int size) {
        ShipDesign design = NewShipTemplate.newDestroyerDesign(this);
        design.mission(ShipDesign.DESTROYER);
        design.maxUnusedTurns(OBS_DESTROYER_TURNS);
        return design;
    }
    @Override
    public ShipSpecialColony bestColonySpecial() {
        ShipSpecialColony bestSpecial = null;
        boolean ignoreEnv = empire.ignoresPlanetEnvironment();

        for (ShipSpecial spec : empire.shipLab().specials()) {
            if (spec.isColonySpecial()) {
                ShipSpecialColony colSpec = (ShipSpecialColony) spec;
                if (bestSpecial == null)
                    bestSpecial = colSpec;
                else if (ignoreEnv) {
                    if (bestSpecial.cost() >  colSpec.cost())  // silicoids measure by cost
                        bestSpecial = colSpec;
                }
                else {
                    if (bestSpecial.tech().hostilityAllowed() < colSpec.tech().hostilityAllowed())
                        bestSpecial = colSpec;
                }
            }
        }
        return bestSpecial;
    }
    @Override
    public float fightingAdapted(ShipDesign d)
    {
        if(d == null)
            return 0;
        float totalWeaponSpace = 0;
        float totalFightingSpace = 0;
        for (int i=0; i<maxWeapons(); i++)
        {
            totalWeaponSpace += d.weapon(i).space(d) * d.wpnCount(i);
            if(!d.weapon(i).groundAttacksOnly())
                totalFightingSpace += d.weapon(i).space(d) * d.wpnCount(i);
        }
        float fAd = 0;
        if(totalWeaponSpace > 0)
            fAd = totalFightingSpace / totalWeaponSpace;
        if(d.hasColonySpecial())
            fAd = totalFightingSpace / d.spaceUsed();
        return fAd;
    }
    @Override
    public float bombingAdapted(ShipDesign d)
    {
        if(d == null)
            return 0;
        float totalWeaponSpace = 0;
        float totalBombingSpace = 0;
        for (int i=0; i<maxWeapons(); i++)
        {
            totalWeaponSpace += d.weapon(i).space(d) * d.wpnCount(i);
            if(d.weapon(i).groundAttacksOnly())
                totalBombingSpace += d.weapon(i).space(d) * d.wpnCount(i);
        }
        float bAd = 0;
        if(totalWeaponSpace > 0)
            bAd = totalBombingSpace / totalWeaponSpace;
        return bAd;
    }
    @Override
    public boolean wantHybrid()
    {
        if(empire.generalAI().defenseRatio() == 1.0 || empire.generalAI().defenseRatio() < 0.5)
            return false;
        return true;
    } 
    public void ScrapDesign(ShipDesign d)
    {
        //System.out.print("\n"+empire.name()+" "+d.name()+" is scrapped.");
        if(lab().canScrapADesign())
            lab().scrapDesign(d);
    }
    @Override
    public boolean MaintenanceLimitReached(ShipDesign d)
    {
        boolean reached = false;
        if(empire.totalFleetCost() > 0)
        {
            float maintenancePercentage = galaxy().ships.shipDesignCount(empire.id, d.id()) * d.cost() / empire.totalFleetCost();
            float percentageOfMaxMaintenance = min(1, empire.shipMaintCostPerBC() / empire.fleetCommanderAI().maxShipMaintainance());
            float slotsForCombat = 4;
            if(empire.shipLab().needScouts)
                slotsForCombat--;
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+d.name()+" maintenancePercentage: "+maintenancePercentage+" percentageOfMaxMaintenance: "+percentageOfMaxMaintenance+" combined: "+maintenancePercentage * percentageOfMaxMaintenance +" / "+1.0f / slotsForCombat);
            if(maintenancePercentage * percentageOfMaxMaintenance > 1.0f / slotsForCombat)
                reached = true;
        }
        return reached;
    }
    @Override
    public ShipDesign BestDesignToScout()
    {
        ShipDesignLab lab = lab();
        ShipDesign scout = null;
        float cheapestCost = Float.MAX_VALUE;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            float currCost = d.cost();
            if(d.range() == empire.scoutRange())
                currCost /= 2;
            currCost /= d.warpSpeed();
            if(currCost < cheapestCost)
            {
                cheapestCost = currCost;
                scout = d;
            }
        }
        return scout;
    }
    @Override
    public ShipDesign BestDesignToRepell()
    {
        ShipDesignLab lab = lab();
        ShipDesign repeller = null;
        float cheapestCost = Float.MAX_VALUE;
        float highestCount = 0;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            if(fightingAdapted(d) == 0 || d.hasColonySpecial())
                continue;
            if(shipCounts[d.id()] > highestCount)
            {
                highestCount = shipCounts[d.id()];
                repeller = d;
                cheapestCost = Float.MAX_VALUE;
            }
            if(shipCounts[d.id()] == highestCount)
            {
                if(d.cost() < cheapestCost)
                {
                    cheapestCost = d.cost();
                    repeller = d;
                }
            }
        }
        return repeller;
    }
    @Override
    public ShipDesign BestDesignToFight()
    {
        ShipDesignLab lab = lab();
        ShipDesign fighter = null;
        float fightScore = 0;
        int lowestCount = Integer.MAX_VALUE;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            float fightingAdapted = fightingAdapted(d);
            float score = fightingAdapted * (1 - d.availableSpace() / d.totalSpace()) * d.warpSpeed();
            if(fightingAdapted > 0.5f)
            {
                score = (fightingAdapted + bombingAdapted(d)) * (1 - d.availableSpace() / d.totalSpace()) * d.warpSpeed();
            }
            //System.out.print("\n"+empire.name()+" BestDesignToFight "+d.name()+" score: "+score);
            if(score > fightScore)
            {
                fightScore = score;
                fighter = d;
                lowestCount = Integer.MAX_VALUE;
            }
            if(score > 0 && score == fightScore)
            {
                int count = galaxy().ships.shipDesignCount(empire.id, d.id());
                if(count < lowestCount)
                {
                    lowestCount = count;
                    fighter = d;
                }
            }
        }
        return fighter;
    }
    @Override
    public ShipDesign BestDesignToBomb()
    {
        ShipDesignLab lab = lab();
        ShipDesign bomber = null;
        float bombScore = 0;
        int lowestCount = Integer.MAX_VALUE;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            float score = 0;
            //Any hybrid with bombs should be preferred to a bad pure bomber
            if(bombingAdapted(d) > 0)
                score = (fightingAdapted(d) + bombingAdapted(d)) * (1 - d.availableSpace() / d.totalSpace()) * d.warpSpeed();
            if(score > bombScore)
            {
                bombScore = score;
                bomber = d;
                lowestCount = Integer.MAX_VALUE;
            }
            if(score > 0 && score == bombScore)
            {
                int count = galaxy().ships.shipDesignCount(empire.id, d.id());
                if(count < lowestCount)
                {
                    lowestCount = count;
                    bomber = d;
                }
            }
        }
        return bomber;
    }
    @Override
    public ShipDesign BestDesignToColonize()
    {
        ShipDesignLab lab = lab();
        ShipDesign colonizer = null;
        float colScore = 0;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            if(!d.hasColonySpecial())
                continue;
            float score = d.warpSpeed() * d.range() * d.colonySpecial().tech().level();
            score *= 1 + fightingAdapted(d);
            score /= d.size() + 1;
            if(score > colScore)
            {
                colScore = score;
                colonizer = d;
            }
        }
        return colonizer;
    }
    private void MarkObsolete()
    {
        ShipDesignLab lab = lab();
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            //System.out.print("\n"+empire.name()+" cheking "+d.name());
            if(d.obsolete())
                continue;
            if(d == BestDesignToColonize())
                continue;
            if(d == BestDesignToFight())
                continue;
            if(d == BestDesignToBomb() && !wantHybrid())
                continue;
            if(d == BestDesignToScout() && empire.shipLab().needScouts)
                continue;
            if(d == BestDesignToRepell() && (empire.generalAI().needScoutRepellers() || !empire.hasAnyContact()))
                continue;
            //System.out.print("\n"+empire.name()+" "+d.name()+" is marked obsolete.");
            d.obsolete(true);
        }
    }
    ShipWeapon MainWeapon(ShipDesign d)
    {
        ShipWeapon weaponWithMostSpace = null;
        float highestWeaponSpace = 0;
        for (int i=0; i<maxWeapons(); i++)
        {
            float weaponspace = d.weapon(i).space(d) * d.wpnCount(i);
            if(weaponspace > highestWeaponSpace)
            {
                highestWeaponSpace = weaponspace;
                weaponWithMostSpace = d.weapon(i);
            }
        }
        return weaponWithMostSpace;
    }
}
