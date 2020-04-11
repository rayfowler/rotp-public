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
package rotp.model.tech;

import rotp.model.colony.MissileBaseShield;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipShield;

public final class TechDeflectorShield extends Tech {
    public int damage;
    public MissileBaseShield baseShield;

    public TechDeflectorShield (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public boolean canBeMiniaturized()      { return true; }
    @Override
    public void init() {
        super.init();
        techType = Tech.DEFLECTOR_SHIELD;
        baseShield = new MissileBaseShield(this);

        switch(typeSeq) {
            case 0:
                damage = 0;
                baseShield.baseCost(0);
                free = true;
                break;
            case 1:
                damage = 1;
                baseShield.baseCost(75);
                free = true;
                break;
            case 2:
                damage = 2;
                baseShield.baseCost(89);
                break;
            case 3:
                damage = 3;
                baseShield.baseCost(103);
                break;
            case 4:
                damage = 4;
                baseShield.baseCost(117);
                break;
            case 5:
                damage = 5;
                baseShield.baseCost(130);
                break;
            case 6:
                damage = 6;
                baseShield.baseCost(144);
                break;
            case 7:
                damage = 7;
                baseShield.baseCost(158);
                break;
            case 8:
                damage = 9;
                baseShield.baseCost(172);
                break;
            case 9:
                damage = 11;
                baseShield.baseCost(186);
                break;
            case 10:
                damage = 13;
                baseShield.baseCost(199);
                break;
            case 11:
                damage = 15;
                baseShield.baseCost(213);
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return damage < c.tech().maxDeflectorShieldLevel();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c)) {
            c.tech().topDeflectorShieldTech(this);
            c.tech().updateMissileBase();
        }
        ShipShield sh = new ShipShield(this);
        c.shipLab().addShield(sh);
    }
    public int ranking() {
        return damage <= 7 ? damage : (int) (7 + ((damage-7) /2));
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return 2.5f + (.5f*ranking());
            case ShipDesign.MEDIUM : return 16  + (3*ranking());
            case ShipDesign.LARGE  : return 100 + (20*ranking());
            case ShipDesign.HUGE   : return 625 + (125*ranking());
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return 5*ranking();
            case ShipDesign.MEDIUM : return 5  + (15*ranking());
            case ShipDesign.LARGE  : return 30 + (30*ranking());
            case ShipDesign.HUGE   : return 125 + (125*ranking());
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return 5*ranking();
            case ShipDesign.MEDIUM : return 5  + (15*ranking());
            case ShipDesign.LARGE  : return 30 + (30*ranking());
            case ShipDesign.HUGE   : return 125 + (125*ranking());
        }
        return 0;
    }
}
