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

import java.io.Serializable;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.util.Base;

public class MissileBase implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final float MINIMUM_COST = 120;
    public static final float BASE_HP = 50;

    private MissileBaseArmor armor;
    private MissileBaseMissile missile;
    private MissileBaseMissile scatterPack;
    private MissileBaseShield shield;
    private MissileBaseComputer computer;
    private MissileBaseECM ecm;
    private MissileBaseMissileShield missileShield;

    public void armor(MissileBaseArmor m)                  { armor = m; }
    public MissileBaseMissile missile()                    { return missile; }
    public void missile(MissileBaseMissile m)              { missile = m; }
    public MissileBaseMissile scatterPack()                { return scatterPack; }
    public void scatterPack(MissileBaseMissile m)          { scatterPack = m; }
    public void shield(MissileBaseShield m)                { shield = m; }
    public void computer(MissileBaseComputer m)            { computer = m; }
    public void ecm(MissileBaseECM m)                      { ecm = m; }
    public void missileShield(MissileBaseMissileShield m)  { missileShield = m; }

    public float maxHits()        { return BASE_HP * armor.tech().hitsAdj; }
    public float ecmLevel()       { return ecm == null ? 0 : ecm.level(); }
    public float computerLevel()  { return computer.level() + 1; }  // attack level is computer + 1 for scanner
    public float missileDefense() { return computer.level() + ecmLevel(); }
    public float beamDefense()    { return computer.level(); }
    public float bombDefense()    { return 1 + ecmLevel(); }
    public float missileInterceptPct(ShipWeaponMissileType missile) {
        return missileShield == null ? 0 : missileShield.interceptPct(missile);
    }
    public float firepower(float shield) {
        return max(missile.warhead().firepower(shield),scatterPack.warhead().firepower(shield));
    }
    public float cost(Empire emp) {
        float cost = MINIMUM_COST;
        if (armor != null)
            cost += armor.cost(emp);
        if (shield != null)
            cost += shield.cost(emp);
        if (missile != null)
            cost += missile.cost(emp);
        if (scatterPack != null)
            cost += scatterPack.cost(emp);
        if (missileShield != null)
            cost += missileShield.cost(emp);
        if (computer != null)
            cost += computer.cost(emp);
        if (ecm != null)
            cost += ecm.cost(emp);
        return cost;
    }
}
