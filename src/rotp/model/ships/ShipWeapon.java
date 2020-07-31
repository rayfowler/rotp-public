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

import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;

public class ShipWeapon extends ShipComponent {
    private static final long serialVersionUID = 1L;
    @Override
    public boolean isBeamWeapon()     { return false; }
    @Override
    public boolean isWeapon()         { return true; }
    public boolean noWeapon()         { return range() == 0; }
    public int minDamage()            { return 0; }
    public int maxDamage()            { return 0; }
    public int shots()                { return 1; }

    public int turnsToFire()          { return 1; }
    public float computerLevel()      { return 0; }
    public boolean canAttackPlanets() { return (!noWeapon() && (maxDamage() > 0)); }
    public float firepower()          { return firepower(0); }
    public float firepower(float shield) {
        float min = minDamage();
        float max = maxDamage();
        float shd = shield * shieldMod();
        float dmg = (summate(max-shd) - summate(min-shd-1)) / (max-min+1);
        return attacksPerRound() * dmg;
    }
    public float max(ShipDesign d, int i) {
        return noWeapon() ? 0 : max(0, (float)Math.floor(d.availableSpaceForWeaponSlot(i) / space(d))) ;
    }
    @Override
    public float estimatedKills(CombatStack source, CombatStack target, int num) {
        if (groundAttacksOnly() && !target.isColony())
            return 0;

        float shieldMod = source.targetShieldMod(this);
        float shieldLevel = shieldMod * target.shieldLevel();
        float dmg = firepower(shieldLevel);
		
		// modnar: account for planetDamageMod()
		// correctly calculate damage estimate for attacking colony (in round-about way)
		// beams and torpedoes do half damage against colonies, planetDamageMod() = 0.5f
		// other weapons have planetDamageMod() = 1.0f, so this correction would have no effect for them
		// average(beamMax/2-shield, beamMin/2-shield)  // correct formula
		// = average(beamMax-2*shield, beamMin-2*shield)/2  // equivalent formula used here
		if (target.isColony()) {
		    shieldLevel = shieldMod * target.shieldLevel() / planetDamageMod();
			dmg = firepower(shieldLevel) * planetDamageMod();
		}
		
        if (dmg == 0)
            return 0;

        if (isStreamingWeapon() && (dmg > target.maxHits()))
            return (num*dmg/target.maxHits());
        else if (dmg < target.maxHits())
            return  num / (float) Math.ceil(target.maxHits() / dmg);
        else
            return num;
    }
    @Override
    public float estimatedBombardDamage(CombatStack source, CombatStackColony target) {
        int num = bombardAttacks();
        float shieldMod = source.targetShieldMod(this);
        float shieldLevel = shieldMod * target.shieldLevel();
        return firepower(shieldLevel)* num;
    }
    @Override
    public float estimatedBombardDamage(ShipDesign des, CombatStackColony target) {
        int num = bombardAttacks();
        float shieldMod = des.targetShieldMod(this);
        float shieldLevel = shieldMod * target.shieldLevel();
        return firepower(shieldLevel)* num;
    }
    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
            case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
            case 1: return str((int)max(d, bank));
            case 2: int min = minDamage();
                int max = maxDamage();
                return (min == max) ? ""+min : text("SHIP_DESIGN_WEAPON_DMG_RANGE", min, max);
            case 3: return str((int)(cost(d)+(enginesRequired(d)*d.engine().cost(d))));
            case 4: return str((int)size(d));
            case 5: return str((int)power(d));
            case 6: return str((int)space(d));
            case 7: return desc();
        }
        return "";
    }
    public void drawIneffectiveAttack(CombatStack source, CombatStack target) {
        try {
            tech().drawIneffectiveAttack(source, target, source.weaponNum(this));
        }
        catch(Exception e) { }
    }
    public void drawUnsuccessfulAttack(CombatStack source, CombatStack target) {
        try {
            tech().drawUnsuccessfulAttack(source, target, source.weaponNum(this));
        }
        catch(Exception e) { }
    }
    public void drawSuccessfulAttack(CombatStack source, CombatStack target, float dmg) {
        try {
            tech().drawSuccessfulAttack(source, target, source.weaponNum(this), dmg);
        }
        catch(Exception e) { }              
    }
}
