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
import rotp.model.combat.CombatStackShip;
import rotp.model.tech.TechRepulsor;

public final class ShipSpecialRepulsor extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialRepulsor(TechRepulsor t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public int repulsorRange()           { return 1; }
    @Override
    public String name()                 { return tech().name(); }
    @Override
    public String desc()                 { return tech().brief(); }
    @Override
    public boolean isWeapon()            { return true; }
    @Override
    public int range()                   { return 1; }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
        makeSuccessfulAttack(source, target);
    }
    private void makeSuccessfulAttack(CombatStack source, CombatStack target) {
        if (target.isShip()|| target.isMonster()) {
            tech().drawSpecialAttack(source, target, 1, 0);
        }
    }
}
