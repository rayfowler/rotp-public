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

import rotp.model.tech.TechShipInertial;

public final class ShipSpecialInertial extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialInertial (TechShipInertial t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public String designGroup()              { return "Inertial"; }
    @Override
    public boolean isInertial()        { return true;  }
    @Override
    public TechShipInertial tech() { return (TechShipInertial) super.tech(); }
    @Override
    public int speedBonus()        { return tech().combatSpeedBonus; }
    @Override
    public int defenseBonus()      { return tech().defenseBonus; }
    @Override
    public float blackHoleDef()   { return tech().blackHoleEffectMod; }
}
