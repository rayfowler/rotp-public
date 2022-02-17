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
package rotp.model.combat;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.ShipView;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.*;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public class CombatStackShip extends CombatStack {
    public ShipDesign design;
    public ShipFleet fleet;
    public int selectedWeaponIndex;
    public final List<ShipComponent> weapons = new ArrayList<>();
    public float displacementPct = 0;

    public int[] weaponCount = new int[7];
    public int[] weaponAttacks = new int[7];
    public int[] shotsRemaining = new int[7];
    public int[] roundsRemaining = new int[7]; // how many rounds you can fire (i.e. missiles)
    public int[] baseTurnsToFire = new int[7];    // how many turns to wait before you can fire again
    public int[] wpnTurnsToFire = new int[7];    // how many turns to wait before you can fire again
    public boolean bombardedThisTurn = false;
    private boolean usingAI = true;
    public int repulsorRange = 0;
    private CombatStack ward;
    
    @Override
    public String toString() {
        if (target != null)
            return concat(shortString(), "  targeting: [", target.shortString(), "]");
        else
            return shortString();
    }
    @Override
    public String shortString() {
        return concat(design.name(), " hp: ", str((int)hits), "/", str((int)maxHits), " at:", str(x), ",", str(y));
    }
    public CombatStackShip(ShipFleet fl, int index, ShipCombatManager m) {
        mgr = m;
        fleet = fl;
        empire = fl.empire();
        design = empire.shipLab().design(index);
        usingAI = (empire == null) || empire.isAIControlled();
        captain = empire.ai().shipCaptain();
        origNum = num = fl.num(index);
        startingMaxHits = maxHits = design.hits();
        maxMove = design.moveRange();
        maxShield = m.system().inNebula() ? 0 : design.shieldLevel();
        attackLevel = design.attackLevel() + empire.shipAttackBonus();
        maneuverability = design.maneuverability();
        repulsorRange = design.repulsorRange();
        hits = maxHits;
        move = maxMove;
        shield = maxShield;
        missileDefense = design.missileDefense() + empire.shipDefenseBonus();
        beamDefense = design.beamDefense() + empire.shipDefenseBonus();
        displacementPct = design.missPct();
        repairPct = designShipRepairPct();
        beamRangeBonus = designBeamRangeBonus();
        image = design.image();
        initShip();
    }

    @Override
    public boolean usingAI()          { return usingAI; }
    @Override
    public boolean isShip()          { return true;  }
    @Override
    public String name()             { return str(num)+":"+design.name(); }
    @Override
    public ShipDesign design()       { return design; }
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)       { return st.isMonster() || empire.aggressiveWith(st.empire, sys); }
    @Override
    public CombatStack ward()             { return ward; }
    @Override
    public boolean hasWard()              { return ward != null; }
    @Override
    public void ward(CombatStack st)      { ward = st; }
    @Override
    public int repulsorRange()            { return repulsorRange; }
    @Override
    public float designCost()             { return design.cost(); }
    @Override
    public int numWeapons()               { return weapons.size(); }
    @Override
    public ShipComponent weapon(int i)    { return weapons.get(i); }
    @Override
    public boolean hasTeleporting() { return design.allowsTeleporting(); }
    @Override
    public boolean canScan()        { return design.allowsScanning(); }
    @Override
    public boolean canRetreat()     { return !atLastColony && (maneuverability > 0); }
    @Override
    public float autoMissPct()      { return displacementPct; }
    @Override
    public ShipComponent selectedWeapon() { return weapons.get(selectedWeaponIndex); }
    @Override
    public boolean canDamage(CombatStack target) { return estimatedKills(target) > 0; }
    @Override
    public float bombDamageMod()   { return 0; }
    @Override
    public float blackHoleDef()    { return design.blackHoleDef(); }
    @Override
    public void recordKills(int num) { empire.shipLab().recordKills(design, num); }
    @Override
    public boolean ignoreRepulsors()    { return cloaked || canTeleport(); }
    @Override
    public void becomeDestroyed()    {
        fleet.removeShips(design.id(), num, true);
        empire.shipLab().recordDestruction(design, num);
        mgr.currentStack().recordKills(num);

        super.becomeDestroyed();
        for (ShipComponent c: weapons)
            c.becomeDestroyed();
    }
    @Override
    public boolean canFireWeapon() {
        for (CombatStack st: mgr.activeStacks()) {
            if ((empire != st.empire) && canAttack(st))
                return true;
        }
        return false;
    }
    @Override
    public boolean canFireWeaponAtTarget(CombatStack st) {
        if (st == null)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (!comp.isSpecial() && shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean hasBombs() {
        for (int i=0; i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (comp.groundAttacksOnly() && (roundsRemaining[i] > 0))
                return true;
        }
        return false;
    }
    @Override
    public int maxFiringRange(CombatStack tgt) {
        int maxRange = 0;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent wpn = weapons.get(i);
            if (wpn.groundAttacksOnly() && !tgt.isColony())
                continue;
            if (roundsRemaining[i]>0)
                maxRange = max(maxRange,weaponRange(wpn));
        }
        return maxRange;
    }
    @Override
    public int optimalFiringRange(CombatStack tgt) {
        // if only missile weapons, use that range
        // else use beam weapon range;
        int missileRange = -1;
        int weaponRange = -1;
        
        for (int i=0;i<weapons.size();i++) {
            ShipComponent wpn = weapons.get(i);
            // if we are bombing a planet, ignore other weapons
            //ail: if we count specials as weapons, we'll never get close when we have long-range-specials but short range-weapons
            if(wpn.isSpecial())
                continue;
            if (tgt.isColony() && wpn.groundAttacksOnly())
                return 1;
            else if (wpn.isMissileWeapon())
            {
                if(roundsRemaining[i] > 0)
                {
                    float mslRange = 1;
                    float requiredRange = 2 * tgt.maxMove * sqrt(2) - 0.7f;
                    mslRange = max(weaponRange(wpn) - requiredRange, mslRange);
                    missileRange = (int) max(tgt.repulsorRange() + 1, repulsorRange() + 1, missileRange, mslRange);
                }
            }
            else if(!wpn.groundAttacksOnly())
            {
                if(empire.ai().shipCaptain().useSmartRangeForBeams())
                {
                    if(tgt.maxFiringRange(this) > repulsorRange()) //If our enemy has a bigger range than our repulsors we close in no matter what
                        weaponRange = 1;
                    else
                        weaponRange = min(repulsorRange() + 1, max(weaponRange, weaponRange(wpn))); //ail: optimal firing-range for beam-weapons should be as close as possible but still take advantage of repulsor
                }
                else
                    weaponRange = weaponRange(wpn); //Use longest range for base-AI as it otherwise can't deal with repulsor-beam-ships because it doesn't have a loop around it's path-finding trying bigger ranges when range 1 is blocked
            }
        }
        return max(missileRange, weaponRange);
    }
    @Override
    public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        return design.missileInterceptPct(wpn);
    }
    public float designShipRepairPct() {
        float healPct = 0;
        for (int i=0;i<ShipDesign.maxSpecials();i++)
            healPct = max(healPct, design.special(i).shipRepairPct());
        return healPct;
    }
    private int designBeamRangeBonus() {
        int rng = 0;   
        for (int j=0;j<ShipDesign.maxSpecials();j++)
            rng += design.special(j).beamRangeBonus();
        return rng;
    }
    public void initShip() {
        int cols = empire.numColonies();
        atLastColony = ((empire == mgr.system().empire()) && (cols == 1));
        canCloak = design.allowsCloaking();
        cloak();

        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            if (validWeapon(i) && (design.wpnCount(i) > 0)) {
                weaponCount[weapons.size()] = design.wpnCount(i);
                weaponAttacks[weapons.size()] = design.weapon(i).attacksPerRound();
                roundsRemaining[weapons.size()] = design.weapon(i).shots();
                baseTurnsToFire[weapons.size()] = design.weapon(i).turnsToFire();
                wpnTurnsToFire[weapons.size()] = 1;
                weapons.add(design.weapon(i));
            }
        }
        for (int i=0;i<ShipDesign.maxSpecials();i++) {
            if (design.special(i).isWeapon()) {
                weaponCount[weapons.size()] = 1;
                weaponAttacks[weapons.size()] = 1;
                roundsRemaining[weapons.size()] = 1;
                baseTurnsToFire[weapons.size()] = 1;
                wpnTurnsToFire[weapons.size()] = 1;
                weapons.add(design.special(i));
            }
        }

        System.arraycopy(weaponAttacks, 0, shotsRemaining, 0, shotsRemaining.length);

        if (weapons.size() > 0)
            selectedWeaponIndex = 0;
    }
    @Override
    public int wpnCount(int i) { return design.wpnCount(i); }
    @Override
    public int shotsRemaining(int i) { return shotsRemaining[i]; }
    @Override
    public void reloadWeapons() {
        super.reloadWeapons();
        System.arraycopy(weaponAttacks, 0, shotsRemaining, 0, shotsRemaining.length);
        
        for (ShipComponent c: weapons)
            c.reload();
        //ail: reset selectedWeaponIndex too, so that ship will consistently start from the same weapon each new turn
        if (weapons.size() > 0)
            selectedWeaponIndex = 0;
    }
    @Override
    public void endTurn() {
        super.endTurn();
        boolean anyWeaponFired = false;
        for (int i=0;i<shotsRemaining.length;i++) {
            boolean thisWeaponFired = shotsRemaining[i]<weaponAttacks[i];
            anyWeaponFired = anyWeaponFired || thisWeaponFired;
            wpnTurnsToFire[i] = thisWeaponFired ? baseTurnsToFire[i] : wpnTurnsToFire[i]-1;          
        }
        
        if (!anyWeaponFired)
            cloak();
        if (bombardedThisTurn)
            fleet.bombarded(design.id());
        bombardedThisTurn = false;
    }
    @Override
    public void cloak() {
        if (!cloaked && canCloak) {
            cloaked = true;
            transparency = TechCloaking.TRANSPARENCY;
        }
    }
    @Override
    public void uncloak() {
        if (cloaked) {
            cloaked = false;
            transparency = 1;
        }
    }
    @Override
    public boolean retreatToSystem(StarSystem s) {
        if (s == null)
            return false;

        galaxy().ships.retreatSubfleet(fleet, design.id(), s.id);
        return true;
    }
    @Override
    public float initiative() {
        return design.initiative() + empire.shipInitiativeBonus();
    }
    @Override
    public boolean selectBestWeapon(CombatStack target) {
        if (target.destroyed())
            return false;
        if (shipComponentCanAttack(target, selectedWeaponIndex))
            return true;

        rotateToUsableWeapon(target);
        return shipComponentCanAttack(target, selectedWeaponIndex);
    }
    @Override
    public void rotateToUsableWeapon(CombatStack target) {
        int i = selectedWeaponIndex;
        int j = i;
        boolean looking = true;
        
        while (looking) {
            j++;
            if (j == weapons.size())
                j = 0;
            selectedWeaponIndex = j;
            if ((j == i) || shipComponentCanAttack(target, j))
                looking = false;
        }
    }
    @Override
    public int weaponIndex() {
        return selectedWeaponIndex;
    }
    @Override
    public void fireWeapon(CombatStack targetStack) {
        fireWeapon(targetStack, weaponIndex(), false);
    }
    @Override
    public void fireWeapon(CombatStack targetStack, int index, boolean allShots) {
        if (targetStack == null)
            return;

        if (targetStack.destroyed())
            return;
        selectedWeaponIndex = index;
        target = targetStack;
        target.damageSustained = 0;
        int shotsTaken = allShots ? shotsRemaining[index] : 1;

        // only fire if we have shots remaining... this is a missile concern
        if ((roundsRemaining[index] > 0) && (shotsRemaining[index] > 0)) {
            shotsRemaining[index] = shotsRemaining[index]-shotsTaken;
            uncloak();
            ShipComponent selectedWeapon = selectedWeapon();
            // some weapons (beams) can fire multiple per round
            int count = num*shotsTaken*weaponCount[index];
            if (selectedWeapon.isMissileWeapon()) {
                CombatStackMissile missile = new CombatStackMissile(this, (ShipWeaponMissileType) selectedWeapon, count);
                //log(fullName(), " launching ", missile.fullName(), " at ", targetStack.fullName());
                mgr.addStackToCombat(missile);
            }
            else {
                //log(fullName(), " firing ", str(count), " ", selectedWeapon.name(), " at ", targetStack.fullName());
                selectedWeapon.fireUpon(this, target, count);
            }
            if (selectedWeapon.isLimitedShotWeapon())
                roundsRemaining[index] = max(0, roundsRemaining[index]-1);
            if (target == null) {
                //log("TARGET IS NULL AFTER BEING FIRED UPON!");
                return;
            }
            if (target.damageSustained > 0)
               ; //log("weapon damage: ", str(target.damageSustained));
        }

        if (shotsRemaining[index] == 0)
            rotateToUsableWeapon(targetStack);
        target.damageSustained = 0;
        
        if (targetStack.isColony())
            bombardedThisTurn = true;
    }
    private boolean validWeapon(int i) {
        ShipWeapon wpn = design.weapon(i);
        return wpn.isWeapon() && !wpn.noWeapon();
    }
    @Override
    public boolean canAttack(CombatStack st) {
        if (st == null)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean canPotentiallyAttack(CombatStack st) {
        if (st == null)
            return false;
        if (empire.alliedWith(id(st.empire)))
            return false;
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanPotentiallyAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean isArmed() {
        for (int i=0;i<weapons.size();i++) {
            if (roundsRemaining[i] > 0) {
                // armed if: weapons are not bombs or if not allied with planet (& can bomb it)
                if (!weapons.get(i).groundAttacksOnly())
                    return true;
                if (mgr.system().isColonized() && !empire.alliedWith(mgr.system().empire().id))
                    return true;
            }
        }
        return false;
    }
    @Override
    public float estimatedKills(CombatStack target) {
        float kills = 0;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (!comp.isLimitedShotWeapon() || (roundsRemaining[i] > 0)) 
            {
                //ail: take attack and defense into account
                float hitPct = 1.0f;
                if(comp.isBeamWeapon())
                    hitPct = (5 + attackLevel - target.beamDefense()) / 10;
                if(comp.isMissileWeapon())
                    hitPct = (5 + attackLevel - target.missileDefense()) / 10;
                hitPct = max(.05f, hitPct);
                hitPct = min(hitPct, 1.0f);
                //ail: we totally have to consider the weapon-count too!
                kills += hitPct * comp.estimatedKills(this, target, weaponCount[i] * num);
            }
        }
        return kills;
    }
    @Override
    public boolean currentWeaponCanAttack(CombatStack target) {
        if (selectedWeapon() == null) 
            return false;

        if (target.inStasis || target.isMissile())
            return false;

        int wpn = selectedWeaponIndex;
        if (shotsRemaining[wpn] < 1) 
            return false;

        if (roundsRemaining[wpn]< 1) 
            return false;

        return shipComponentCanAttack(target, wpn);
    }
    public boolean shipComponentCanAttack(CombatStack target, int index) {
        if (target == null)
            return false;

        if (target.inStasis || target.isMissile())
            return false;

        if (index >= weapons.size())
            return false;
        
        ShipComponent shipWeapon = weapons.get(index);

        if ((shipWeapon == null) || !shipWeapon.isWeapon())
            return false;

        if (shotsRemaining[index] < 1)
            return false;
        
        if (wpnTurnsToFire[index] > 1)
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;

        return true;
    }
    private boolean shipComponentCanPotentiallyAttack(CombatStack target, int index) {
        if (target == null)
            return false;

        if (target.isMissile())
            return false;

        ShipComponent shipWeapon = weapons.get(index);

        if ((shipWeapon == null) || !shipWeapon.isWeapon())
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        return true;
    }
    @Override
    public int weaponNum(ShipComponent comp) {
        return weapons.indexOf(comp);
    }
    @Override
    public boolean shipComponentIsUsed(int index) {
        return shotsRemaining[index] < 1 || (roundsRemaining[index] < 1) || (wpnTurnsToFire[index] > 1);
    }
    @Override
    public boolean shipComponentIsOutOfMissiles(int index) {
        return weapon(index).isMissileWeapon() && roundsRemaining[index] == 0;
    }
    @Override
    public boolean shipComponentIsOutOfBombs(int index) {
        return weapon(index).groundAttacksOnly() && roundsRemaining[index] == 0;
    }
    @Override
    public String wpnName(int i) {
        ShipComponent wpn = weapons.get(i);
        if (wpn.isLimitedShotWeapon())
            return wpn.name()+":"+str(roundsRemaining[i]);
        else
            return wpn.name();
    }

    @Override
    public boolean shipComponentValidTarget(int index, CombatStack target) {
        ShipComponent shipWeapon = weapons.get(index);
        if (target == null)
            return false;
        if (empire == target.empire)
            return false;
        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;
        return true;
    }
    @Override
    public boolean shipComponentInRange(int index, CombatStack target) {
        ShipComponent shipWeapon = weapons.get(index);
        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;
        return true;
    }
    @Override
    public float targetShieldMod(ShipComponent c) {
        return design.targetShieldMod(c);
    }
    @Override
    public void loseShip() {
        int orig = num;
        super.loseShip();
        int shipsLost = orig-num;
        fleet.removeShips(design.id(), shipsLost, true);

        // record losses
        if (!destroyed())  // if destroyed, already recorded lose in super.loseShip()
            mgr.results().addShipDestroyed(design, shipsLost);
        
        empire.shipLab().recordDestruction(design, shipsLost);
        mgr.currentStack().recordKills(shipsLost);
    }
    @Override
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        Image img = design.image();

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale0 = min((float)stackW/w0, (float)stackH/h0)*9/10;

        int x1 = x;
        int y1 = y;
        int w1 = (int)(scale0*w0);
        int h1 = (int)(scale0*h0);

        int s1 = scaled(1);
        int s2 = scaled(2);
        
        if (scale != 1.0f) {
            int prevW = w1;
            int prevH = h1;
            w1 = (int) (w1*scale);
            h1 = (int) (h1*scale);
            x1 = x1 +(prevW-w1)/2;
            y1 = y1 +(prevH-h1)/2;
        }

        Composite prevComp = g.getComposite();
        if (transparency < 1) {
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,transparency);
            g.setComposite(ac);
        }
        if (reversed)  // XOR
            g.drawImage(img, x1, y1, x1+w1, y1+h1, w0, 0, 0, h0, ui);
        else
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, ui);

        if (transparency < 1)
            g.setComposite(prevComp);

        if (mgr.currentStack().isShip()) {
            CombatStackShip shipStack = (CombatStackShip) mgr.currentStack();
            if (!mgr.performingStackTurn ) {
                if (shipStack.design == design) {
                    Stroke prev = g.getStroke();
                    g.setStroke(BasePanel.stroke2);
                    g.setColor(ShipBattleUI.currentBorderC);
                    g.drawRect(x1+s1, y1+s1, stackW-s2, stackH-s2);
                    g.setStroke(prev);
                }
            }
        }

        int iconW = BasePanel.s18;
        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        int nameMgn = ui.showTacticalInfo() ? iconW + BasePanel.s5 : BasePanel.s5;
        String name = ui.showTacticalInfo() ? design.name() : text("SHIP_COMBAT_COUNT_NAME", str(num), design.name());
        scaledFont(g, name, stackW-nameMgn,16,8);
        int sw2 = g.getFontMetrics().stringWidth(name);
        int x1mgn = reversed || !ui.showTacticalInfo() ? x1 : x1+iconW;
        int x2 = max(x1mgn, x1mgn+((stackW-nameMgn-sw2)/2));

        g.setColor(Color.lightGray);
        drawString(g, name, x2, y2);
        
        if (inStasis) {
            g.setColor(TechStasisField.STASIS_COLOR);
            g.fillRect(x1,y1,stackW, stackH);
            String s = text("SHIP_COMBAT_STASIS");
            g.setFont(font(20));
            g.setColor(Color.white);
            int sw = g.getFontMetrics().stringWidth(s);
            int x3 = x1+(stackW-sw)/2;
            int y3 = y1+(stackH/2);
            drawBorderedString(g, s,x3,y3, Color.black, Color.white);
        }
        
        int mgn = BasePanel.s2;
        int x4 = x+mgn;
        int y4 = y+mgn;
        int w4 = stackW-mgn-mgn;
        int barH = BasePanel.s10;
        if (ui.showTacticalInfo()) {
            // draw health bar & hp
            g.setColor(healthBarBackC);
            g.fillRect(x4, y4, w4, barH);
            int w4a = (int)(w4*hits/maxHits);
            g.setColor(healthBarC);
            g.fillRect(x4, y4, w4a, barH);
            // draw ship count
            g.setColor(healthBarC);
            String numStr = str(num);
            g.setFont(narrowFont(20));
            int numW = g.getFontMetrics().stringWidth(numStr);
            int x6 = reversed ? x4: x4+w4-numW-BasePanel.s10;
            g.fillRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
            g.setColor(Color.white);
            Stroke prevStroke = g.getStroke();
            g.setStroke(BasePanel.stroke1);
            g.drawRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
            g.setStroke(prevStroke);
            g.drawString(numStr, x6+BasePanel.s5,y4+BasePanel.s18);
            // draw hit points
            g.setColor(Color.white);
            String hpStr = ""+(int)Math.ceil(hits)+"/"+(int)Math.ceil(maxHits);
            g.setFont(narrowFont(12));
            int hpW = g.getFontMetrics().stringWidth(hpStr);
            int x5 = reversed ? x4+((w4-hpW+numW)/2) : x4+((w4-hpW-numW)/2);
            g.drawString(hpStr, x5, y4+BasePanel.s9);
                
            
            ShipView view = player().shipViewFor(design());
            if (view != null) {
                // draw shield level
                g.setColor(shipShieldC);
                int x4a = reversed ? x4+w4-iconW : x4;
                int y4a =y4+barH+BasePanel.s1;
                g.fillOval(x4a, y4a, iconW, iconW);
                if (view.shieldKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(shieldLevel()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4a+BasePanel.s14);
                }
                //draw attack level
                g.setColor(shipAttackC);
                int y4b =y4a+iconW+BasePanel.s2;
                g.fillOval(x4a, y4b, iconW, iconW);
                if (view.attackLevelKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(attackLevel()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4b+BasePanel.s14);
                }
                //draw beam defense level
                g.setColor(shipBeamDefenseC);
                int y4c =y4b+iconW+BasePanel.s1;
                g.fillOval(x4a, y4c, iconW, iconW);
                if (view.beamDefenseKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(beamDefense()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4c+BasePanel.s14);
                }
                //draw missie defense level
                g.setColor(shipMissDefenseC);
                int y4d =y4c+iconW+BasePanel.s1;
                g.fillOval(x4a, y4d, iconW, iconW);
                if (view.missileDefenseKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(missileDefense()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4d+BasePanel.s14);
                }
            }
        }
    }
    public void drawRetreat() {
        if (!mgr.showAnimations())
            return;

        ShipBattleUI ui = mgr.ui;
        Graphics2D g = (Graphics2D) ui.getGraphics();

        Color portalColor = Color.white;
        g.setColor(portalColor);

        Rectangle rect = ui.combatGrids[x][y];

        int x0 = rect.x;
        int y0 = rect.y;
        int h0 = rect.height;
        int w0 = rect.width;
        
        playAudioClip("ShipRetreat");

        // open portal
        for (int i=0; i<10; i++) {
            ui.paintCellImmediately(x,y); 
           g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0+w0-(w0/16), y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            else
                g.fillOval(x0, y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            sleep(20);
        }

        // reverse ship
        reverse();
        ui.paintCellImmediately(x,y);
        g.setColor(portalColor);
        if (reversed)
            g.fillOval(x0, y0, w0/16, h0);
        else
            g.fillOval(x0+w0-(w0/16), y0, w0/16, h0);

        sleep(50);

        // move ship through portal
        g.setClip(rect);
        for (int i=0;i<25;i++) {
            offsetX = reversed ? offsetX-.04f : offsetX+.04f;
            ui.paintCellImmediately(x,y);
            g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0, y0, w0/16, h0);
            else
                g.fillOval(x0+w0-(w0/16), y0, w0/16, h0);
            sleep(30);
        }
        visible = false;
        g.setClip(null);

        // close portal
        for (int i=10; i>=0; i--) {
            ui.paintCellImmediately(x,y);
            g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0, y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            else
                g.fillOval(x0+w0-(w0/16), y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            sleep(20);
        }
        ui.paintCellImmediately(x,y);
    }
}
