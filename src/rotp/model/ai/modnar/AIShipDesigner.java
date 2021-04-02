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
package rotp.model.ai.modnar;

import java.util.List;
import rotp.model.ai.interfaces.ShipDesigner;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipSpecialColony;
import rotp.model.ships.ShipWeapon;
import rotp.util.Base;

public class AIShipDesigner implements Base, ShipDesigner {
    private static final int OBS_DESTROYER_TURNS = 10; // modnar: reduce obs_turns
    private static final int OBS_FIGHTER_TURNS = 8; // modnar: reduce obs_turns
    private static final int OBS_BOMBER_TURNS = 9; // modnar: reduce obs_turns
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
        ShipDesignLab lab = lab();
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign d = lab.design(slot);
            if (d.obsolete()) {
                d.remainingLife--;
                // if remainingLife < 0, then this design is not an
                // active design waiting to be scrapped...it's a slot that
                // needs to be freed up
                if (d.remainingLife() < 0) {
                    if (!lab.slotInUse(slot)) {
                        log("Empire: "+empire.name()+ "  Scrapping obsolete design: "+d.name()+"  in slot:"+slot);
                        lab.scrapDesign(d);
                    }
                }
            }
        }
    }
    public void updateScoutDesign() {
        ShipDesignLab lab = lab();
        
        // if we are not using scouts anymore, quit
        if (!lab.needScouts)
            return;
        
        ShipDesign currDesign = lab.scoutDesign();
        int currSlot = currDesign.id();
        if (currDesign.engine() == lab.fastestEngine())
            return;

        ShipDesign newScout = newScoutDesign();
        if (newScout.matchesDesign(currDesign))
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
        boolean extendedFuelNeeded = !lab.needColonyShips && lab.needExtendedColonyShips;

        ShipDesign newDesign = newColonyDesign(weaponsNeeded, extendedFuelNeeded);

        // if currDesign is obsolete, replace it immediately with new design
        if (currDesign.obsolete() && (currDesign.remainingLife() < 1)) {
            lab.scrapDesign(currDesign);
            log("Replacing obsolete colony design");
            lab.setColonyDesign(newDesign, currSlot);
            return;            
        }   

        if (newDesign.matchesDesign(currDesign))
            return;
        
        // mark existing design obsolete. this will notify the ai fleet commander
        // to not issue colonization orders until the design is replaced and allow
        // existing ships in transit to finish. This also starts a countdown timer
        // to replace it anyway
        currDesign.becomeObsolete(OBS_COLONY_TURNS);

        // do we have any colony ships in transit but not retreating.
        // if not, replace the design
        List<ShipFleet> colonyFleets = galaxy().ships.inTransitNotRetreatingFleets(empire.id, currDesign.id());
        if (colonyFleets.isEmpty()) {
            lab.scrapDesign(currDesign);
            log("No more colony ships in transit: updating colony design");
            lab.setColonyDesign(newDesign, currSlot);
            return;            
        }
    }
    public void updateBomberDesign() {
        ShipDesignLab lab = lab();
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = lab.bomberDesign();
        int currSlot = currDesign.id();
        
        // if we don't have any faster engines
        if (currDesign.engine() == lab.fastestEngine()) {
            // modnar: change this check to 15%, remove absolute free space check
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.15f)) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better
                return;
            }
        }
        
        // find best hypothetical design vs current targets
        ShipDesign newDesign = newBomberDesign(currDesign.size());
        
        if (newDesign.matchesDesign(currDesign))
            return;
        
        // modnar: use combined engine, shield, attack level, armor, and damage/BC to justify switching
        // re-ordered to always check first
        // one level increase in engine == ~1.22x (from warp-1 to warp 2), ~1.12x (from warp-3 to warp 4), ~1.07x (from warp-6 to warp 7)
        // one level increase in shield or attack level == 1.1x
        // one level increase in armor == ~1.22x to ~1.07x
        float engineImprv = (float) Math.sqrt( (newDesign.warpSpeed() + 1) / (currDesign.warpSpeed() + 1) );
        float shieldImprv = (float) ( 1.0f + (newDesign.shieldLevel() - currDesign.shieldLevel())/10.0f );
        float hitChanceImprv = (float) ( 1.0f + (newDesign.attackLevel() - currDesign.attackLevel())/10.0f );
        float armorImprv = (float) Math.sqrt(newDesign.hits() / currDesign.hits());
        
        // modnar: factor in cost with firepower
        // modnar: NewShipTemplate.perTurnDamage was already done in NewShipTemplate to ensure better damage
        // use slightly different damage measure for balance
        float oppShield = lab.bestEnemyPlanetaryShieldLevel();
        float newDmgPerBC = newDesign.firepower(oppShield) / newDesign.cost();
        float currDmgPerBC = currDesign.firepower(oppShield) / currDesign.cost();
        float dmgRatio = newDmgPerBC / currDmgPerBC;
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        // modnar: adjust upgradeThreshold value
        float upgradeThreshold = empire.atWar() ? 1.5f : 1.25f;
        float upgradeChance = dmgRatio * engineImprv * shieldImprv * hitChanceImprv * armorImprv;
        if (upgradeChance < upgradeThreshold)
            return;
        
        // modnar: after passing combined check, mark current design obsolete
        // mark existing design obsolete
        currDesign.becomeObsolete(OBS_BOMBER_TURNS);
        
        // if we have very few fighters actually in use, go ahead and
        // scrap/replace now
        float bcValue = currDesign.cost()*shipCounts[currDesign.id()];
        // modnar: scrap easier, 1/2 turn of total Empire production
        float civProd = empire.totalPlanetaryProduction();
        boolean easyToReplace = bcValue <= 0.5f*civProd;
        
        if (easyToReplace) {
            // modnar: setting the design name here seems to cause the blank ship name issue seen, comment out
            //newDesign.name(currDesign.name());
            //if  (newDesign.size() == currDesign.size())
            //    newDesign.iconKey(currDesign.iconKey());
            lab.scrapDesign(currDesign);
            log("Bomber easy to replace");
            lab.setBomberDesign(newDesign, currSlot);
            return;
        }
        
        // if currDesign is obsolete, replace it immediately
        if (currDesign.obsolete() && (currDesign.remainingLife() < 1)) {
            lab.scrapDesign(currDesign);
            log("Replacing obsolete bomber design");
            lab.setBomberDesign(newDesign, currSlot);
            return;            
        }
        
        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // if there was no slot available for the new design, consider 
        // auto-upgrading anyway if we are not at war. We want to avoid
        // unnecessarily using the empty slot if we don't have to
        if ((slot < 0) && empire().enemies().isEmpty()) {
            lab.scrapDesign(currDesign);
            log("No enemies: Bomber upgrade chance:"+upgradeChance);
            lab.setBomberDesign(newDesign, currSlot);
            return;
        }
        
        // if there is a slot available, use it for the new design
        if (slot > 0) {
            log("Slot available: Bomber upgrade chance:"+upgradeChance);
            lab.setBomberDesign(newDesign, slot);
            return;
        }
    }
    public void updateFighterDesign() {
        ShipDesignLab lab = lab();
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = lab.fighterDesign();
        int currSlot = currDesign.id();
        
        // if we don't have any faster engines
        if (currDesign.engine() == lab.fastestEngine()) {
            // modnar: change this check to 15%, remove absolute free space check
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.15f)) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better
                return;
            }
        }
        
        // ShipFighterTemplate.setPerTurnDamage(currDesign, empire());
        // NewShipTemplate.setPerTurnShipDamage(currDesign, empire()); // modnar: not needed
        
        // find best hypothetical design vs current targets
        ShipDesign newDesign = newFighterDesign(currDesign.size());
        
        if (currDesign.matchesDesign(newDesign))
            return;
        
        // modnar: use combined engine, shield, attack level, armor, and damage/BC to justify switching
        // re-ordered to always check first
        // one level increase in engine == ~1.22x (from warp-1 to warp 2), ~1.12x (from warp-3 to warp 4), ~1.07x (from warp-6 to warp 7)
        // one level increase in shield or attack level == 1.1x
        // one level increase in armor == ~1.22x to ~1.07x
        float engineImprv = (float) Math.sqrt( (newDesign.warpSpeed() + 1) / (currDesign.warpSpeed() + 1) );
        float shieldImprv = (float) ( 1.0f + (newDesign.shieldLevel() - currDesign.shieldLevel())/10.0f );
        float hitChanceImprv = (float) ( 1.0f + (newDesign.attackLevel() - currDesign.attackLevel())/10.0f );
        float armorImprv = (float) Math.sqrt(newDesign.hits() / currDesign.hits());
        
        // modnar: use firepowerAntiShip, factor in cost
        // modnar: NewShipTemplate.perTurnDamage was already done in NewShipTemplate to ensure better damage
        // use slightly different damage measure for balance
        float oppShield = lab.bestEnemyShieldLevel();
        float newDmgPerBC = newDesign.firepowerAntiShip(oppShield) / newDesign.cost();
        float currDmgPerBC = currDesign.firepowerAntiShip(oppShield) / currDesign.cost();
        float dmgRatio = newDmgPerBC / currDmgPerBC;
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        // modnar: adjust upgradeThreshold value
        float upgradeThreshold = empire.atWar() ? 1.5f : 1.25f;
        float upgradeChance = dmgRatio * engineImprv * shieldImprv * hitChanceImprv * armorImprv;
        if (upgradeChance < upgradeThreshold)
            return;
        
        // modnar: after passing combined check, mark current design obsolete
        // mark existing design obsolete
        currDesign.becomeObsolete(OBS_FIGHTER_TURNS);
        
        // if we have very few fighters actually in use, go ahead and
        // scrap/replace now
        float bcValue = currDesign.cost()*shipCounts[currDesign.id()];
        // modnar: scrap easier, 1/2 turn of total Empire production
        float civProd = empire.totalPlanetaryProduction();
        boolean easyToReplace = bcValue <= 0.5f*civProd;
        
        if (easyToReplace) {
            // modnar: setting the design name here seems to cause the blank ship name issue seen, comment out
            //newDesign.name(currDesign.name());
            //if  (newDesign.size() == currDesign.size())
            //    newDesign.iconKey(currDesign.iconKey());
            lab.scrapDesign(currDesign);
            log("Fighter easy to replace");
            lab.setFighterDesign(newDesign, currSlot);
            return;
        }
        
        // if currDesign is obsolete, replace it immediately with new design
        if (currDesign.obsolete() && (currDesign.remainingLife() < 1)) {
            lab.scrapDesign(currDesign);
            log("Replacing obsolete fighter design");
            lab.setFighterDesign(newDesign, currSlot);
            return;            
        }
        
        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // if there was no slot available for the new design, consider 
        // auto-upgrading anyway if we are not at war. We want to avoid
        // unnecessarily using the empty slot if we don't have to
        if ((slot < 0) && empire().enemies().isEmpty()) {
            lab.scrapDesign(currDesign);
            log("No enemies: Fighter upgrade chance:"+upgradeChance);
            lab.setFighterDesign(newDesign, currSlot);
            return;
        }
        
        // if there is a slot available, use it for the new design
        if (slot > 0) {
            log("Slot available: Fighter upgrade chance:"+upgradeChance);
            lab.setFighterDesign(newDesign, slot);
            return;
        }
    }
    public void updateDestroyerDesign() {
        ShipDesignLab lab = lab();
        
        // recalculate current design's damage vs. current targets
        ShipDesign currDesign = lab.destroyerDesign();
        int currSlot = currDesign.id();
        
        // if we don't have any faster engines
        if (currDesign.engine() == lab.fastestEngine()) {
            // modnar: change this check to 15%, remove absolute free space check
            // and if the design has less than 10% free space or has less than 25/50/75/100 free space, we assume the redesign has no sense
            // 25/50/75/100 may be a too straight-line set of values
            if ((currDesign.availableSpace()/(currDesign.totalSpace()) < 0.15f)) {
                // we're fairly certain AI packed the ship before and if the current modules haven't shrinked enough,
                // that means we don't have enough new tech to make the new design markedly better
                return;
            }
        }
        
        // ShipDestroyerTemplate.setPerTurnDamage(currDesign, empire());
        // NewShipTemplate.setPerTurnShipDamage(currDesign, empire); // modnar: not needed
        
        // find best hypothetical design vs current targets
        ShipDesign newDesign = newDestroyerDesign(currDesign.size());
        
        if (currDesign.matchesDesign(newDesign)) 
            return;
        
        // modnar: use combined engine, shield, attack level, armor, and damage/BC to justify switching
        // re-ordered to always check first
        // one level increase in engine == ~1.22x (from warp-1 to warp 2), ~1.12x (from warp-3 to warp 4), ~1.07x (from warp-6 to warp 7)
        // one level increase in shield or attack level == 1.1x
        // one level increase in armor == ~1.22x to ~1.07x
        float engineImprv = (float) Math.sqrt( (newDesign.warpSpeed() + 1) / (currDesign.warpSpeed() + 1) );
        float shieldImprv = (float) ( 1.0f + (newDesign.shieldLevel() - currDesign.shieldLevel())/10.0f );
        float hitChanceImprv = (float) ( 1.0f + (newDesign.attackLevel() - currDesign.attackLevel())/10.0f );
        float armorImprv = (float) Math.sqrt(newDesign.hits() / currDesign.hits());
        
        // modnar: use firepowerAntiShip, factor in cost
        // modnar: NewShipTemplate.perTurnDamage was already done in NewShipTemplate to ensure better damage
        // use slightly different damage measure for balance
        float oppShield = lab.bestEnemyShieldLevel();
        float newDmgPerBC = newDesign.firepowerAntiShip(oppShield) / newDesign.cost();
        float currDmgPerBC = currDesign.firepowerAntiShip(oppShield) / currDesign.cost();
        float dmgRatio = newDmgPerBC / currDmgPerBC;
        
        // switch to new design when damage is floatd
        // more willing to upgrade when not at war
        // modnar: adjust upgradeThreshold value
        float upgradeThreshold = empire.atWar() ? 1.5f : 1.25f;
        float upgradeChance = dmgRatio * engineImprv * shieldImprv * hitChanceImprv * armorImprv;
        if (upgradeChance < upgradeThreshold)
            return;
        
        // modnar: after passing combined check, mark current design obsolete
        // mark existing design obsolete
        currDesign.becomeObsolete(OBS_DESTROYER_TURNS);
        
        // if we have very few destroyers actually in use, go ahead and
        // scrap/replace now
        float bcValue = currDesign.cost()*shipCounts[currDesign.id()];
        // modnar: scrap easier, 1 turn of total Empire production
        float civProd = empire.totalPlanetaryProduction();
        boolean easyToReplace = bcValue <= 1.0f*civProd;
        
        if (easyToReplace) {
            // modnar: setting the design name here seems to cause the blank ship name issue seen, comment out
            //newDesign.name(currDesign.name());
            //if  (newDesign.size() == currDesign.size())
            //    newDesign.iconKey(currDesign.iconKey());
            lab.scrapDesign(currDesign);
            log("Destroyer easy to replace");
            lab.setDestroyerDesign(newDesign, currSlot);
            return;
        }
        
        // if currDesign is obsolete, replace it immediately with new design
        if (currDesign.obsolete() && (currDesign.remainingLife() < 1)) {
            lab.scrapDesign(currDesign);
            log("Replacing obsolete destroyer design");
            lab.setDestroyerDesign(newDesign, currSlot);
            return;            
        }
        
        // check for an available slot for the new design
        int slot = lab.availableDesignSlot();
        
        // if there was no slot available for the new design, consider 
        // auto-upgrading anyway if we are not at war. We want to avoid
        // unnecessarily using the empty slot if we don't have to
        if ((slot < 0) && empire().enemies().isEmpty()) {
            lab.scrapDesign(currDesign);
            log("No enemies: Destroyer upgrade chance:"+upgradeChance);
            lab.setDestroyerDesign(newDesign, currSlot);
            return;
        }
        
        // if there is a slot available, use it for the new design
        if (slot > 0) {
            log("Slot available: Destroyer upgrade chance:"+upgradeChance);
            lab.setDestroyerDesign(newDesign, slot);
            return;
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

        // AIs will always put 1 beam weapon on their colony ships if AI contact made
        if (weaponNeeded) {
            ShipWeapon bestWpn = lab.bestUnlimitedShotWeapon(design, 1);
            if (bestWpn != null)
                design.addWeapon(bestWpn, 1);
        }

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
        else if (maxProd >= 700) // modnar: change from 300, keep fighters smaller
            maxSize = ShipDesign.LARGE;
        else if (maxProd >= 300) // modnar: change from 60 (!), keep fighters smaller
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
        if (maxProd >= 1500) // modnar: change from 1000, keep bombers smaller
            maxSize = ShipDesign.HUGE;
        else if (maxProd >= 450) // modnar: change from 200, keep bombers smaller
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
        if (maxProd >= 800) // modnar: change from 1000, pump out HUGE destroyers sooner, 800 is around soil/terraform+40/robo-4
            maxSize = ShipDesign.HUGE;
        else if (maxProd >= 350) // modnar: change from 200, keep destroyer smaller in beginning
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
