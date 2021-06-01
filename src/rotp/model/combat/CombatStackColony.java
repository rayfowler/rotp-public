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

import rotp.model.colony.Colony;
import rotp.model.colony.MissileBase;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipWeapon;
import rotp.model.ships.ShipWeaponMissile;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechScanner;

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
        return (missileBase() == null)? 0 : missileBase().missileInterceptPct(missile);
    }
    @Override
    public float bioweaponDefense() {
        return (missileBase() == null)? 0 : missileBase().bombDefense();
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
}