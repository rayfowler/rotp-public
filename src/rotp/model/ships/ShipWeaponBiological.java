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
import rotp.model.tech.TechBiologicalWeapon;

public final class ShipWeaponBiological extends ShipWeapon {
    private static final long serialVersionUID = 1L;
    public ShipWeaponBiological(TechBiologicalWeapon t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public TechBiologicalWeapon tech()      { return (TechBiologicalWeapon) super.tech(); }
    @Override
    public boolean groundAttacksOnly()      { return true; }
    @Override
    public boolean isBioWeapon()            { return true; }
    @Override
    public int range()                      { return 1;  }
    @Override
    public int minDamage()                  { return tech().minDamage; }
    @Override
    public int maxDamage()                  { return tech().maxDamage; }
    @Override
    public int bombardAttacks()             { return 5; }
    @Override
    public int shots()                      { return 5; }
    @Override
    public boolean isLimitedShotWeapon()    { return true; }
    @Override
    public float estimatedBioweaponDamage(CombatStack source, CombatStackColony target) {
        float antidote = target.empire.tech().antidoteLevel();
        // this is simplified and inexact when antidote > 1
        // and doesn't consider att vs def
        float min = max(0, minDamage()-antidote);
        float max = max(0, maxDamage()-antidote);
        float avg = (min+max)/2;
        return bombardAttacks()*avg;
    }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
        source.usedBioweapons();
        float defense = target.bioweaponDefense();
        float attack = source.attackLevel();
        float pct = (5 + attack - defense) / 10;
        pct = max(.05f, pct);

        float totalDamage = 0;
        for (int i=0;i<count;i++) {
            if (random() < pct) 
            {
                float currentDamage = roll(minDamage(), maxDamage());
                currentDamage = max(0, currentDamage - target.empire.tech().antidoteLevel());
                totalDamage += currentDamage;
            }
        }
        if (totalDamage > 0)
            drawSuccessfulAttack(source, target, totalDamage);
        else
            drawIneffectiveAttack(source, target);
        target.takeBioweaponDamage(totalDamage);
    }
}
