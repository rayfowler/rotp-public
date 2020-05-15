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
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.empires.Empire;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class ShipComponent implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private String techId;
    private float sequence = -1;
    private transient Tech tech;

    public static Comparator<ShipComponent> SELECTION_ORDER = (ShipComponent o1, ShipComponent o2) -> o1.sequence < o2.sequence ? -1 : 1;
    protected void tech(Tech t)          { techId = t.id(); tech = null; }
    public float sequence()             { return sequence; }
    public void sequence(float d)       { sequence = d; }
    public Tech tech() {
        if (tech == null)
            tech = tech(techId);
        return tech;
    }
    public boolean isSpecial()           { return false; }
    public boolean isWeapon()            { return false; }
    public boolean groundAttacksOnly()   { return false; }
    public boolean canAttackShips()      { return false; } // can a ship armed with only this item successfully destroy another ship?
    public boolean isMissileWeapon()     { return false; }
    public boolean isBeamWeapon()        { return false; }
    public boolean isLimitedShotWeapon() { return false; }
    public int attacksPerRound()         { return 1; }
    public boolean isStreamingWeapon()   { return false; }
    public int repulsorRange()           { return 0; }
    public int range()                   { return 0; }
    public float shieldMod()             { return 1; }
    public boolean heavy()               { return false; }
    public void drawAttack    (CombatStack source, CombatStack target, Component ui) { }
    public void drawAttackEffect(CombatStack source, CombatStack target, Component ui) { }
    public int bombardAttacks()          { return 0; }
    public float estimatedBioweaponDamage(CombatStack source, CombatStackColony stack) { return 0;}
    public float estimatedBombardDamage(CombatStack source, CombatStackColony stack) { return 0;}
    public float estimatedBombardDamage(ShipDesign d, CombatStackColony stack) { return 0;}

    public boolean validTarget(CombatStack st) { return true; }
    public boolean hasAttackEffect()     { return false;  }
    public int weaponWidth()             { return 0; }
    public int weaponSpread()            { return 0; }
    public boolean pellets()             { return false; }
    public boolean waves()               { return false; }
    public float planetDamageMod()       { return 1; }
    public void reload()                 { }
    public void becomeDestroyed()        { }
    public float estimatedKills(CombatStack source, CombatStack target, int num) {  return 0; }

    public void fireUpon(CombatStack source, CombatStack target, int count)      { 	}
    public boolean isNone()          { return nullTech(); }
    public String name()             { return techId == null ? "" : tech().item(); }
    public String desc()             { return techId == null ? "" : tech().brief(); }
    public float cost()              { return cost(null); }
    public boolean nullTech()        {
        Tech tech1 = tech();
        // tech == 0 is used for non-selections
        return (tech1 == null) || ((tech1.level() == 0) && (tech1.typeSeq == 0));
    }
    public String desc(ShipDesign d)  { return desc(); }
    public float cost(ShipDesign d)  {
        Empire emp = d == null ? null : d.empire();
        return nullTech() ? 0 : (float) Math.ceil(tech().costMiniaturization(emp) * tech().baseCost(d));
    }
    public float space(ShipDesign d) { return  nullTech() ? 0 : (float) Math.ceil(size(d) + engineSpaceRequired(d)); }
    public float power(ShipDesign d) { return nullTech() ? 0 :  tech().basePower(d); }
    public float size(ShipDesign d)  {
        Empire emp = d == null ? null : d.empire();
        return nullTech() ? 0 :   tech().sizeMiniaturization(emp) * tech().baseSize(d);
    }
    public float enginesRequired(ShipDesign d) {
        float req = power(d);
        float pow = d.engine().powerOutput();
        return req/pow;
    }
    public float engineSpaceRequired(ShipDesign d) {
        return (enginesRequired(d) * d.engine().size(d));
    }
    public String fieldValue(int n, ShipDesign d, int bank) { return ""; }
    public static int FLD_NUM = 0;
    public static ShipDesign FLD_DESIGN = null;
    public static int FLD_BANK = 0;
    public static Comparator<ShipComponent> FIELD_VALUE = new Comparator<ShipComponent>() {
        @Override
        public int compare(ShipComponent o1, ShipComponent o2) {
            String s1 = o1.fieldValue(FLD_NUM, FLD_DESIGN, FLD_BANK);
            String s2 = o2.fieldValue(FLD_NUM, FLD_DESIGN, FLD_BANK);
            Integer i1 = 0; try { i1 = Integer.valueOf(s1); } catch(NumberFormatException e) {}
            Integer i2 = 0; try { i2 = Integer.valueOf(s2); } catch(NumberFormatException e) {}
            if (Objects.equals(i1, i2))
                return s1.compareTo(s2);
            else
                return i1.compareTo(i2);
        }
    };
}
