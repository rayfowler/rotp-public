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

import rotp.model.ships.ShipWeaponMissile;
import rotp.model.tech.TechMissileWeapon;

public final class MissileBaseMissile extends MissileBaseComponent { 
    private static final long serialVersionUID = 1L;
    private final ShipWeaponMissile warhead;

    public ShipWeaponMissile warhead()    { return warhead; }

    public boolean isMissileWeapon()      { return true; }
    public boolean isLimitedShotWeapon()  { return false; }
    @Override
    public TechMissileWeapon tech()       { return warhead.tech(); }
    public MissileBaseMissile(TechMissileWeapon t, int cost) {
        warhead = new ShipWeaponMissile(t, false, 3, t.range*2, t.speed+1);
        tech(t);
        baseCost(cost);
    }
}
