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
import java.util.SortedMap; 
import java.util.TreeMap;
import java.util.Iterator;

import rotp.model.ai.EnemyShipTarget;
import rotp.model.ai.EnemyColonyTarget;
import rotp.model.ai.interfaces.ShipDesigner;

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.Race;
import rotp.model.empires.ShipView;
import rotp.model.galaxy.StarSystem;

import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import static rotp.model.ships.ShipDesign.maxSpecials;
import static rotp.model.ships.ShipDesign.maxWeapons;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;
import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;
import rotp.util.Base;

public class NewShipTemplate implements Base {
    private static final NewShipTemplate instance = new NewShipTemplate();
    private static final ShipDesign mockDesign = new ShipDesign();

    enum DesignType { FIGHTER, BOMBER, DESTROYER };

    // indices for race shipDesignMods
    public static final int COST_MULT_S = 0;
    public static final int COST_MULT_M = 1;
    public static final int COST_MULT_L = 2;
    public static final int COST_MULT_H = 3;
    public static final int MODULE_SPACE = 4;
    public static final int SHIELD_WEIGHT_FB = 5;
    public static final int SHIELD_WEIGHT_D = 6;
    public static final int ECM_WEIGHT_FD = 7;
    public static final int ECM_WEIGHT_B = 8;
    public static final int MANEUVER_WEIGHT_BD = 9;
    public static final int MANEUVER_WEIGHT_F = 10;
    public static final int ARMOR_WEIGHT_FB = 11;
    public static final int ARMOR_WEIGHT_D = 12;
    public static final int SPECIALS_WEIGHT = 13;
    public static final int SPEED_MATCHING = 14;
    public static final int REINFORCED_ARMOR = 15;
    public static final int BIO_WEAPONS = 16;
    public static final int PREF_PULSARS = 17;
    public static final int PREF_CLOAK = 18;
    public static final int PREF_REPAIR = 19;
    public static final int PREF_INERTIAL = 20;
    public static final int PREF_MISS_SHIELD = 21;
    public static final int PREF_REPULSOR = 22;
    public static final int PREF_STASIS = 23;
    public static final int PREF_STREAM_PROJECTOR = 24;
    public static final int PREF_WARP_DISSIPATOR = 25;
    public static final int PREF_TECH_NULLIFIER = 26;
    public static final int PREF_BEAM_FOCUS = 27;
    

    public static ShipDesign newFighterDesign(ShipDesigner ai) {
        return instance.bestDesign(ai, DesignType.FIGHTER);
    }
    public static ShipDesign newBomberDesign(ShipDesigner ai) {
        return instance.bestDesign(ai, DesignType.BOMBER);
    }
    public static ShipDesign newDestroyerDesign(ShipDesigner ai) {
        return instance.bestDesign(ai, DesignType.DESTROYER);
    }

    private ShipDesign bestDesign(ShipDesigner ai, DesignType role) {
        Race race = ai.empire().dataRace();
        // create a blank design, one for each size. Add the current design as a 5th entry
        ShipDesign[] shipDesigns = new ShipDesign[4];
        for (int i = 0; i<4; i++) 
            shipDesigns[i] = newDesign(ai, role, i); 

        // race's design cost multiplier for each hull size (set in definition.txt file)
        float[] costMultiplier = new float[4];
        costMultiplier[0] = race.shipDesignMods[1];
        costMultiplier[1] = race.shipDesignMods[1];
        costMultiplier[2] = race.shipDesignMods[1];
        costMultiplier[3] = race.shipDesignMods[1];       
        // add another entry for the current design, using the cost multiplier for its size

        // how many ships of each design can we build for virtual tests?
        // use top 5 colonies
        float shipBudgetBC = shipProductionBudget(ai, 5);
        
        SortedMap<Float, ShipDesign> designSorter = new TreeMap<>();
        
        for (int i = 0; i<costMultiplier.length; i++) {
            ShipDesign design = shipDesigns[i];
            // number of whole designs we can build within our budget
            int count = (int) Math.floor(shipBudgetBC / (design.cost() * costMultiplier[i]));
            float score = 0;
            if(count >= 1)
                score = design.spaceUsed() / design.totalSpace();
            if(role.BOMBER == role)
            {
                boolean hasBombs = false;
                for (int j=0; j<maxWeapons(); j++)
                {
                    if(design.weapon(j).groundAttacksOnly())
                    {
                        hasBombs = true;
                        break;
                    }
                }
                if (!hasBombs)
                    score = 0;
            }
            for (int j=0;j<maxSpecials();j++)
                if(!design.special(j).isNone())
                    score *= 1.1;
            designSorter.put(score, design);
        }
        // lastKey is design with greatest damage
        return designSorter.get(designSorter.lastKey()); 
    }

    private ShipDesign newDesign(ShipDesigner ai, DesignType role, int size) {
        ShipDesign d = ai.lab().newBlankDesign(size);
        // name it first so we can use name for reference in debugging
        ai.lab().nameDesign(d);
        // engines are always the priority in MOO1 mechanics
        setFastestEngine(ai, d);
        // battle computers are always the priority in MOO1 mechanics
        setBestBattleComputer(ai, d); 
        
        float totalSpace = d.availableSpace();
        Race race = ai.empire().dataRace();
        float enemyMissilePercentage = 0.0f;
        //ail: looking at the stats of our enemies
        Empire bestVictim = ai.empire().generalAI().bestVictim();
        if(bestVictim != null)
        {
            float nonMissileTotal = 0.0f;
            float missileTotal = 0.0f;
            for(ShipView enemyDesign : bestVictim.shipLab().designHistory())
            {
                if(enemyDesign.design().scrapped())
                {
                    continue;
                }
                for (int i=0; i<maxWeapons(); i++)
                {
                    ShipWeapon weapon = enemyDesign.weapon(i);
                    if(weapon == null)
                        continue;
                    if(weapon.isMissileWeapon())
                    {
                        missileTotal = weapon.cost() * enemyDesign.wpnCount(i);
                    }
                    else
                    {
                        nonMissileTotal = weapon.cost() * enemyDesign.wpnCount(i);
                    }
                }
            }
            if(missileTotal+nonMissileTotal > 0)
            {
                enemyMissilePercentage = missileTotal / (missileTotal + nonMissileTotal);
            }
            if(bestVictim.shipMaintCostPerBC()+bestVictim.missileBaseCostPerBC() > 0)
            {
                enemyMissilePercentage = (enemyMissilePercentage * bestVictim.shipMaintCostPerBC() + bestVictim.missileBaseCostPerBC()) / (bestVictim.shipMaintCostPerBC() + bestVictim.missileBaseCostPerBC());
            }
        }

        // initial separation of the free space left onto weapons and non-weapons/specials
        float moduleSpaceRatio = race.shipDesignMods[MODULE_SPACE];        
        float modulesSpace = totalSpace * moduleSpaceRatio;

        // arbitrary initial weighting of what isn't weapons
        int shieldWeight = 4;
        int ecmWeight = 3;    
        ecmWeight = (int)Math.round(ecmWeight * 2 * enemyMissilePercentage);
        int maneuverWeight = 2;
        int armorWeight = 3; 
        int specialsWeight = 4; 
        boolean sameSpeedAllowed = true; 
        boolean reinforcedArmorAllowed = true; 
        
        // if we have a large ship, let's let the AI use more specials; it may have to differentiate designs more
        if (size >= ShipDesign.LARGE)
            specialsWeight += 1; 

        // the sum of shield, ECM, maneuver and armor weights may be not exactly equal to modulesSpace
        // however, unless it isn't 1.0 or close to it of available space after engines and BC, it doesn't matter
        int weightsSum = shieldWeight + ecmWeight + maneuverWeight + armorWeight + specialsWeight;

        float shieldSpace = modulesSpace * shieldWeight / weightsSum;
        float ecmSpace = modulesSpace * ecmWeight / weightsSum;
        float maneuverSpace = modulesSpace * maneuverWeight / weightsSum;
        float armorSpace = modulesSpace * armorWeight / weightsSum;
        float specialsSpace = modulesSpace * specialsWeight / weightsSum;
        
        // ail: removed leftovers. The impact isn't minor at all. Even at a weight of 0 they usually crammed in the max-level ECM, leaving much less space for weapons
        // branches made in the ugly way for clarity
        // specials will be skipped for smaller hulls in the early game, bringing a bit more allowance to the second fitting
        //ArrayList<ShipSpecial> specials;
        SortedMap<Float, ShipSpecial> specials;
        
        boolean needRange = false;
        boolean boostInertial = false;
        
        for(EmpireView ev : ai.empire().contacts())
        {
            for(ShipView enemyDesign : ev.empire().shipLab().designHistory())
            {
                if(enemyDesign.design().scrapped())
                {
                    continue;
                }
                if(enemyDesign.design().repulsorRange() > 0)
                {
                    needRange = true;
                }
                for (int j=0;j<maxSpecials();j++)
                    if(enemyDesign.design().special(j).createsBlackHole())
                        boostInertial = true;
            }
        }
        
        switch (role) {
            case BOMBER:
                specials = buildSpecialsList(d, ai, enemyMissilePercentage, true, false, boostInertial, size, specialsSpace);
                break;
            case FIGHTER:
            default:
                specials = buildSpecialsList(d, ai, enemyMissilePercentage, false, needRange, boostInertial, size, specialsSpace);
                break;
        }
        
        switch (role) {
            case BOMBER:
                setFittingSpecial(ai, d, specialsSpace, specials);
                setFittingShields(ai, d, shieldSpace);
                setFittingArmor(ai, d, armorSpace, reinforcedArmorAllowed);
                setFittingManeuver(ai, d, maneuverSpace, sameSpeedAllowed);
                setFittingECM(ai, d, ecmSpace);
                break;
            case FIGHTER:
            default:
                setFittingSpecial(ai, d, specialsSpace, specials);
                setFittingArmor(ai, d, armorSpace, reinforcedArmorAllowed);
                setFittingECM(ai, d, ecmSpace);
                setFittingShields(ai, d, shieldSpace);
                setFittingManeuver(ai, d, maneuverSpace, sameSpeedAllowed);
                break;
        }
        
        for (int j=0;j<maxSpecials();j++)
            if(d.special(j).beamRangeBonus() > 0)
                needRange = false;
        
        float firstWeaponSpaceRatio = 0.8f; // bombs for bombers, best weapon for destroyers
        // what's left will be used on non-bombs for bombers, second best weapon for destroyers
        // repeat calls of setOptimalShipCombatWeapon() will result in a weapon from another category (beam, missile, streaming) than already installed
        // fighters will have a single best weapon over all four slots
        
        switch (role) {
            case BOMBER:
                setOptimalWeapon(ai, d, firstWeaponSpaceRatio * d.availableSpace(), 1, false, false, false);
                setOptimalWeapon(ai, d, d.availableSpace(), 3, needRange, true, true); // uses slot 1
                break;
            case FIGHTER:
            default:
                setOptimalWeapon(ai, d, d.availableSpace(), 4, needRange, true, true); // uses slots 0-3
                break;
        }
        ai.lab().iconifyDesign(d);
        for (int i = 0; i <= 2; ++i) {
            if (d.special(i) != null) {
                //System.out.print("\n"+ai.empire().name()+" "+d.name()+" special: "+d.special(i).name());
            }
        }
        return d;
    }
////////////////////////////////////////////////////


// ********** MODULE FITTING FUNCTIONS ********** //

    private void setFastestEngine(ShipDesigner ai, ShipDesign d) {
        d.engine(ai.lab().fastestEngine());
    }

    private void setBestBattleComputer(ShipDesigner ai, ShipDesign d) {
        List<ShipComputer> comps = ai.lab().computers();
        for (int i=comps.size()-1; i >=0; i--) {
            d.computer(comps.get(i));
            if (d.availableSpace() >= 0)
                return;
        }
    }

    private float setFittingManeuver(ShipDesigner ai, ShipDesign d, float spaceAllowed, boolean sameSpeedAllowed) {
        float initialSpace = d.availableSpace();

        for (ShipManeuver manv : ai.lab().maneuvers()) {
            ShipManeuver prevManv = d.maneuver();
            int prevSpeed = d.combatSpeed();
            
            d.maneuver(manv);

            if ((initialSpace - d.availableSpace()) > spaceAllowed) {
                d.maneuver(prevManv);
            }
            else if ((d.combatSpeed() == prevSpeed) && (!sameSpeedAllowed)) {
                d.maneuver(prevManv);
            }
        }
        return (spaceAllowed - (initialSpace - d.availableSpace()));
    }

    private float setFittingArmor(ShipDesigner ai, ShipDesign d, float spaceAllowed, boolean reinforcedArmorAllowed){
        float initialSpace = d.availableSpace();

        boolean foundIt = false;
        List<ShipArmor> armors = ai.lab().armors();
        for (int i=armors.size()-1; (i >=0) && (!foundIt); i--) {
            ShipArmor arm = armors.get(i);

            // as we go backwards from the bestest armor to the worsest,
            // a better armor should always be chosen before the reinforced one if it exists due to smaller size
            // some races will just never use reinforced armor, I believe, the ones that prefer smaller ships
            if (!arm.reinforced() || (reinforcedArmorAllowed)) {
                d.armor(armors.get(i));
                if ((initialSpace - d.availableSpace()) <= spaceAllowed)
                    foundIt = true;
            }
        }
        return (spaceAllowed - (initialSpace - d.availableSpace()));
    }

    private float setFittingShields(ShipDesigner ai, ShipDesign d, float spaceAllowed) {
        float initialSpace = d.availableSpace();

        boolean foundIt = false;
        List<ShipShield> shields = ai.lab().shields();
        for (int i=shields.size()-1; (i >= 0) && (!foundIt); i--) {
            d.shield(shields.get(i));
            if ((initialSpace - d.availableSpace()) <= spaceAllowed)
                foundIt = true;
        }
        return (spaceAllowed - (initialSpace - d.availableSpace()));
    }

    private float setFittingECM(ShipDesigner ai, ShipDesign d, float spaceAllowed) {
        float initialSpace = d.availableSpace();

        boolean foundIt = false;
        List<ShipECM> ecm = ai.lab().ecms();
        for (int i=ecm.size()-1; (i >=0) && (!foundIt); i--) {
            d.ecm(ecm.get(i));
            if ((initialSpace - d.availableSpace()) <= spaceAllowed)
                foundIt = true;
        }
        return (spaceAllowed - (initialSpace - d.availableSpace()));
    }
////////////////////////////////////////////////////


// ********** SPECIALS SELECTION AND FITTING FUNCTIONS ********** //

    private SortedMap<Float, ShipSpecial> buildSpecialsList(ShipDesign d, ShipDesigner ai, float antiMissle, boolean bomber, boolean needRange, boolean boostInertial, int size, float specialsSpace) {
        SortedMap<Float, ShipSpecial> specials = new TreeMap<>(Collections.reverseOrder());
        List<ShipSpecial> allSpecials = ai.lab().specials();

        for (ShipSpecial spec: allSpecials) {
            if(spec.isNone() || spec.isColonySpecial() || spec.isFuelRange())
                continue;
            if(spec.space(d) > specialsSpace)
                continue;
            Tech tech = spec.tech();
            float currentScore = 0;
            
            if(tech.isType(Tech.AUTOMATED_REPAIR))
            {
                if(tech.sequence == 0)
                    currentScore = 50;
                if(tech.sequence == 1)
                    currentScore = 100;
                currentScore -= ai.empire().tech().avgTechLevel(); //loses usefullness with more miniaturization
                if(d.size() < 2)
                    currentScore = 0;
                if(d.size() > 2)
                    currentScore *= 6;
            }
            else if(tech.isType(Tech.SCANNER))
                currentScore = 50;
            else if(tech.isType(Tech.BLACK_HOLE))
            {
                currentScore = 500;
                currentScore *= (5-d.size());
                if(needRange)
                    currentScore /= 5;
            }
            else if(spec.beamRangeBonus() > 0)
            {
                if(bomber)
                    currentScore = 40;
                else
                    currentScore = 200;
                currentScore *= spec.beamRangeBonus();
                if(needRange)
                    currentScore *= 5;
            }
            else if(spec.beamShieldMod() < 1)
            {
                if(bomber)
                    currentScore = 40;
                else
                    currentScore = 200;
            }
            else if(tech.isType(Tech.CLOAKING))
            {
                if(bomber)
                    currentScore = 500;
                else
                    currentScore = 250;
                if(needRange)
                    currentScore *= 2;
            }
            else if(tech.isType(Tech.DISPLACEMENT))
            {
                if(bomber)
                    currentScore = 100;
                else
                    currentScore = 50;
            }
            else if(tech.isType(Tech.ENERGY_PULSAR))
            {
                currentScore = 50 * (tech.sequence + 1);
                currentScore *= (5-d.size());
                if(bomber)
                    currentScore /= 5;
                if(needRange)
                    currentScore /= 5;
            }
            else if(tech.isType(Tech.MISSILE_SHIELD))
            {
                if(tech.sequence == 0)
                    currentScore = 40;
                if(tech.sequence == 1)
                    currentScore = 75;
                if(tech.sequence == 2)
                    currentScore = 100;
                float missileLevel = 0;
                if(ai.empire().tech().topBaseMissileTech() != null)
                    missileLevel = Math.max(missileLevel, ai.empire().tech().topBaseMissileTech().level());
                if(ai.empire().tech().topBaseScatterPackTech() != null)
                    missileLevel = Math.max(missileLevel, ai.empire().tech().topBaseScatterPackTech().level());
                currentScore -= missileLevel;
                currentScore = Math.max(0, currentScore);
                currentScore *= 10;
                currentScore *= antiMissle;
                if(!bomber)
                    currentScore /= 2;
            }
            else if(tech.isType(Tech.REPULSOR))
            {
                currentScore = 50;
                if(bomber)
                    currentScore /= 5;
                if(needRange)
                    currentScore *= 2;
            }
            else if(tech.isType(Tech.SHIP_INERTIAL))
            {
                currentScore = 100 * (tech.sequence + 1);
                if(bomber)
                    currentScore *= 2;
                if(boostInertial)
                    currentScore *= 2;
            }
            else if(tech.isType(Tech.SHIP_NULLIFIER))
            {
                currentScore = 100;
                if(needRange)
                    currentScore *= 1.5;
            }
            else if(tech.isType(Tech.STASIS_FIELD))
            {
                currentScore = 200;
                if(needRange)
                    currentScore /= 5;
            }
            else if(tech.isType(Tech.STREAM_PROJECTOR))
            {
                currentScore = 100 * (tech.sequence + 1);
                currentScore *= (5-d.size());
                if(needRange)
                    currentScore *= 2;
                if(bomber)
                    currentScore /= 5;
            }
            else if(tech.isType(Tech.SUBSPACE_INTERDICTOR))
                currentScore = 200 * (1 - ai.empire().generalAI().defenseRatio());
            else if(tech.isType(Tech.TELEPORTER))
            {
                currentScore = 100 * ai.empire().generalAI().defenseRatio();
                if(bomber)
                    currentScore /= 5;
            }
            //ail: removed division by size otherwise bigger and better stuff will never be used because other stuff just miniaturizes more
            //currentScore /= spec.space(d);
            //if we put stuff with 0 score, we end up with tinies and auto-repair
            if(currentScore > 0)
                specials.put(currentScore, spec);
            //System.out.print("\n"+ai.empire().name()+" "+d.name()+" "+spec.name()+" score "+currentScore);
        }
        return specials; 
    }

    private float setFittingSpecial(ShipDesigner ai, ShipDesign d, float spaceAllowed, SortedMap<Float, ShipSpecial> specials) {
        int nextSlot = d.nextEmptySpecialSlot();
        if (nextSlot < 0)
            return spaceAllowed;
        if(specials.isEmpty())
            return spaceAllowed;
        
        float remainingSpace = spaceAllowed; 

        for(ShipSpecial spec : specials.values())
        {
            if(spec.isNone())
                continue;
            if(spec.space(d) <= remainingSpace)
            {
                d.special(nextSlot,spec);
                nextSlot++;
                remainingSpace -= spec.space(d);
                //System.out.print("\n"+ai.empire().name()+" "+d.name()+" added "+spec.name()+" with "+spec.space(d)+" remaining: "+remainingSpace);
                if(nextSlot > 2)
                    break;
            }
        }
        return remainingSpace;
    }
 
    public static List<TechTree> assessRivalsTech(Empire emp, int rivalsNum) {
        List<TechTree> rivalTech = new ArrayList<>();
        SortedMap<Float, EmpireView> relationsMap = new TreeMap<>();

        // sorting all known empires by the relations with them, ascending
        for (EmpireView ev : emp.empireViews()) {
            if (ev != null)
                relationsMap.put(ev.embassy().relations(), ev);
        }

        // yeah, sorry, that was the most straightforward Java-ish method I found to get top three
        if (!relationsMap.isEmpty()) { 
            Iterator<EmpireView> worstNeighbors = relationsMap.values().iterator();
            for (int i = 0; (i < rivalsNum) && (worstNeighbors.hasNext()); i++) {
                rivalTech.add(worstNeighbors.next().spies().tech());
            }
        }
        
        // if we have less known empires than rivalsNum, add ourselves into the list
        if (rivalTech.size() < rivalsNum) {
            rivalTech.add(emp.tech());
        }
        return rivalTech;
    }
    
// ********* FUNCTIONS SETTING ANTI-SHIP AND ANTI-PLANET WEAPONS ********** //

    private void setOptimalWeapon(ShipDesigner ai, ShipDesign d, float spaceAllowed, int numSlotsToUse, boolean mustBeRanged, boolean mustTargetShips, boolean prohibitMissiles) {
        List<ShipWeapon> allWeapons = ai.lab().weapons();
        ShipWeapon bestWeapon = null;
        float bestScore = 0.0f;
        float shield = ai.empire().bestEnemyShieldLevel();
        if(!mustTargetShips)
            shield = ai.empire().bestEnemyPlanetaryShieldLevel();
        float startingShield = shield;
        //System.out.print("\n"+ai.empire().name()+" "+d.name()+" air: "+mustTargetShips+" ranged: "+mustBeRanged+" beams: "+prohibitMissiles);
        while(bestWeapon == null)
        {
            for (ShipWeapon wpn: allWeapons) {
                if (wpn.canAttackShips() && mustTargetShips || !mustTargetShips) {
                    //System.out.print("\n"+ai.empire().name()+" "+d.name()+" wpn: "+wpn.name()+" air: "+mustTargetShips+" shd: "+shield+" spc: "+wpn.space(d)+"/"+spaceAllowed);
                    //We don't want missiles: Can be outrun, can run out and strong counters exist
                    if(wpn.space(d) > spaceAllowed)
                        continue;
                    if (wpn.isMissileWeapon() && prohibitMissiles)
                        continue;
                    if (wpn.range() < 2 && mustBeRanged)
                        continue;
                    if(!mustTargetShips && !wpn.groundAttacksOnly())
                        continue;
                    float currentScore = wpn.firepower(shield) / wpn.space(d);
                    if(currentScore > bestScore)
                    {
                        bestWeapon = wpn;
                        bestScore = currentScore;
                    }
                }
            }
            if(bestWeapon == null)
            {
                shield -= 1;
                //if we couldn't find any ranged-weapon that does damage, we allow 
                if(shield < 0 && prohibitMissiles == true && mustBeRanged == true)
                {
                    shield = startingShield;
                    prohibitMissiles = false;
                    continue;
                }
                if(shield < 0 && prohibitMissiles == false && mustBeRanged == true)
                {
                    shield = startingShield;
                    mustBeRanged = false;
                    continue;
                }
                if(shield < 0)
                    return;
            }
        }
        int weaponSlotsOccupied = 0;
        for (int i = 0; i < ShipDesign.maxWeapons; i++) {
            if (d.wpnCount(i)>0) {
                weaponSlotsOccupied++;
            }
        }
        
        int num = (int)Math.floor(spaceAllowed / bestWeapon.space(d));
        //ail: there is a maximum amount of bombs that make sense, which is exceeded in lategame, we need to calculate it and adjust the number
        if(bestWeapon.groundAttacksOnly())
        {
            //maximum hitpoints a colony can have * hitpoints per point of population
            float highestPopulation = 0;
            for (int id=0;id<ai.empire().sv.count();id++) 
            {
                StarSystem current = galaxy().system(id);
                if(current.planet().maxSize() > highestPopulation)
                    highestPopulation = current.planet().maxSize();
            }
            float maxDamageNeeded = highestPopulation * 200;
            float maxBombsNeeded = maxDamageNeeded / (bestWeapon.firepower(ai.empire().bestEnemyPlanetaryShieldLevel()-ai.empire().bestEnemyShieldLevel()) * 10);
            num = (int) Math.ceil(min(num, maxBombsNeeded));
        }
        //System.out.print("\n"+ai.empire().name()+" "+d.name()+" add: "+num+" "+bestWeapon.name());
        int maxSlots = weaponSlotsOccupied + numSlotsToUse;
        if (maxSlots > ShipDesign.maxWeapons)
            maxSlots = ShipDesign.maxWeapons;

        for (int slot=weaponSlotsOccupied; slot<maxSlots;slot++) {
            int numSlot = (int) Math.ceil((float)num/(maxSlots-slot));
            if (numSlot > 0) {
                d.weapon(slot, bestWeapon);
                d.wpnCount(slot, numSlot);
                num -= numSlot;
            }
        }
    }
    
    private float shipProductionBudget(ShipDesigner ai, int topSystems) {
        // how many BCs can topSystems number of top producing colonies
        // crank into ship production with shipRatio ratio per turn

        List<StarSystem> systems = ai.empire().allColonizedSystems();
        List<Float> systemsProduction = new ArrayList<>();

        for (StarSystem sys: systems)
            systemsProduction.add(sys.colony().production());
        
        Collections.sort(systemsProduction, Collections.reverseOrder());
        
        float totalShipProduction = 0f;
        for (int i = 0; (i<topSystems) && (i<systemsProduction.size()); i++)
            totalShipProduction += systemsProduction.get(i);

        return totalShipProduction;
    }
}
