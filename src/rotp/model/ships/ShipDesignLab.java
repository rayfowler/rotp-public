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
package rotp.model.ships;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.ShipView;
import rotp.model.tech.TechEngineWarp;
import rotp.util.Base;

public class ShipDesignLab implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_DESIGNS = 6;

    private Empire empire;
    private final ShipDesign[] designs = new ShipDesign[ShipDesignLab.MAX_DESIGNS];
    private final DesignStargate stargateDesign = new DesignStargate();

    private final List<ShipView> designHistory = new ArrayList<>();
    private int shipStyleIndex;

    private final List<ShipComputer> computer = new ArrayList<>();
    private final List<ShipShield> shield = new ArrayList<>();
    private final List<ShipECM> ecm = new ArrayList<>();
    private final List<ShipArmor> armor = new ArrayList<>();
    private final List<ShipEngine> engine = new ArrayList<>();
    private final List<ShipManeuver> maneuver = new ArrayList<>();
    private final List<ShipWeapon> weapon = new ArrayList<>();
    private final List<ShipSpecial> special = new ArrayList<>();

    private int scoutDesignId, bomberDesignId, fighterDesignId, colonyDesignId, destroyerDesignId;

    private float bestEnemyShieldLevel = 0;
    private float bestEnemyPlanetaryShieldLevel = 0;
    public boolean needColonyShips = false;
    public boolean needExtendedColonyShips = false;
    public boolean needScouts = true;

    private transient List<ShipDesign> outdatedDesigns;

    public ShipDesign[] designs()                 { return designs; }
    public ShipDesign design(int i)               { return designs[i]; }
    public DesignStargate stargateDesign()        { return stargateDesign; }
    public Empire empire()                        { return empire; }
    public List<ShipView> designHistory()         { return designHistory; }
    public List<ShipComputer> computers()         { return computer; }
    public List<ShipShield> shields()             { return shield; }
    public List<ShipECM> ecms()                   { return ecm; }
    public List<ShipArmor> armors()               { return armor; }
    public List<ShipEngine> engines()             { return engine; }
    public List<ShipManeuver> maneuvers()         { return maneuver; }
    public List<ShipWeapon> weapons()             { return weapon; }
    public List<ShipSpecial> specials()           { return special; }
    public ShipDesign scoutDesign()               { return designs[scoutDesignId]; }
    public ShipDesign bomberDesign()              { return designs[bomberDesignId]; }
    public ShipDesign fighterDesign()             { return designs[fighterDesignId]; }
    public ShipDesign colonyDesign()              { return designs[colonyDesignId]; }
    public ShipDesign destroyerDesign()           { return designs[destroyerDesignId]; }
    public float bestEnemyShieldLevel()           { return bestEnemyShieldLevel; }
    public float bestEnemyPlanetaryShieldLevel()  { return bestEnemyPlanetaryShieldLevel; }
    public int shipStyleIndex()                   { return shipStyleIndex; }

    public ShipWeapon noWeapon()                  { return weapons().get(0); }
    public ShipSpecial noSpecial()                { return specials().get(0); }

    public List<ShipDesign> outdatedDesigns() {
        if (outdatedDesigns == null)
            outdatedDesigns = new ArrayList<>();
        return outdatedDesigns;
    }
    public void init(Empire c)  {
        empire = c;
        if (ShipLibrary.current().styles.contains(empire.race().preferredShipSet))
            shipStyleIndex = ShipLibrary.current().styles.indexOf(empire.race().preferredShipSet);
        else
            shipStyleIndex = ShipLibrary.current().selectRandomUnchosenSet();

        // add default ship options that are NOT provided by starting techs
        // shield, armor, engine added by starting techs
        addWeapon(new ShipWeapon());
        addSpecial(new ShipSpecial());

        loadInitialDesigns();
    }
    public boolean isCurrentLabDesign(int id) {
        return (id == scoutDesignId) || (id == fighterDesignId) || (id == destroyerDesignId)
                || (id == bomberDesignId) || (id == colonyDesignId);
    }
    public int numDesigns() {
        int i = 0;
        for (ShipDesign d : designs) {
            if (d.active())
                i++;
        }
        return i;
    }
    public boolean canAddDesign() {
        for (ShipDesign d : designs) {
            if (!d.active())
                return true;
        }
        return false;
    }
    public boolean canScrapADesign() {
        int numActive = 0;
        for (ShipDesign d : designs) {
            if (d.active())
                numActive++;
        }
        return numActive > 1;
    }
    private void addDesign(ShipDesign des)  {
        boolean added = false;
        for (int i=0;i<designs.length;i++) {
            if (!designs[i].active()) {
                designs[i] = des;
                des.active(true);
                des.id(i);
                des.seq(i);
                return;
            }
        }
        if (!added)
            throw new RuntimeException("Invalid attempt to add design");
    }
    private void loadInitialDesigns() {
        for (int i=0;i<designs.length;i++) {
            if (designs[i] == null) {
                designs[i] = newBlankDesign(ShipDesign.SMALL);
                designs[i].id(i);
            }
        }
        ShipDesign design;

        design = empire.isPlayer() ? startingPlayerScoutDesign() : empire.shipDesignerAI().newScoutDesign();
        setScoutDesign(design, 0);

        design = empire.isPlayer() ? startingPlayerFighterDesign() : empire.shipDesignerAI().newFighterDesign(ShipDesign.SMALL);
        setFighterDesign(design, 1);

        design = empire.isPlayer() ? startingPlayerBomberDesign() : empire.shipDesignerAI().newBomberDesign(ShipDesign.MEDIUM);
        setBomberDesign(design, 2);

        design = empire.isPlayer() ? startingPlayerDestroyerDesign() : empire.shipDesignerAI().newDestroyerDesign(ShipDesign.MEDIUM);
        setDestroyerDesign(design, 3);

        design = startingPlayerColonyDesign();
        setColonyDesign(design, 4);
    }
    public void nextTurn() {
        // update opp shield level (for fighter designs)
        bestEnemyShieldLevel = empire.bestEnemyShieldLevel();
        bestEnemyPlanetaryShieldLevel = empire.bestEnemyPlanetaryShieldLevel();
        for (ShipDesign d : designs) {
            d.recalculateCost();
        }
    }
    public void recordConstruction(Design d, int num) {
        ShipView sv = shipViewFor((ShipDesign)d);
        sv.design().addTotalBuilt(num);
        sv.setViewDate();
    }
    public ShipView shipViewFor(ShipDesign d) {
        // get shipView from design history. If none, add one.
        for (ShipView sv1 : designHistory) {
            if (sv1.matches(d))
                return sv1;
        }

        ShipView sv = new ShipView(empire, d);
        sv.scan();
        designHistory.add(sv);
        return sv;
    }
    public void recordScrap(Design d, int num)       { d.addTotalScrapped(num); }
    public void recordDestruction(Design d, int num) { d.addTotalDestroyed(num); }
    public void recordUse(Design d, int num)         { d.addTotalUsed(num); }
    public void recordKills(Design d, int num) {
        ShipDesign sd = (ShipDesign) d;
        ShipView sv = shipViewFor(sd);
        sv.addTotalKills(num);
    }
    public int availableDesignSlot() {
        for (int i=0;i<designs.length;i++) {
            if (!designs[i].active())
                return i;
        }
        return -1;
    }
    public void setScoutDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.SCOUT);
        addDesign(d);
        empire().swapShipConstruction(scoutDesign(), d);
        scoutDesignId = d.id();
        log("Empire: "+empire.name()+" creates scout design: "+d.name()+"  slot:"+slot);
    }
    public void setColonyDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.COLONY);
        addDesign(d);
        empire().swapShipConstruction(colonyDesign(), d);
        colonyDesignId = d.id();
        log("Empire: "+empire.name()+" creates colony design: "+d.name()+"  slot:"+slot);
    }
    public void setFighterDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.FIGHTER);
        addDesign(d);
        empire().swapShipConstruction(fighterDesign(), d);
        fighterDesignId = d.id();
        log("Empire: "+empire.name()+" creates fighter design: "+d.name()+"  slot:"+slot);
    }
    public void setBomberDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.BOMBER);
        addDesign(d);
        empire().swapShipConstruction(bomberDesign(), d);
        bomberDesignId = d.id();
        log("Empire: "+empire.name()+" creates bomber design: "+d.name()+"  slot:"+slot);
    }
    public void setDestroyerDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.DESTROYER);
        addDesign(d);
        empire().swapShipConstruction(destroyerDesign(), d);
        destroyerDesignId = d.id();
        log("Empire: "+empire.name()+" creates destroyer design: "+d.name()+"  slot:"+slot);
    }
    public void scrapOutdatedDesign() {
        ShipDesign mostOutdated = null;
        for (ShipDesign d : outdatedDesigns()) {
            if ((mostOutdated == null) || (d.remainingLife() < mostOutdated.remainingLife()))
                mostOutdated = d;
        }
        if (mostOutdated != null) {
            outdatedDesigns().remove(mostOutdated);
            scrapDesign(mostOutdated);
        }
    }
    public ShipDesign startingPlayerScoutDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.SMALL);
        design.special(0, specialReserveFuel());
        design.mission(ShipDesign.SCOUT);
        design.name(text("SHIP_DESIGN_1ST_SCOUT_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingPlayerFighterDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.SMALL);
        design.engine(engines().get(0));
        design.addWeapon(beamWeapon(0, false), 1);
        design.mission(ShipDesign.FIGHTER);
        design.name(text("SHIP_DESIGN_1ST_FIGHTER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingPlayerBomberDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.MEDIUM);
        design.addWeapon(bombWeapon(0), 2);
        design.addWeapon(beamWeapon(0, false), 2);
        design.mission(ShipDesign.BOMBER);
        design.name(text("SHIP_DESIGN_1ST_BOMBER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingPlayerDestroyerDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.MEDIUM);
        design.mission(ShipDesign.DESTROYER);
        design.addWeapon(missileWeapon(0, 2), 1);
        design.addWeapon(beamWeapon(0, false), 3);
        design.name(text("SHIP_DESIGN_1ST_DESTROYER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingPlayerColonyDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.LARGE);
        design.mission(ShipDesign.COLONY);
        design.special(0, empire.shipDesignerAI().bestColonySpecial());
        design.name(text("SHIP_DESIGN_1ST_COLONY_NAME"));
        iconifyDesign(design);
        return design;
    }
    public void nameDesign(ShipDesign d) {
        List<String> shipNames = empire.race().shipNames(d.size());
        if ((shipNames == null) || shipNames.isEmpty()) {
            d.name(concat("Ship", str(roll(100,999))));
            return;
        }
        List<String> remainingNames = new ArrayList<>();
        for (String name : shipNames) {
            if (designNamed(name) == null)
                remainingNames.add(name);
        }
        if (remainingNames.isEmpty()) {
            d.name(concat("Ship", str(roll(100,999))));
            return;
        }
        int index = min(0,remainingNames.size()-1);
        d.name(remainingNames.get(index));
    }
    public void iconifyDesign(ShipDesign newDesign) {
        // get all valid icons for this size and remove ones already being used
        List<String> validIconKeys = ShipLibrary.current().validIconKeys(shipStyleIndex, newDesign.size());
        for (ShipDesign d : designs()) {
            if (d.active() && (d.size() == newDesign.size()))
                validIconKeys.remove(d.iconKey());
        }

        if (validIconKeys.isEmpty()) {
            int newNum = roll(0,ShipLibrary.designsPerSize-1);
            newDesign.iconKey(ShipLibrary.current().shipKey(shipStyleIndex, newDesign.size(), newNum));
        }
        else
            newDesign.iconKey(random(validIconKeys));
    }
    public Design prevDesignFrom(Design d, boolean stargateBuilt) {
        int index;
        if ((d == null) || (d == stargateDesign))
            index = MAX_DESIGNS;
        else
            index = d.id();

        while (true) {
            if (index == 0) {
                if (empire.tech().canBuildStargate() && !stargateBuilt)
                    return stargateDesign;
                else
                    index = MAX_DESIGNS;
            }
            index--;
            ShipDesign design = design(index);
            if (design.active())
                return design;
        }
    }
    public Design nextDesignFrom(Design d, boolean stargateBuilt) {
        int index;
        if ((d == null) || (d == stargateDesign))
            index = -1;
        else
            index = d.id();

        while (true) {
            if (index == (MAX_DESIGNS-1)) {
                if (empire.tech().canBuildStargate() && !stargateBuilt)
                    return stargateDesign;
               else
                    index = -1;
            }
            index++;
            ShipDesign design = design(index);
            if (design.active())
                return design;
        }
    }
    public ShipDesign newBlankDesign(int size) {
        ShipDesign design = new ShipDesign(size);
        design.active(false);
        design.lab(this);
        design.computer(computers().get(0));
        design.shield(shields().get(0));
        design.ecm(ecms().get(0));
        design.armor(armors().get(0));
        design.engine(engines().get(0));
        design.maneuver(maneuvers().get(0));
        for (int i=0;i<ShipDesign.maxWeapons();i++)
            design.weapon(i, weapons().get(0));
        for (int i=0;i<ShipDesign.maxSpecials();i++)
            design.special(i, specials().get(0));
        return design;
    }
    public void clearDesign(ShipDesign design) {
        design.computer(computers().get(0));
        design.shield(shields().get(0));
        design.ecm(ecms().get(0));
        design.armor(armors().get(0));
        design.engine(engines().get(0));
        design.maneuver(maneuvers().get(0));
        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            design.weapon(i, weapons().get(0));
            design.wpnCount(i, 0);
        }
        for (int i=0;i<ShipDesign.maxSpecials();i++)
            design.special(i, specials().get(0));
    }
    public void scrapDesign(ShipDesign d) {
        int designId = d.id();

        // remove from existing fleets
        int scrappedCount = galaxy().ships.scrapDesign(empire.id, designId);
        log("Scrapping design: ", d.name(), " count:", str(scrappedCount));

        d.scrapped(true);
        d.active(false);
        d.addTotalScrapped(scrappedCount);

        // reimburse civ reserve for 1/2 of ship's cost (halved when added to reserve)
        empire().addReserve(d.scrapValue(scrappedCount)*2);
        empire().swapShipConstruction(d);
        
        // remove scrapped design from list of designs and replace with new, inactive design
        designs[designId] = newBlankDesign(ShipDesign.SMALL);
        designs[designId].id(designId);
        designs[designId].copyFrom(d);
    }
    public String nextAvailableIconKey(int size, String currIconKey) {
        int newNum;
        String iconKey;

        // get all valid icons for this size and remove ones already being used
        // null-check necessary for design because this can be called up on init when not all designs are present
        List<String> validIconKeys = ShipLibrary.current().validIconKeys(shipStyleIndex, size);
        for (ShipDesign d : designs()) {
            if (d.active() && (d.size() == size))
                validIconKeys.remove(d.iconKey());
        }

        if (validIconKeys.isEmpty()) {
            newNum = roll(0,ShipLibrary.designsPerSize-1);
            iconKey = ShipLibrary.current().shipKey(shipStyleIndex, size, newNum);
        }
        else {
            newNum = validIconKeys.indexOf(currIconKey);
            if ((newNum + 1) >= validIconKeys.size())
                newNum = 0;
            else
                newNum++;
            iconKey = validIconKeys.get(newNum);
        }
        return iconKey;
    }
    public void addComputer(ShipComputer c) {
        computers().add(c);
        Collections.sort(computers(),ShipComponent.SELECTION_ORDER);
    }
    public void addShield(ShipShield c) {
        shields().add(c);
        Collections.sort(shields(),ShipComponent.SELECTION_ORDER);
    }
    public void addECM(ShipECM c) {
        ecms().add(c);
        Collections.sort(ecms(),ShipComponent.SELECTION_ORDER);
    }
    public void addArmor(ShipArmor c) {
        armors().add(c);
        Collections.sort(armors(),ShipComponent.SELECTION_ORDER);
}
    public void addEngine(ShipEngine c) {
        engines().add(c);
        Collections.sort(engines(),ShipComponent.SELECTION_ORDER);
    }
    public void addManeuver(ShipManeuver c) {
        maneuvers().add(c);
        Collections.sort(maneuvers(),ShipComponent.SELECTION_ORDER);
    }
    public boolean hasManeuverForTech(TechEngineWarp tech) {
        for (ShipManeuver manv: maneuvers()) {
            if (manv.tech() == tech)
                return true;
        }
        return false;
    }
    public void addWeapon(ShipWeapon c) {
        weapons().add(c);
        Collections.sort(weapons(),ShipComponent.SELECTION_ORDER);
    }
    public void addSpecial(ShipSpecial c) {
        specials().add(c);
        Collections.sort(specials(),ShipComponent.SELECTION_ORDER);
    }
    public ShipWeapon beamWeapon(int seq, boolean heavy) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.isBeamWeapon()) {
                ShipWeaponBeam wpn2 = (ShipWeaponBeam) wpn;
                if ((wpn2.tech().sequence == seq) && (wpn2.heavy() == heavy))
                    return wpn;
            }
        }
        return null;
    }
    public ShipWeapon missileWeapon(int seq, int shots) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.isMissileWeapon()) {
                ShipWeaponMissile wpn2 = (ShipWeaponMissile) wpn;
                if ((wpn2.tech().sequence == seq) && (wpn2.shots() == shots))
                    return wpn;
            }
        }
        return null;
    }
    public ShipWeapon bombWeapon(int seq) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.groundAttacksOnly()) {
                if (((ShipWeaponBomb) wpn).tech().sequence == seq)
                    return wpn;
            }
        }
        return null;
    }
    public ShipSpecial specialNamed(String s) {
        for (ShipSpecial spec : specials()) {
            if (spec.name().equals(s))
                return spec;
        }
        return null;
    }
    public ShipSpecial specialReserveFuel() {
        for (ShipSpecial spec : specials()) {
            if (spec.isFuelRange())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialBattleScanner() {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsScanning())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialTeleporter() {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsTeleporting())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialCloak() {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsCloaking())
                return spec;
        }
        return null;
    }
    public ShipDesign designNamed(String s) {
        for (ShipDesign des : designs()) {
            if (des.active() && des.name().equals(s))
                return des;
        }
        return null;
    }
    public ShipEngine fastestEngine() {
        ShipEngine fastestEngine = engines().get(0);
        for (ShipEngine eng : engines()) {
            if (eng.warp() > fastestEngine.warp())
                fastestEngine = eng;
        }
        return fastestEngine;
    }
    public ShipArmor bestArmor() {
        ShipArmor bestArmor = armors().get(0);
        for (ShipArmor arm : armors()) {
            if (!arm.reinforced() && (arm.sequence() > bestArmor.sequence()))
                bestArmor = arm;
        }
        return bestArmor;
    }
    public ShipComputer nextBestComputer(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipComputer comp: computers()) {
            if ((comp.level() > d.computer().level()) && (comp.space(d) < space))
                return comp;
        }
        return null;
    }
    public ShipManeuver nextBestManeuver(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipManeuver manv: maneuvers()) {
            if ((manv.level() > d.maneuver().level()) && (manv.space(d) < space))
                return manv;
        }
        return null;
    }
    public ShipShield nextBestShield(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipShield shld: shields()) {
            if ((shld.level() > d.shield().level()) && (shld.space(d) < space))
                return shld;
        }
        return null;
    }
    public ShipECM nextBestECM(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipECM ecm: ecms()) {
            if ((ecm.level() > d.ecm().level()) && (ecm.space(d) < space))
                return ecm;
        }
        return null;
    }
    public ShipWeapon bestWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon() && !wpn.groundAttacksOnly()) {
                numWeapons = (int) (space/wpn.space(d));
                wpnDamage = numWeapons * wpn.firepower(shieldLevel);
                if (wpn.isLimitedShotWeapon()) {
                    float base = pow(2,wpn.shots());
                    adjDamage = (base -1)/base * wpnDamage;
                }
                else
                        adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
    public ShipWeapon bestPlanetWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyPlanetaryShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon()) {
                numWeapons = (int) (space/wpn.space(d));
                wpnDamage = numWeapons * wpn.firepower(shieldLevel) * wpn.planetDamageMod();
                if (wpn.isLimitedShotWeapon()) {
                    float base = pow(2,wpn.shots());
                    adjDamage = (base -1)/base * wpnDamage;
                }
                else
                    adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
    public ShipWeapon bestUnlimitedShotWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon() && !wpn.groundAttacksOnly() && !wpn.isLimitedShotWeapon()) {
                numWeapons = (int) (space/wpn.space(d));
                wpnDamage = numWeapons * wpn.firepower(shieldLevel);
                adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
}
