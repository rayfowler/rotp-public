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
import rotp.model.tech.TechBlackHole;

public final class ShipSpecialBlackHole extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialBlackHole(TechBlackHole t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public boolean isWeapon()         { return true; }
    @Override
    public TechBlackHole tech()       { return (TechBlackHole) super.tech(); }
    @Override
    public int range()                { return tech().range; }
    @Override
    public boolean canAttackPlanets() { return true; }
    @Override
    public boolean canAttackShips()   { return true; }
    @Override
    public float estimatedKills(CombatStack source, CombatStack target, int num) {
        //base pct is random from 25 to 100
        float pct = .625f;
        //-2% per shield level
        pct -= (target.shieldLevel() / 50);
        // - effect of inertial specials
        pct -= target.blackHoleDef();
		// modnar: bug fix for negative pct, force pct to be at least 0.0
		pct = (float)Math.max(0.0f, pct);
        return target.num * pct;
    }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
        float pct = (random()*.75f) + .25f;
		// modnar: bug fix for Black Hole damage numbers
		float pctLoss = (float)Math.max(0.0f, pct - (target.shieldLevel() / 50) - target.blackHoleDef());
        int dmg = (int) (pctLoss*target.maxHits*target.num);
        tech().drawSpecialAttack(source, target, count, dmg);
        target.takeBlackHoleDamage(pct);
    }
 }
