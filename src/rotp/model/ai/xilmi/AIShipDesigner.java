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

import java.util.List;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
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
            countdownObsoleteDesigns();
            // designs are updated in a specific order in order to prioritize
            // use of empty design slots
            updateFighterDesign();
            updateDestroyerDesign();
            updateBomberDesign();
            updateColonyDesign();
            updateScoutDesign();
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
                        }
                    }
                }
            }
        }
        return bestDesign;
    }
    private void countdownObsoleteDesigns() {
        if(((empire.shipMaintCostPerBC() > (empire.fleetCommanderAI().maxShipMaintainance() * 1.5) && !empire.atWar() && !empire.fleetCommanderAI().inExpansionMode()))
                || empire.netIncome() <= 0)
        {
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
                else
                {
                    keepScore = 1 - d.availableSpace()/d.totalSpace();
                }
                if(d.isFighter())
                {
                    float currentDamageNoShields = lab.fighterDesign().firepowerAntiShip(0);
                    float currentDamageShields = lab.fighterDesign().firepowerAntiShip(empire.bestEnemyShieldLevel());
                    float obsoleteDamageNoShields = d.firepowerAntiShip(0);
                    float obsoleteDamageShields = d.firepowerAntiShip(empire.bestEnemyShieldLevel());
                    float currentPenetrate = currentDamageShields / currentDamageNoShields;
                    float obsoletePenetrate = obsoleteDamageShields / obsoleteDamageNoShields;
                    if(currentPenetrate > 0)
                    {
                        keepScore *= obsoletePenetrate / currentPenetrate;
                    }
                    else
                    {
                        keepScore = 0;
                    }
                }
                if(d.isBomber())
                {
                    float currentDamageNoShields = lab.bomberDesign().firepower(0);
                    float currentDamageShields = lab.bomberDesign().firepower(empire.bestEnemyPlanetaryShieldLevel());
                    float obsoleteDamageNoShields = d.firepower(0);
                    float obsoleteDamageShields = d.firepower(empire.bestEnemyPlanetaryShieldLevel());
                    float currentPenetrate = currentDamageShields / currentDamageNoShields;
                    float obsoletePenetrate = obsoleteDamageShields / obsoleteDamageNoShields;
                    if(currentPenetrate > 0)
                    {
                        keepScore *= obsoletePenetrate / currentPenetrate;
                    }
                    else
                    {
                        keepScore = 0;
                    }
                }
                keepScore *= shipCounts[d.id()] * d.cost();
                //we can scrap all that we don't need at all
                if(keepScore == 0)
                {
                    lab.scrapDesign(d);
                    //if we do this, we don't have to scrap the design with the lowest keepscore anymore because a slot is now free
                    shouldScrap = false;
                }
                else if(keepScore < lowestKeepScore)
                {
                    //System.out.print("\n"+empire.name()+" "+d.name()+" keepScore: "+keepScore);
                    lowestKeepScore = keepScore;
                    designToScrap = d;
                }
            }
        }
        if(designToScrap != null && shouldScrap)
        {
            //System.out.print("\n"+empire.name()+" "+designToScrap.name()+" is scrapped.");
            lab.scrapDesign(designToScrap);
        }
    }

    public void updateScoutDesign() {
        ShipDesignLab lab = lab();
        // if we are not using scouts anymore, quit
        if (!lab.needScouts)
        {
            //ail: free up the slot for other designs
            ShipDesign currDesign = lab.scoutDesign();
            if(currDesign.active() && currDesign.isScout())
            {
                lab.scrapDesign(currDesign);
            }
            return;
        }
        
        ShipDesign currDesign = lab.scoutDesign();
        int currSlot = currDesign.id();
        if (currDesign.engine() == lab.fastestEngine() && currDesign.active())
            return;

        ShipDesign newScout = newScoutDesign();
        if (newScout.matchesDesign(currDesign, false) && currDesign.active())
            return;

        // NEW DESIGN IS BETTER THAN CURRENT
        // immediately replace scout design, don't bother with obsoleting them
        lab.scrapDesign(currDesign);
        lab.setScoutDesign(newScout, currSlot);
    }
    public void updateColonyDesign() {
        ShipDesignLab lab = lab();
        ShipDesign currDesign = lab.colonyDesign();
        int currSlot = currDesign.id();
        
        // weapons needed on colony ships if we've made AI contact
        boolean weaponsNeeded = !empire.contactedEmpires().isEmpty();
        // current design is "properlyArmed" if it is armed &weapons needed or unarmed & weapons unneeded
        //always try to make extended, if possible, additional-cost is worth it and once it fits on large, the costs won't matter much anyways
        boolean extendedFuelNeeded = true;

        ShipDesign newDesign = newColonyDesign(weaponsNeeded, extendedFuelNeeded);

        if (currDesign.matchesDesign(newDesign, true) && currDesign.active())
        {
            return;
        }
        
        boolean easyToReplace = shipCounts[currDesign.id()] < 1;
        
        if (easyToReplace) {
            lab.scrapDesign(currDesign);
            lab.setColonyDesign(newDesign, currSlot);
            return;
        }
        
        int slot = lab.availableDesignSlot();
        
        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            lab.setColonyDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_COLONY_TURNS);
            return;
        }
        else
        {
            //if there is no slot available I push the old one to obsolete so a new slot will be freed up soon
            currDesign.becomeObsolete(OBS_COLONY_TURNS);
            scrapWorstDesign(false);
            slot = lab.availableDesignSlot();
            lab.setColonyDesign(newDesign, slot);
        }
    }
    public void updateBomberDesign() {
        ShipDesignLab lab = lab();
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = lab.bomberDesign();
        
        // if we don't have any faster engines
        if (currDesign.engine() == lab.fastestEngine() && currDesign.active()) {
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.1f)) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better            
                currDesign.remainingLife++;
                return;
            }
        }
        //System.out.print("\n"+empire.name()+" should design new bomber since old one has "+currDesign.availableSpace()/currDesign.totalSpace()+"free space");

        int currSlot = currDesign.id();
     
        // find best hypothetical design vs current targets
        ShipDesign newDesign = newBomberDesign(currDesign.size());

        if (currDesign.matchesDesign(newDesign, false) && currDesign.active())
        {
            return;
        }

        boolean easyToReplace = shipCounts[currDesign.id()] < 1;
        
        if (easyToReplace) {
            lab.scrapDesign(currDesign);
            log("Bomber easy to replace");
            lab.setBomberDesign(newDesign, currSlot);
            return;
        }
            
        // WE HAVE BOMBERS IN USE... VERIFY THIS UPGRADE JUSTIFIES SWITCHING OVER
        boolean betterEngine = (newDesign.engine().warp() > currDesign.engine().warp());
        boolean betterArmor = (newDesign.armor().sequence() > currDesign.armor().sequence());

        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        float upgradeThreshold = empire.atWar() ? 1.5f : 1.25f;
        
        float upgradeChance = 1 + currDesign.availableSpace() / currDesign.totalSpace();
        
        //assume that the space not used for weapons is well used for other stuff and thus only look at firepower from space for weapons
        float currentWpnSpc = 0.0f;
        float newWpnSpc = 0.0f;
        for (int i=0; i<maxWeapons(); i++)
        {
            if(!currDesign.weapon(i).groundAttacksOnly())
                continue;
            currentWpnSpc += (currDesign.wpnCount(i) * currDesign.weapon(i).space(currDesign));
            newWpnSpc += (newDesign.wpnCount(i) * newDesign.weapon(i).space(newDesign));
        }
        
        float currentDPBC = currDesign.firepower(empire.bestEnemyPlanetaryShieldLevel()) / currentWpnSpc;
        float newDPBC = newDesign.firepower(empire.bestEnemyPlanetaryShieldLevel()) / newWpnSpc;
        if(currentDPBC > 0)
        {
            upgradeChance *= newDPBC / currentDPBC;
        }
        else if(newDPBC > 0)
        {
            upgradeChance *= 2;
        }
        
        //System.out.print("\n"+galaxy().currentYear()+" "+empire.name()+" Bomber upgrade "+currDesign.name()+" val: "+upgradeChance+" DPBC: "+newDPBC / currentDPBC+" better-Engine: "+betterEngine+" betterArmor: "+betterArmor);
        
        if (!betterEngine && !betterArmor && (upgradeChance < upgradeThreshold) )
            return;
        
        //System.out.print("\n"+empire.name()+" designed new bomber which is "+upgradeChance+" better and should go to slot: "+slot);

        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            log("Slot available: Bomber upgrade chance:"+upgradeChance);
            lab.setBomberDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_BOMBER_TURNS);
            return;
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
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = lab.fighterDesign();
        // if we don't have any faster engines
        if (currDesign.engine() == lab.fastestEngine() && currDesign.active()) {
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.1f)) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better
                currDesign.remainingLife++;
                return;
            }
        }

        int currSlot = currDesign.id();
        
        // find best hypothetical design vs current targets
        ShipDesign newDesign = newFighterDesign(currDesign.size());
      
        if (currDesign.matchesDesign(newDesign, false) && currDesign.active())
        {
            return;
        }

        // if we have very few fighters actually in use, go ahead and
        // scrap/replace now
        boolean easyToReplace = shipCounts[currDesign.id()] < 1;
        
        if (easyToReplace) {
            lab.scrapDesign(currDesign);
            log("Fighter easy to replace");
            lab.setFighterDesign(newDesign, currSlot);
            return;
        }
        
        // WE HAVE FIGHTERS IN USE... VERIFY THIS UPGRADE JUSTIFIES SWITCHING OVER

        // factor in speed improvements when determining if new design is better
        // i.e. wrp 5, 100 dmg is better than wrp 3, 120 dmg
        boolean betterEngine = (newDesign.engine().warp() > currDesign.engine().warp());
        boolean betterArmor = (newDesign.armor().sequence() > currDesign.armor().sequence());

        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        float upgradeThreshold = empire.atWar() ? 1.5f : 1.25f;
        
        float upgradeChance = 1 + currDesign.availableSpace() / currDesign.totalSpace();

        float currentWpnSpc = 0.0f;
        float newWpnSpc = 0.0f;
        for (int i=0; i<maxWeapons(); i++)
        {
            currentWpnSpc += (currDesign.wpnCount(i) * currDesign.weapon(i).space(currDesign));
            newWpnSpc += (newDesign.wpnCount(i) * newDesign.weapon(i).space(newDesign));
        }
        float currentDPBC = currDesign.firepowerAntiShip(empire.bestEnemyShieldLevel()) / currentWpnSpc;
        float newDPBC = newDesign.firepowerAntiShip(empire.bestEnemyShieldLevel()) / newWpnSpc;
        if(currentDPBC > 0)
        {
            upgradeChance *= newDPBC / currentDPBC;
        }
        else if(newDPBC > 0)
        {
            upgradeChance *= 2;
        }
        
        //System.out.print("\n"+galaxy().currentYear()+" "+empire.name()+" Fighter upgrade "+currDesign.name()+" val: "+upgradeChance+" DPBC: "+newDPBC / currentDPBC+" better-Engine: "+betterEngine+" betterArmor: "+betterArmor);
        
        //System.out.print("\n"+empire.name()+" designed new fighter which is "+upgradeChance+" better and should go to slot: "+slot);
        if (!betterEngine && !betterArmor && (upgradeChance < upgradeThreshold) )
            return;

        // if there is a slot available, use it for the new design
        if (slot >= 0) {
            log("Slot available: Fighter upgrade chance:"+upgradeChance);
            lab.setFighterDesign(newDesign, slot);
            currDesign.becomeObsolete(OBS_FIGHTER_TURNS);
            return;
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
        //ail: don't use destroyer, just free up the design-slot here for more fighters and bombers
        ShipDesignLab lab = lab();
        
        ShipDesign currDesign = lab.destroyerDesign();
        if(currDesign.active() && currDesign.isDestroyer())
        {
            lab.scrapDesign(currDesign);
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
        lab.iconifyDesign(design);
        return design;
    }
    public ShipDesign newColonyDesign() {
        return newColonyDesign(!empire.contactedEmpires().isEmpty(), false);
    }
    public ShipDesign newColonyDesign(boolean weaponNeeded, boolean extendedRangeNeeded) {
        ShipDesignLab lab = lab();
        ShipDesign design = lab.newBlankDesign(ShipDesign.LARGE);
        design.special(0, bestColonySpecial());
        design.engine(lab.fastestEngine());
        design.mission(ShipDesign.COLONY);
        design.maxUnusedTurns(OBS_COLONY_TURNS);

        // if we don't need regular-range colony ship
        if (extendedRangeNeeded) {
            ShipSpecial prevSpecial = design.special(1);
            ShipSpecial special = lab.specialReserveFuel();
            design.special(1, special);
            design.setSmallestSize();
            if (design.size() > ShipDesign.LARGE)
                design.special(1, prevSpecial);
        }
        design.setSmallestSize();
        //ail: only if we now still have room, we put a weapon on it. Extended range and smaller size is way more important than a weapon without computer
        if (weaponNeeded) {
            ShipWeapon bestWpn = lab.bestUnlimitedShotWeapon(design, 1);
            if (bestWpn != null && design.availableSpace() >= bestWpn.space(design))
                design.addWeapon(bestWpn, 1);
        }
        lab.nameDesign(design);
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
    //    ShipDesign design = ShipDestroyerTemplate.newDesign(this);
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
}
