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
import rotp.model.tech.TechBombWeapon;

public final class ShipWeaponBomb extends ShipWeapon {
    private static final long serialVersionUID = 1L;
    public ShipWeaponBomb(TechBombWeapon t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public boolean groundAttacksOnly() { return true; }
    @Override
    public TechBombWeapon tech()       { return (TechBombWeapon) super.tech(); }
    @Override
    public int range()                 { return tech().range(); }
    @Override
    public int minDamage()             { return tech().damageLow(); }
    @Override
    public int maxDamage()             { return tech().damageHigh(); }
    @Override
    public int bombardAttacks()        { return 10; }
    @Override
    public boolean hasAttackEffect()   { return true; }
    @Override
    public boolean pellets()           { return true; }
    @Override
    public int shots()                 { return 10; }
    @Override
    public boolean isLimitedShotWeapon() { return true; }
    @Override
    public float estimatedBombardDamage(CombatStack source, CombatStackColony target) {
        return super.estimatedBombardDamage(source, target) * target.bombDamageMod();
    }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
        float defense = target.bombDefense();
        float attack = source.attackLevel();
        float pct = (5 + attack - defense) / 10;
        pct = max(.05f, pct);

        float totalDamage = 0;
        float shieldMod = source.targetShieldMod(this)*shieldMod();
        boolean successfullyHit = false;
        for (int i=0;i<count;i++) {
            if (random() < pct) { 
                successfullyHit = true;
                if (!target.destroyed()) {
                    float damage = roll(minDamage(), maxDamage());
                    damage = target.takeBombDamage(damage, shieldMod);
                    totalDamage += damage;
                }
            }
        }
        if (totalDamage > 0)
            drawSuccessfulAttack(source, target, totalDamage);
        else if (successfullyHit)
            drawIneffectiveAttack(source, target);
        else
            drawUnsuccessfulAttack(source, target);
    }
}
