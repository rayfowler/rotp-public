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

import rotp.model.ai.interfaces.ShipDesigner;

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.Race;
import rotp.model.galaxy.StarSystem;

import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import static rotp.model.ships.ShipDesign.maxSpecials;
import static rotp.model.ships.ShipDesign.maxWeapons;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.Tech;
import rotp.model.tech.TechBiologicalWeapon;
import rotp.model.tech.TechTree;
import rotp.util.Base;

public class NewShipTemplate implements Base {
    private static final NewShipTemplate instance = new NewShipTemplate();

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
    
    public static void nameShipDesign(ShipDesigner ai, ShipDesign d) {
        instance.nameDesign(ai, d);
    }
    
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
        // create a blank design, one for each size. Add the current design as a 5th entry
        ShipDesign[] shipDesigns = new ShipDesign[4];
        for (int i = 0; i<4; i++) 
        {
            shipDesigns[i] = newDesign(ai, role, i);
        }

        int ReachableSystems = 0;
        for (int id=0;id<ai.empire().sv.count();id++) 
        {
            if(ai.empire().sv.inShipRange(id))
                ReachableSystems++;
        }
        
        SortedMap<Float, ShipDesign> designSorter = new TreeMap<>();
        float costLimit = ai.empire().totalPlanetaryProduction() * 0.125f * 50 / ReachableSystems;
        //System.out.print("\n"+galaxy().currentTurn()+" "+ai.empire().name()+" costlimit: "+costLimit+" reachables: "+ReachableSystems+" totalCost at limit: "+(costLimit*ai.empire().systemsInShipRange(ai.empire()).size() / 50));
        float biggestShipWeaponSize = 0;
        float biggestBombSize = 0;
        float highestAttackLevel = 0;
        ShipDesign biggestWeaponDesign = null;
        for (int i = 0; i<4; i++) {
            ShipDesign design = shipDesigns[i];
            for (int j=0; j<maxWeapons(); j++)
            {
                if(design.weapon(j).groundAttacksOnly())
                {
                    if(design.weapon(j).space(design) > biggestBombSize)
                        biggestBombSize = design.weapon(j).space(design);
                }
                else
                {
                    if(design.weapon(j).space(design) > biggestShipWeaponSize)
                    {
                        biggestShipWeaponSize = design.weapon(j).space(design);
                        biggestWeaponDesign = design;
                    }
                }
            }
            if(design.attackLevel() > highestAttackLevel)
                highestAttackLevel = design.attackLevel();
        }
        //System.out.print("\n"+galaxy().currentTurn()+" "+ai.empire().name()+" costlimit: "+costLimit+" biggestShipWeaponSize: "+biggestShipWeaponSize);
        for (int i = 0; i<4; i++) {
            ShipDesign design = shipDesigns[i];
            float score = design.spaceUsed() / design.cost();
            float defScore = design.hits() / design.cost();
            float hitPct = (5 + highestAttackLevel - (design.beamDefense() + design.missileDefense()) / 2) / 10;
            hitPct = max(.05f, hitPct);
            hitPct = min(hitPct, 1.0f);
            float absorbPct = 1.0f;
            if(design.firepowerAntiShip(0) > 0)
                absorbPct = max(biggestWeaponDesign.firepowerAntiShip(design.shieldLevel()) / biggestWeaponDesign.firepowerAntiShip(0), 0.05f); //more than 95% absorb will be normalized so score doesn't become infinity
            float mitigation = Float.MAX_VALUE;
            if(hitPct > 0 && absorbPct > 0)    
                mitigation = (1 / hitPct) * (1 / absorbPct);
            defScore *= mitigation;
            score *= defScore;
            //System.out.print("\n"+ai.empire().name()+" "+design.name()+" Role: "+role+" size: "+design.size()+" score wo. costlimit: "+score+" costlimit-dividor: "+design.cost() / costLimit);
            if(design.cost() > costLimit)
                score /= design.cost() / costLimit;
            
            float spaceWpnSize = 0;
            float bombWpnSize = 0;
            for (int j=0; j<maxWeapons(); j++)
            {
                if(design.weapon(j).groundAttacksOnly())
                {
                    if(design.weapon(j).space(design) > bombWpnSize)
                        bombWpnSize = design.weapon(j).space(design);
                }
                else
                {
                    if(design.weapon(j).space(design) > spaceWpnSize)
                        spaceWpnSize = design.weapon(j).space(design);
                }
            }
            float specialsMod = 1;
            for (int s=0; s<maxSpecials(); s++)
            {
                //Intertial doesn't add to score because it's already taken into account for in the mitigation
                if(!design.special(s).isNone() && !design.special(s).isInertial())
                    specialsMod*=1.26;
            }

            score *= specialsMod;
            float weaponSizeMod = 1.0f;
            if(role == role.BOMBER && biggestBombSize > 0)
                weaponSizeMod *= bombWpnSize / biggestBombSize;
            else if(biggestShipWeaponSize > 0)
                weaponSizeMod *= spaceWpnSize / biggestShipWeaponSize;
            if(ai.empire().shipDesignerAI().wantHybrid())
                if(biggestBombSize > 0)
                    weaponSizeMod *= ai.empire().generalAI().defenseRatio() + (1 - ai.empire().generalAI().defenseRatio()) * bombWpnSize / biggestBombSize;
                else
                    weaponSizeMod *= ai.empire().generalAI().defenseRatio(); 
            score *= weaponSizeMod;
            //System.out.print("\n"+ai.empire().name()+" "+design.name()+" Role: "+role+" size: "+design.size()+" score: "+score+" tonnageScore: "+design.spaceUsed() / design.cost()+" defscore: "+defScore+" wpnScore: "+weaponSizeMod+" costlimit: "+costLimit+" spaceWpnSize: "+spaceWpnSize+" bomb-adpt: "+ai.bombingAdapted(design)+" specialsMod: "+specialsMod+" absorbPct: "+absorbPct+ " hitPct: "+hitPct);
            designSorter.put(score, design);
            //For bombers we want the smallest that has the best bomb because it's easiest to "dose"
            if(role == role.BOMBER && weaponSizeMod == 1)
                break;
        }
        // lastKey is design with greatest damage
        return designSorter.get(designSorter.lastKey()); 
    }
    
    public void nameDesign(ShipDesigner ai, ShipDesign d)
    {
        String nameToUse = "";
        if(!ai.empire().allColonizedSystems().isEmpty())
            nameToUse = ai.empire().allColonizedSystems().get((int)random(ai.empire().allColonizedSystems().size())).name();
        List<String> shipNames = ai.empire().race().shipNames(d.size());
        nameToUse += " " + shipNames.get((int)random(shipNames.size()));
        String Before = nameToUse;
        int numeral = 2;
        while(ai.lab().designNamed(nameToUse) != null)
        {
            nameToUse = Before + " " + numeral;
            numeral++;
        }
        d.name(nameToUse);
    }

    private ShipDesign newDesign(ShipDesigner ai, DesignType role, int size) {
        ShipDesign d = ai.lab().newBlankDesign(size);
        // name it first so we can use name for reference in debugging
        // engines are always the priority in MOO1 mechanics
        ai.lab().nameDesign(d);
        setFastestEngine(ai, d);
        // battle computers are always the priority in MOO1 mechanics
        if(role != role.DESTROYER)
            setBestBattleComputer(ai, d); 
        
        float totalSpace = d.availableSpace();
        Race race = ai.empire().dataRace();
        float enemyMissilePercentage = 0.0f;
        //ail: looking at the stats of our enemies
        boolean needRange = false;
        boolean boostInertial = false;
        float topSpeed = 0;
        float antiDote = 0;
        float avgECM = 0;
        float avgHP = 0;
        float bestSHD = 0;
        float longRangePct = 0;
        float totalCost = 0;
        float nonMissileTotal = 0.0f;
        float missileTotal = 0.0f;
        
        for(EmpireView ev : ai.empire().contacts())
        {
            if(ev.spies().tech().antidoteLevel() > antiDote)
                antiDote = ev.spies().tech().antidoteLevel();
            for(ShipDesign enemyDesign : ev.empire().shipLab().designs())
            {
                if(enemyDesign.scrapped())
                {
                    continue;
                }
                boolean isLongRange = false;
                if(enemyDesign.repulsorRange() > 0)
                {
                    needRange = true;
                }
                for (int j=0;j<maxSpecials();j++)
                {
                    if(enemyDesign.special(j).createsBlackHole())
                        boostInertial = true;
                    if(enemyDesign.special(j).beamRangeBonus() > 0)
                        isLongRange = true;
                    if(enemyDesign.special(j).allowsCloaking())
                        isLongRange = true;
                }
                for (int i=0; i<maxWeapons(); i++)
                {
                    ShipWeapon weapon = enemyDesign.weapon(i);
                    if(weapon == null)
                        continue;
                    if(weapon.range() > 1)
                        isLongRange = true;
                    if(weapon.isMissileWeapon())
                    {
                        missileTotal += weapon.cost() * enemyDesign.wpnCount(i);
                    }
                    else
                    {
                        nonMissileTotal += weapon.cost() * enemyDesign.wpnCount(i);
                    }
                }
                if(enemyDesign.combatSpeed() > topSpeed)
                    topSpeed = enemyDesign.combatSpeed();
                float count = ev.empire().shipDesignCount(enemyDesign.id());
                avgECM += enemyDesign.ecm().level() * enemyDesign.cost() * count;
                avgHP += enemyDesign.hits() * enemyDesign.cost() * count;
                if(enemyDesign.shieldLevel() > bestSHD)
                    bestSHD = enemyDesign.shieldLevel();
                if(isLongRange)
                    longRangePct += enemyDesign.cost() * count;
                totalCost += enemyDesign.cost() * count;
            }
            if(ev.empire().shipMaintCostPerBC()+ev.empire().missileBaseCostPerBC() > 0)
            {
                enemyMissilePercentage = max((enemyMissilePercentage * ev.empire().shipMaintCostPerBC() + ev.empire().missileBaseCostPerBC()) / (ev.empire().shipMaintCostPerBC() + ev.empire().missileBaseCostPerBC()), enemyMissilePercentage);
            }
        }
        //if opponent has repulsors we can't fire missiles point blank so they must be faster to compensate
        if(needRange)
            topSpeed++;
        //when ships move diagonally they can outrun same-speed missiles so missiles must be faster
        topSpeed *= sqrt(2);
        if(totalCost > 0)
        {
            longRangePct /= totalCost;
            avgECM /= totalCost;
            avgHP /= totalCost;
        }
        if(missileTotal+nonMissileTotal > 0)
        {
            enemyMissilePercentage = max(enemyMissilePercentage, missileTotal / (missileTotal + nonMissileTotal));
        }

        // initial separation of the free space left onto weapons and non-weapons/specials
        float moduleSpaceRatio = race.shipDesignMods[MODULE_SPACE];        
        float modulesSpace = totalSpace * moduleSpaceRatio;

        // arbitrary initial weighting of what isn't weapons
        int shieldWeight = 4;
        if(role == DesignType.BOMBER)
        {
            shieldWeight = 0;
        }
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

        float shieldSpace = d.totalSpace() * 0.2f;
        float ecmSpace = modulesSpace * ecmWeight / weightsSum;
        float maneuverSpace = modulesSpace * maneuverWeight / weightsSum;
        float armorSpace = modulesSpace * armorWeight / weightsSum;
        float specialsSpace = modulesSpace * specialsWeight / weightsSum;
        
        // ail: removed leftovers. The impact isn't minor at all. Even at a weight of 0 they usually crammed in the max-level ECM, leaving much less space for weapons
        // branches made in the ugly way for clarity
        // specials will be skipped for smaller hulls in the early game, bringing a bit more allowance to the second fitting
        //ArrayList<ShipSpecial> specials;
        SortedMap<Float, ShipSpecial> specials;
        //System.out.print("\n"+ai.empire().name()+" "+d.name()+" avgSHD: "+avgSHD+" avgECM: "+avgECM);
        
        switch (role) {
            case BOMBER:
                specials = buildSpecialsList(d, ai, enemyMissilePercentage, true, false, boostInertial, longRangePct);
                break;
            case FIGHTER:
            default:
                specials = buildSpecialsList(d, ai, enemyMissilePercentage, false, needRange, boostInertial, longRangePct);
                break;
        }
        
        boolean haveBHG = false;
        boolean haveCloaking = false;
        float spaceOfBlackHoleCloakCombo = 0;
        for(ShipSpecial spec : specials.values())
        {
            if(spec.allowsCloaking())
            {
                haveCloaking = true;
                spaceOfBlackHoleCloakCombo += spec.space(d);
            }
            if(spec.createsBlackHole())
            {
                spaceOfBlackHoleCloakCombo += spec.space(d);
                if(spaceOfBlackHoleCloakCombo < 0.5f * d.totalSpace())
                    haveBHG = true;
            }
        }
        //when we can combine cloaking with either BHG or Stasis, we allow a lot more space for specials
        if(haveCloaking && haveBHG)
            specialsSpace = max(specialsSpace, totalSpace * 0.5f);
        
        switch (role) {
            case BOMBER:
                setFittingSpecial(ai, d, specialsSpace, specials, true);
                setFittingArmor(ai, d, armorSpace, reinforcedArmorAllowed);
                setFittingManeuver(ai, d, maneuverSpace, sameSpeedAllowed);
                setFittingECM(ai, d, ecmSpace);
                break;
            case DESTROYER:
                setFittingArmor(ai, d, armorSpace, reinforcedArmorAllowed);
                setFittingManeuver(ai, d, maneuverSpace, sameSpeedAllowed);
                break;
            case FIGHTER:
            default:
                setFittingSpecial(ai, d, specialsSpace, specials, false);
                setFittingArmor(ai, d, armorSpace, reinforcedArmorAllowed);
                setFittingECM(ai, d, ecmSpace);
                setFittingShields(ai, d, shieldSpace);
                setFittingManeuver(ai, d, maneuverSpace, sameSpeedAllowed);
                break;
        }
        
        //if we couldn't determine other's HP, we take our own after putting on armor
        if(avgHP == 0)
            avgHP = d.hits();
        
        for (int j=0;j<maxSpecials();j++)
        {
            if(d.special(j).beamRangeBonus() > 0)
                needRange = false;
            if(d.special(j).allowsCloaking())
                needRange = false;
        }
        
        float hybridBombRatio = 0;
        if(ai.wantHybrid())
        {
            hybridBombRatio = 1 - ai.empire().generalAI().defenseRatio();
        }
        //System.out.print("\n"+galaxy().currentTurn()+" "+ai.empire().name()+" hybridBombRatio: "+hybridBombRatio);
        // what's left will be used on non-bombs for bombers, second best weapon for destroyers
        // repeat calls of setOptimalShipCombatWeapon() will result in a weapon from another category (beam, missile, streaming) than already installed
        // fighters will have a single best weapon over all four slots
        
        ShipWeapon bestNonBomb = null;
        switch (role) {
            case BOMBER:
                setOptimalWeapon(ai, d, d.availableSpace(), 1, false, false, false, topSpeed, avgECM, bestSHD, antiDote, false, avgHP);
                bestNonBomb = setOptimalWeapon(ai, d, d.availableSpace(), 3, needRange, true, false, topSpeed, avgECM, bestSHD, antiDote, true, avgHP); // even though bombs should use all space, this is run in case of it running into the max-required-bombs per design-limit
                break;
            case DESTROYER:
                bestNonBomb = setOptimalWeapon(ai, d, d.availableSpace(), 4, needRange, true, false, topSpeed, avgECM, bestSHD, antiDote, true, avgHP); // uses slots 0-3
            case FIGHTER:
            default:
                setOptimalWeapon(ai, d, d.availableSpace() * hybridBombRatio, 1, false, false, false, topSpeed, avgECM, bestSHD, antiDote, false, avgHP);
                bestNonBomb = setOptimalWeapon(ai, d, d.availableSpace(), 4, needRange, true, false, topSpeed, avgECM, bestSHD, antiDote, false, avgHP); // uses slots 0-3
                break;
        }
        //Since destroyer is always tiny and we want to make sure we have a weapon, the computer is added afterwards
        if(role == role.DESTROYER)
            setBestBattleComputer(ai, d); 
        ai.lab().iconifyDesign(d);
        for (int i = 0; i <= 2; ++i) {
            if (d.special(i) != null) {
                d.special(i, ai.lab().noSpecial());
            }
        }
        //if removing beam-bonus gave us some space back, we re-add something else
        boolean skipBeamBonus = false;
        if((bestNonBomb != null && !bestNonBomb.isBeamWeapon()) || bestNonBomb == null)
            skipBeamBonus = true;
        setFittingSpecial(ai, d, d.availableSpace(), specials, skipBeamBonus);
        //if we still have space cram in whatever weapon still fits
        switch(role)
        {
            case BOMBER:
                setOptimalWeapon(ai, d, d.availableSpace(), 1, false, false, false, topSpeed, avgECM, bestSHD, antiDote, true, avgHP);
                break;
            default:
                setOptimalWeapon(ai, d, d.availableSpace(), 4, needRange, true, false, topSpeed, avgECM, bestSHD, antiDote, true, avgHP);
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
            //System.out.print("\n"+ai.empire().name()+" "+d.name()+" "+d.name()+" shieldspace: "+spaceAllowed+" "+shields.get(i).name()+" "+shields.get(i).space(d));
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

    private SortedMap<Float, ShipSpecial> buildSpecialsList(ShipDesign d, ShipDesigner ai, float antiMissle, boolean bomber, boolean needRange, boolean boostInertial, float longRangePct) {
        SortedMap<Float, ShipSpecial> specials = new TreeMap<>(Collections.reverseOrder());
        List<ShipSpecial> allSpecials = ai.lab().specials();
        
        boolean hasCloaking = false;
        for (ShipSpecial spec: allSpecials) {
            if(spec.allowsCloaking())
                hasCloaking = true;
        }
        int designsWithStasisField = 0;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign ourDesign = ai.lab().design(slot);
            for (int j=0;j<maxSpecials();j++)
            {
                if(!ourDesign.special(j).isNone() && ourDesign.special(j).tech().isType(Tech.STASIS_FIELD) == true)
                    designsWithStasisField++;
            }
        }

        for (ShipSpecial spec: allSpecials) {
            if(spec.isNone() || spec.isColonySpecial() || spec.isFuelRange())
                continue;
            Tech tech = spec.tech();
            float currentScore = 0;
            
            //new approach: The main idea behind our bombers is that they are cheap and that they carry lots of bombs, so no specials besided of cloaking-device help us outside of combat
            if(bomber && !tech.isType(Tech.CLOAKING))
                continue;
            
            if(tech.isType(Tech.AUTOMATED_REPAIR))
            {
                if(tech.typeSeq == 0)
                    currentScore = 50;
                if(tech.typeSeq == 1)
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
                if(needRange && !hasCloaking)
                    currentScore /= 5;
            }
            else if(spec.beamRangeBonus() > 0)
            {
                if(bomber)
                    currentScore = 20;
                else
                    currentScore = 100;
                currentScore *= (d.totalSpace() - spec.space(d)) / d.totalSpace();
                if(needRange && !hasCloaking)
                    currentScore *= 5;
            }
            else if(spec.beamShieldMod() < 1)
            {
                if(bomber)
                    currentScore = 40;
                else
                    currentScore = 200;
                currentScore *= (d.totalSpace() - spec.space(d)) / d.totalSpace();
            }
            else if(tech.isType(Tech.CLOAKING))
            {
                //ail: we always want it. It's the best!
                currentScore = 5000;
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
                currentScore = 50 * (tech.typeSeq + 1);
                currentScore *= (5-d.size());
                if(bomber)
                    currentScore /= 5;
                if(needRange)
                    currentScore /= 5;
            }
            else if(tech.isType(Tech.MISSILE_SHIELD))
            {
                if(tech.typeSeq == 0)
                    currentScore = 40;
                if(tech.typeSeq == 1)
                    currentScore = 75;
                if(tech.typeSeq == 2)
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
                currentScore = 250 * (1 - longRangePct);
                if(bomber)
                    currentScore /= 5;
                if(needRange && !hasCloaking)
                    currentScore *= 2;
            }
            else if(tech.isType(Tech.SHIP_INERTIAL))
            {
                currentScore = 100 * (tech.typeSeq + 1);
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
                currentScore = 500;
                if(needRange && !hasCloaking || designsWithStasisField > 1)
                    currentScore /= 10;
            }
            else if(tech.isType(Tech.STREAM_PROJECTOR))
            {
                currentScore = 100 * (tech.typeSeq + 1);
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
            //System.out.print("\n"+ai.empire().name()+" "+d.name()+" "+spec.name()+" score "+currentScore+" space: "+spec.space(d)+"/"+d.totalSpace());
        }
        return specials; 
    }

    private float setFittingSpecial(ShipDesigner ai, ShipDesign d, float spaceAllowed, SortedMap<Float, ShipSpecial> specials, boolean skipBeamBonus) {
        int nextSlot = d.nextEmptySpecialSlot();
        if (nextSlot < 0)
            return spaceAllowed;
        if(specials.isEmpty())
            return spaceAllowed;
        
        float remainingSpace = spaceAllowed; 
        
        boolean alreadyInertial = false;
        for(ShipSpecial spec : specials.values())
        {
            if(spec.isNone())
                continue;
            if((spec.beamRangeBonus() > 0 || spec.beamShieldMod() < 1) && skipBeamBonus)
                continue;
            if(spec.isInertial() && alreadyInertial)
                continue;
            if(spec.space(d) <= remainingSpace)
            {
                d.special(nextSlot,spec);
                nextSlot++;
                remainingSpace -= spec.space(d);
                if(spec.isInertial())
                    alreadyInertial = true;
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

    private ShipWeapon setOptimalWeapon(ShipDesigner ai, ShipDesign d, float spaceAllowed, int numSlotsToUse, boolean mustBeRanged, boolean mustTargetShips, boolean prohibitMissiles, float missileSpeedMinimum, float avgECM, float avgSHD, float antiDote, boolean downSize, float avgHP) {
        List<ShipWeapon> allWeapons = ai.lab().weapons();
        ShipWeapon bestWeapon = null;
        float bestScore = 0.0f;
        float shield = avgSHD;
        if(!mustTargetShips)
            shield = ai.empire().bestEnemyPlanetaryShieldLevel() + ai.empire().bestEnemyShieldLevel();
        float startingShield = shield;
        //System.out.print("\n"+ai.empire().name()+" "+d.name()+" air: "+mustTargetShips+" ranged: "+mustBeRanged+" beams: "+prohibitMissiles);
        while(bestWeapon == null)
        {
            for (ShipWeapon wpn: allWeapons) {
                if (wpn.canAttackShips() && mustTargetShips || !mustTargetShips) {
                    //System.out.print("\n"+ai.empire().name()+" "+d.name()+" wpn: "+wpn.name()+" air: "+mustTargetShips+" shd: "+shield+" spc: "+wpn.space(d)+"/"+spaceAllowed);
                    //We don't want missiles: Can be outrun, can run out and strong counters exist
                    if(wpn.space(d) > spaceAllowed && downSize)
                        continue;
                    if (wpn.isMissileWeapon() && prohibitMissiles)
                        continue;
                    if (wpn.range() < 2 && mustBeRanged)
                        continue;
                    if(!mustTargetShips && !wpn.groundAttacksOnly())
                        continue;
                    float missileDamageMod = 1.0f;
                    if(wpn.isMissileWeapon())
                    {
                        ShipWeaponMissileType swm = (ShipWeaponMissileType)wpn;
                        //System.out.print("\n"+ai.empire().name()+" "+d.name()+" wpn: "+wpn.name()+" speed: "+swm.speed());
                        if(swm.speed() <= missileSpeedMinimum)
                            continue;
                        avgECM -= swm.computerLevel();
                        missileDamageMod = max(0.0f, 1.0f - 0.1f * avgECM);
                        missileDamageMod *= swm.shots() / 5.0f;
                    }
                    float currentScore = wpn.firepower(shield) * missileDamageMod / wpn.space(d);
                    float overKillMod = 1.0f;
                    float expectedDamagePerShot = max(0,(wpn.minDamage() + wpn.maxDamage()) / 2.0f - shield);
                    if(expectedDamagePerShot > avgHP && mustTargetShips)
                        overKillMod = avgHP / expectedDamagePerShot;
                    currentScore *= overKillMod;
                        
                    if(wpn.isBioWeapon() && allowBioWeapons(ai))
                        currentScore = bioWeaponScoreMod(ai) * TechBiologicalWeapon.avgDamage(wpn.maxDamage(), (int)antiDote) * 200 / wpn.space(d);
                    //System.out.print("\n"+ai.empire().name()+" "+d.name()+" wpn: "+wpn.name()+" score: "+currentScore+" overKillMod: "+overKillMod+" avgHP: "+avgHP+" expectedDamagePerShot: "+expectedDamagePerShot);
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
                    return null;
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
        if(num > 0 && bestWeapon != null)
            return bestWeapon;
        return null;
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
    private float bioWeaponScoreMod(ShipDesigner ai)
    {
        float scoreMod = 1;
        float totalMissileBaseCost = 0;
        float totalShipCost = 0;
        for(Empire enemy : ai.empire().contactedEmpires())
        {
            totalMissileBaseCost += enemy.missileBaseCostPerBC();
            totalShipCost += enemy.shipMaintCostPerBC();
        }
        if(totalMissileBaseCost > 0)
        {
            scoreMod = totalShipCost / (totalMissileBaseCost + totalShipCost);
        }
        return scoreMod;
    }
    private boolean allowBioWeapons(ShipDesigner ai)
    {
        boolean allow = false;
        int DesignsWithRegularBombs = 0;
        int DesignsWithBioWeapons = 0;
        for (int slot=0;slot<ShipDesignLab.MAX_DESIGNS;slot++) {
            ShipDesign ourDesign = ai.lab().design(slot);
            boolean hasRegular = false;
            boolean hasBio = false;
            for (int j=0;j<maxWeapons();j++)
            {
                if(ourDesign.weapon(j).groundAttacksOnly())
                {
                    if(ourDesign.weapon(j).isBioWeapon())
                        hasBio = true;
                    else if(ourDesign.weapon(j).tech() == ai.empire().tech().topBombWeaponTech())
                        hasRegular = true;
                }
            }
            if(hasRegular && ai.empire().shipDesignerAI().MaintenanceLimitReached(ourDesign))
                DesignsWithRegularBombs++;
            if(hasBio)
                DesignsWithBioWeapons++;
        }
        if(DesignsWithRegularBombs > 1 && DesignsWithBioWeapons < 2)
            allow = true;
        return allow;
    }
    public float weaponSpace(ShipDesign d)
    {
        float totalWeaponSpace = 0;
        for (int i=0; i<maxWeapons(); i++)
        {
            totalWeaponSpace += d.weapon(i).space(d) * d.wpnCount(i);
        }
        return totalWeaponSpace;
    }
    public float specialSpace(ShipDesign d)
    {
        float totalSpecialSpace = 0;
        for (int i=0; i<maxSpecials(); i++)
        {
            totalSpecialSpace += d.special(i).space(d) * d.wpnCount(i);
        }
        return totalSpecialSpace;
    }
}
