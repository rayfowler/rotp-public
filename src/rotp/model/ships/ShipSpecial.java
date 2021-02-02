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

public class ShipSpecial extends ShipComponent {
    private static final long serialVersionUID = 1L;
    @Override
    public boolean isSpecial()         { return true;  }
    public boolean isFuelRange()       { return false;  }
    public boolean isColonySpecial()   { return false;  }
    public boolean allowsCloaking()    { return false;  }
    public boolean isPulsar()          { return false;  }
    public boolean isInertial()        { return false;  }
    public boolean isMissileShield()   { return false; }
    public boolean createsBlackHole()  { return false; }
    public boolean allowsTeleporting() { return false;  }
    public boolean allowsScanning()    { return false;  }
    public boolean canAttackPlanets()  { return isWeapon(); }
    public int speedBonus()            { return 0; }
    public int initiativeBonus()       { return 0; }
    public int attackBonus()           { return 0; }
    public int defenseBonus()          { return 0; }
    public float blackHoleDef()        { return 0; }
    public int beamRangeBonus()        { return 0; }
    public float beamShieldMod()       { return 1; }
    public float missileIntercept(ShipWeaponMissileType wpn)   { return 0; }
    public float shipRepairPct()       { return 0; }
    public float missPct()             { return 0; }
    public String designGroup()        { return name(); }

    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
            case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
            case 1: return str((int)(cost(d)+(enginesRequired(d)*d.engine().cost(d))));
            case 2: return str((int)size(d));
            case 3: return str((int)power(d));
            case 4: return str((int)space(d));
            case 5: return desc();
        }
        return "";
    }
}
