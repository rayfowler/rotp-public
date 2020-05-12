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
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.*;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public class CombatStackShip extends CombatStack {
    static Color healthBarC = new Color(0,96,0);
    static Color healthBorderC = new Color(64,192,64);
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
        usingAI = (empire == null) || empire.isAI();
        captain = empire.ai().shipCaptain();
        origNum = num = fl.num(index);
        startingMaxHits = maxHits = design.hits();
        maxMove = design.moveRange();
        maxShield = design.shieldLevel();
        attackLevel = design.attackLevel() + empire.race().shipAttackBonus();
        maneuverability = design.maneuverability();
        repulsorRange = design.repulsorRange();
        hits = maxHits;
        move = maxMove;
        shield = maxShield;
        missileDefense = design.missileDefense() + empire.race().shipDefenseBonus();
        beamDefense = design.beamDefense() + empire.race().shipDefenseBonus();
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
            if (!tgt.isColony() || !wpn.groundAttacksOnly())
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
        
        int maxX = ShipCombatManager.maxX;
        int maxY = ShipCombatManager.maxY;
        float maxRetreatMove = 2*tgt.maxMove; // allow 2 turns to retreat before missiles hit
        if (maxRetreatMove > 0) {
            // won't retreat more than the distance to the nearest corner of the map
            float moveToCorner = min(distance(tgt.x,tgt.y,0,0), distance(tgt.x,tgt.y,0,maxY), distance(tgt.x,tgt.y,maxX,0), distance(tgt.x,tgt.y,maxX,maxY));
            maxRetreatMove = min(maxRetreatMove, moveToCorner);
        }
        for (int i=0;i<weapons.size();i++) {
            ShipComponent wpn = weapons.get(i);
            // if we are bombing a planet, ignore other weapons
            if (tgt.isColony() || wpn.groundAttacksOnly())
                return 1;
            else if (wpn.isMissileWeapon()) 
                missileRange = (int) max(1, missileRange, weaponRange(wpn)-maxRetreatMove);
            else
                weaponRange = max(weaponRange,weaponRange(wpn));
        }
        return weaponRange < 0 ? missileRange: weaponRange;
    }
    public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        return design.missileInterceptPct(wpn);
    }
    private float designShipRepairPct() {
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
        return design.initiative() + empire.race().shipInitiativeBonus();
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
                log(fullName(), " launching ", missile.fullName(), " at ", targetStack.fullName());
                mgr.addStackToCombat(missile);
            }
            else {
                log(fullName(), " firing ", str(count), " ", selectedWeapon.name(), " at ", targetStack.fullName());
                selectedWeapon.fireUpon(this, target, count);
            }
            if (target == null) 
                log("TARGET IS NULL AFTER BEING FIRED UPON!");
            if (selectedWeapon.isLimitedShotWeapon())
                roundsRemaining[index] = max(0, roundsRemaining[index]-1);
            if (target.damageSustained > 0)
                log("weapon damage: ", str(target.damageSustained));
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
                kills += comp.estimatedKills(this, target, num * roundsRemaining[i]);
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
    private boolean shipComponentCanAttack(CombatStack target, int index) {
        if (target == null)
            return false;

        if (target.inStasis || target.isMissile())
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
        return (shotsRemaining[index] < 1)  || (roundsRemaining[index] < 1);
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

        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        String name = text("SHIP_COMBAT_COUNT_NAME", str(num), design.name());
        int sw2 = g.getFontMetrics().stringWidth(name);
        int x2 = max(x1, x1+((stackW-sw2)/2));

        g.setColor(Color.lightGray);
        g.drawString(name, x2, y2);

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
