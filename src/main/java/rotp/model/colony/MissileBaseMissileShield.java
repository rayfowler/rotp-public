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
package rotp.model.colony;

import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechMissileShield;

public final class MissileBaseMissileShield extends MissileBaseComponent { 
    private static final long serialVersionUID = 1L;
    @Override
    public TechMissileShield tech() { return (TechMissileShield) super.tech(); }
    public float interceptPct(ShipWeaponMissileType missile) {
        float pct = tech().baseBlockPct - (tech().baseBlockAdjPerLevel * missile.tech().level);
        return Math.max(0,pct);
    }
    public MissileBaseMissileShield(TechMissileShield t) {
        tech(t);
    }
}
