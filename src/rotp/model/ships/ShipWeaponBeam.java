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

import java.awt.BasicStroke;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.tech.TechShipWeapon;

public final class ShipWeaponBeam extends ShipWeapon {
    private static final long serialVersionUID = 1L;
    final static float dash1[] = {1.0f, 4.0f};
    final static BasicStroke dashed0 = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
    final static BasicStroke dashed1 = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 1.0f);
    final static BasicStroke dashed2 = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 2.0f);
    final static BasicStroke dashed3 = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 3.0f);
    final static BasicStroke dashed4 = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 4.0f);
    final static BasicStroke[] pellets = { dashed4, dashed3, dashed2, dashed1, dashed0 };

    private final boolean heavy;

    @Override
    public boolean heavy()   { return heavy; }

    public ShipWeaponBeam(TechShipWeapon t, boolean h) {
        tech(t);
        heavy = h;
        sequence(t.level + .05f);
        if (heavy)
            sequence(sequence() + .01f);
    }
    @Override
    public TechShipWeapon tech()       { return (TechShipWeapon) super.tech(); }
    @Override
    public float computerLevel()       { return tech().computer; }
    @Override
    public boolean isBeamWeapon()      { return true; }
    @Override
    public boolean isStreamingWeapon() { return tech().streaming; }
    @Override
    public int attacksPerRound()    { return tech().attacksPerRound; }
    @Override
    public float shieldMod()          { return tech().enemyShieldMod; }
    @Override
    public float planetDamageMod()    { return CombatStackColony.BEAM_DAMAGE_MOD; }
    @Override
    public int bombardAttacks()        { return 10; }
    @Override
    public boolean canAttackShips()   { return true; }
    @Override
    public float estimatedBombardDamage(CombatStack source, CombatStackColony target) {
        return super.estimatedBombardDamage(source, target) * target.beamDamageMod();
    }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
        if (random() < target.autoMissPct()) {
            drawUnsuccessfulAttack(source, target);
            return;
        }
        float totalDamage = 0;
        float shieldMod = source.targetShieldMod(this)*shieldMod();
        
        // use attack/defense values to determine chance that weapon will hit
        int minDamage = minDamage();
        int maxDamage = maxDamage();
        int range = source.movePointsTo(target.x, target.y);
        float defense = target.beamDefense() + range - 1;
        float attack = source.attackLevel() + tech().computer;
        float hitPct = (5 + attack - defense) / 10;
        hitPct = max(.05f, hitPct);

        boolean successfullyHit = false;
        for (int i=0;i<count;i++) {
            if (random() < hitPct) {
                successfullyHit = true;
                float damage = roll(minDamage, maxDamage);
                if (isStreamingWeapon())
                    damage = target.takeStreamingDamage(damage, shieldMod);
                else
                    damage = target.takeBeamDamage(damage, shieldMod);
                totalDamage += damage;
            }
        }
        if (totalDamage > 0)
            drawSuccessfulAttack(source, target, totalDamage);
        else if (successfullyHit)
            drawIneffectiveAttack(source, target);
        else
            drawUnsuccessfulAttack(source, target);
    }
    @Override
    public int range()                { return heavy ? tech().heavyRange : tech().range; }
    @Override
    public int minDamage()            { return heavy ? tech().heavyDamageLow() : tech().damageLow(); }
    @Override
    public int maxDamage()            { return heavy ? tech().heavyDamageHigh() : tech().damageHigh(); }
    @Override
    public float cost(ShipDesign n)  { return heavy ? 3*super.cost(n) : super.cost(n); }
    @Override
    public float size(ShipDesign n)  { return heavy ? 3*super.size(n) : super.size(n); }
    @Override
    public float power(ShipDesign n) { return heavy ? 3*super.power(n) : super.power(n); }
    @Override
    public String name()              { return heavy ? tech().item2() : tech().item(); }
    @Override
    public int weaponWidth()          { return heavy ? tech().weaponWidth+1 : tech().weaponWidth; }
    @Override
    public int weaponSpread()         { return heavy ? tech().weaponSpread+1 : tech().weaponSpread; }
    @Override
    public boolean pellets()          { return tech().pellets; }
    @Override
    public boolean waves()            { return tech().waves; }
}
