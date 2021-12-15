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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import rotp.model.colony.Colony;
import rotp.model.colony.MissileBase;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipWeapon;
import rotp.model.ships.ShipWeaponMissile;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechScanner;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public class CombatStackColony extends CombatStack {
    public static final float BEAM_DAMAGE_MOD = .5f;
    public static final float TORPEDO_DAMAGE_MOD = .5f;
    public Colony colony;
    public ShipWeaponMissile missile;
    public ShipWeaponMissile scatterPack;
    public float targetDmg = 0;
    public float targetKills = 0;
    public boolean retreatAllowed = true;
    public boolean missileFired = false;
    public boolean colonyDestroyed = false;
    public float startingPop = 0;
    public float startingFactories = 0;
    private float planetaryShieldLevel = 0;
    private boolean usingAI = true;
    public CombatStackColony(Colony col, ShipCombatManager m) {
        mgr = m;
        colony = col;
        empire = colony.empire();
        usingAI = (empire == null) || empire.isAIControlled();
        captain = empire.ai().shipCaptain();
        MissileBase mBase = missileBase();
        origNum = num = (int) colony.defense().bases();
        missile = mBase.missile().warhead();
        scatterPack = mBase.scatterPack() == null ?  null : mBase.scatterPack().warhead();
        maxShield = colony.defense().missileShieldLevel();
        startingMaxHits = maxHits = mBase.maxHits();
        attackLevel = mBase.computerLevel();
        missileDefense = mBase.missileDefense();
        beamDefense = mBase.beamDefense();
        startingPop = colony.population();
        startingFactories = colony.industry().factories();
        planetaryShieldLevel = colony.defense().shieldLevel();
        hits = maxHits;
        shield = maxShield;
    }
    @Override
    public boolean usingAI()          { return usingAI; }
    @Override
    public String toString() {
        if (target != null)
            return concat(shortString(), "  targeting: [", target.shortString(), "]");
        else
            return shortString();
    }
    @Override
    public String shortString() {
        return concat("Colony in: ", colony.starSystem().name(), " -- bases: ", str(num), " at:", str(x), ",", str(y));
    }
    public float populationLost()   { return colony.destroyed() ? (int) startingPop : (int) startingPop - (int) colony.population(); }
    public float factoriesLost()    { return colony.destroyed() ? (int) startingFactories : (int) startingFactories - (int) colony.industry().factories(); }
    @Override
    public void reloadWeapons() {
        super.reloadWeapons();
        missileFired = false;
    }
    @Override
    public boolean selectBestWeapon(CombatStack target)       { return (num > 0) && !missileFired; }
    @Override
    public boolean isColony()         { return true; }
    @Override
    public int shots()                { return num; }
    @Override
    public String name()              { return colony.name(); }
    final public MissileBase missileBase()  { return colony.defense().missileBase(); }
    @Override
    public float designCost()          { return  missileBase().cost(colony.empire()); }
    @Override
    public float initiative()         { return isArmed() ? TechScanner.BATTLE_SCANNER_INITIATIVE : -1; }
    @Override
    public boolean hasWard()          { return true; }
    @Override
    public CombatStack ward()         { return this; }
    @Override
    public boolean isArmed()          { return num > 0; }
    @Override
    public boolean canScan()          { return true; }
    @Override
    public boolean canFireWeapon()    { return isArmed() && !missileFired; }
    @Override
    public float torpedoDamageMod()   { return TORPEDO_DAMAGE_MOD; }
    @Override
    public boolean destroyed()        { return colony.destroyed(); }
    @Override
    public float beamDamageMod()      { return BEAM_DAMAGE_MOD; }
    @Override
    public float missileInterceptPct(ShipWeaponMissileType missile)   {
        return (missileBase() == null || !isArmed())? 0 : missileBase().missileInterceptPct(missile);
    }
    @Override
    public float beamDefense() {
        return (missileBase() == null || !isArmed())? 0 : missileBase().beamDefense();
    }
    @Override
    public float missileDefense() {
        return (missileBase() == null || !isArmed())? 0 : missileBase().missileDefense();
    }
    @Override
    public float bioweaponDefense() {
        return (missileBase() == null || !isArmed())? 0 : missileBase().bombDefense();
    }
    @Override
    public float bombDefense() {
        return (missileBase() == null || !isArmed())? 0 : missileBase().bombDefense();
    }
    @Override
    public float shieldLevel() {
        return num > 0 ? shield : planetaryShieldLevel;
    }
    @Override
    public void assignCollateralDamage(float damage) {
        attacked = true;
        colony.takeCollateralDamage(damage);
        if (colony.destroyed())
            colonyDestroyed = true;
    }
    @Override
    public void takeBioweaponDamage(float damage) {
        colony.takeBioweaponDamage(damage);
        
        if (colony.destroyed()) {
            mgr.destroyStack(this);
            colonyDestroyed = true;
        }
    }
    @Override
    public void loseShip() {
        super.loseShip();
        colony.defense().bases(num);
        mgr.results().basesDestroyed = origNum - num;
    }
    @Override
    public float estimatedKills(CombatStack target) {
        //ail: take attack and defense into account
        float hitPct = (5 + attackLevel - target.missileDefense) / 10;
        hitPct = max(.05f, hitPct);
        //ail: each missile base fires 3 missiles, even in the estimate
        float missileDamage = hitPct * missile.estimatedKills(this, target, 3*num);
        float scatterDamage = 0;
        if(scatterPack != null)
            scatterDamage = hitPct * scatterPack.estimatedKills(this, target, 3*num);
        return max(missileDamage, scatterDamage);
    }
    @Override
    public boolean canAttack(CombatStack target) {
        return (num > 0) && currentWeaponCanAttack(target);
    }
    @Override
    public boolean canPotentiallyAttack(CombatStack target)   { return (num > 0) && !empire.alliedWith(id(target.empire)); }
    @Override
    public boolean canMove()               { return false; }
    @Override
    public boolean canMoveTo(int x, int y) { return false; }
    @Override
    public boolean canTeleport()           { return false; }
    @Override
    public boolean currentWeaponCanAttack(CombatStack target) {
        if (target == null)
            return false;
        if (target.inStasis || missileFired || (num == 0))
            return false;

        // missile range is floatd for planetary bases
        int dist = Math.abs(x-target.x);
        return ((selectedWeapon().range() * 2) >= dist);
    }
    @Override
    public ShipWeaponMissile selectedWeapon() { return missile; }
    @Override
    public void fireWeapon(CombatStack newTarget) {
        //ail: @Ray: My AI usually doesn't build missile-bases and there's also no hook in it to control the used missile type, so'll do some very basic logic here to help the auto-play and the other AIs pick the right missile
        int missileToUse = 0;
        float bestDamage = 0;
        for(int i = 0; i < numWeapons(); ++i)
        {
            float currentDamage = ((ShipWeapon)weapon(i)).firepower(newTarget.shieldLevel());
            if(currentDamage > bestDamage) {
                missileToUse = i;
                bestDamage = currentDamage;
            }
        }
        fireWeapon(newTarget, missileToUse);
    }
    @Override
    public void fireWeapon(CombatStack newTarget, int i) {
        fireWeapon(newTarget, i, false);
    }
    @Override
    public void fireWeapon(CombatStack newTarget, int i, boolean b) {
        if (missileFired)
            return;
        target = newTarget;
        // each missile base fires 3 missiles
        ShipWeaponMissile missileType = missile;
        if(i > 0 && numWeapons() > 0)
            missileType = scatterPack;
        CombatStackMissile missileStack = new CombatStackMissile(this, missileType, 3*num);
        mgr.addStackToCombat(missileStack);
        missileFired = true;
    }
    public void killRebels() {
        if (colony.destroyed())
            return;
        float pctSurvive = min(1, colony.population() / startingPop);
        int newRebels = (int) Math.ceil(pctSurvive*colony.rebels());
        colony.rebels(newRebels);
    }
    @Override
    public int optimalFiringRange(CombatStack target) {
        return 9;
    }
    @Override
    public boolean shipComponentIsUsed(int index) {
        return missileFired;
    }
    @Override
    public boolean shipComponentValidTarget(int index, CombatStack target) {
        if (target == null)
            return false;
        if (empire == target.empire)
            return false;
        return true;
    }
    @Override
    public boolean shipComponentInRange(int index, CombatStack target) {
        return true;
    }
    @Override
    public boolean shipComponentIsOutOfMissiles(int index)                 { return false; }
    @Override
    public int wpnCount(int i)          { return num; }
    @Override
    public int numWeapons()             { 
        if (num == 0)
            return 0;
        return scatterPack == null ? 1 : 2;
    }
    @Override
    public ShipComponent weapon(int i)  { 
        if (i == 0)
            return missile; 
        else 
            return scatterPack;
    }
    @Override
    public String wpnName(int i) {
        if (i == 0)
            return missile.name();
        else
            return scatterPack.name();
    }
    @Override
    public int maxFiringRange(CombatStack tgt) {
        int maxRange = 0;
        if(num > 0)
            maxRange = 9;
        return maxRange;
    }
    @Override
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        int x1 = x;
        int y1 = y;

        int sysId = colony.starSystem().id;
        String sname = player().sv.name(sysId);
        if (sname == null)
            sname = "";
        int iconW = BasePanel.s30;
        int iconH = BasePanel.s18;
        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        int nameMgn = BasePanel.s5;
        String name = ui.showTacticalInfo() || (num == 0) ? sname : text("SHIP_COMBAT_COUNT_NAME", str(num), sname);
        scaledFont(g, name, stackW-nameMgn,16,8);
        int sw2 = g.getFontMetrics().stringWidth(name);
        int x2 = max(x1, x1+((stackW-nameMgn-sw2)/2));

        g.setColor(Color.lightGray);
        drawString(g, name, x2, y2);
        
        
        int mgn = BasePanel.s2;
        int x4 = x+mgn;
        int y4 = y+mgn;
        int w4 = stackW-mgn-mgn;
        int barH = BasePanel.s10;
        if (ui.showTacticalInfo()) {
            // draw health bar & hp
            if (num > 0) {
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
            }
            int shieldLevel = (int)shieldLevel();
            // draw shield level
            g.setColor(sysShieldC);
            int x4a = reversed ? x4+w4-iconW : x4;
            int y4a =y4+barH+BasePanel.s1;
            g.fillOval(x4a, y4a, iconW, iconH);
            g.setColor(Color.white);
            String valStr = str((int)Math.ceil(shieldLevel));
            g.setFont(narrowFont(16));
            int valW = g.getFontMetrics().stringWidth(valStr);
            g.drawString(valStr, x4a+((iconW-valW)/2), y4a+BasePanel.s14);

            //draw population level
            int popLevel = colony == null ? 0 : (int) Math.ceil(colony.population());
            g.setColor(sysPopC);
            int y4b =y4a+iconH+BasePanel.s2;
            g.fillOval(x4a, y4b, iconW, iconH);
            g.setColor(Color.white);
            valStr = str(popLevel);
            g.setFont(narrowFont(16));
            valW = g.getFontMetrics().stringWidth(valStr);
            g.drawString(valStr, x4a+((iconW-valW)/2), y4b+BasePanel.s14);
            //draw factories
            int factLevel = (colony == null) ? 0 : (int) Math.ceil(colony.industry().factories());
            g.setColor(sysFactoryC);
            int y4c =y4b+iconH+BasePanel.s1;
            g.fillOval(x4a, y4c, iconW, iconH);
            g.setColor(Color.white);
            valStr = str(factLevel);
            g.setFont(narrowFont(16));
            valW = g.getFontMetrics().stringWidth(valStr);
            g.drawString(valStr, x4a+((iconW-valW)/2), y4c+BasePanel.s14);
        }
    }
}