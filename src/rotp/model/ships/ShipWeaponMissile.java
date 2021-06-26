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

import java.awt.Component;
import java.awt.Image;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.tech.TechMissileWeapon;

public final class ShipWeaponMissile extends ShipWeaponMissileType {
    private static final long serialVersionUID = 1L;
    private boolean multi = false;
    private final int range;
    private final int shots;
    private final float speed;

    public ShipWeaponMissile(TechMissileWeapon t, boolean m, int sh, int rng, float spd) {
        tech(t);
        range = rng;
        shots = sh;
        speed = spd;
        multi = m;
        sequence(t.level + .1f);
        if (multi)
            sequence(sequence() + .01f);
    }
    @Override
    public boolean isLimitedShotWeapon()  { return tech().isMissileBaseWeapon(); }
    @Override
    public TechMissileWeapon tech()       { return (TechMissileWeapon) super.tech(); }
    @Override
    public float computerLevel()         { return tech().computer; }
    @Override
    public Image image(int num)           { return tech().image(num); }
    @Override
    public float speed()                 { return speed; }
    @Override
    public int minDamage()                { return tech().damage(); }
    @Override
    public int maxDamage()                { return tech().damage(); }
    @Override
    public String name()                  { return super.name() + "-" + shots; }
    @Override
    public int range()                    { return range; }
    @Override
    public int shots()                    { return shots; }
    @Override
    public int scatterAttacks()             { return tech().attacks; }
    @Override
    public int bombardAttacks()           { return shots; }
    @Override
    public float cost(ShipDesign n)      { return multi ? 1.5f * super.cost(n) : super.cost(n); }
    @Override
    public float size(ShipDesign n)      { return multi ? 1.5f * super.size(n) : super.size(n); }
    @Override
    public float power(ShipDesign n)     { return multi ? 1.5f * super.power(n) : super.power(n); }
    @Override
    public String desc()                  { return multi ? tech().brief2() : tech().brief();  }
    @Override
    public float estimatedBombardDamage(CombatStack source, CombatStackColony target) {
        // missiles always do max damage on bombardment
        return (maxDamage()-target.shieldLevel()) * bombardAttacks() * target.missileDamageMod();
    }
    @Override
    public void drawAttackEffect(CombatStack source, CombatStack target, Component comp) { }
}
