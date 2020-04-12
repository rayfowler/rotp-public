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

import rotp.model.empires.Empire;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipSpecialInertial;

public final class TechShipInertial extends Tech {
    public int defenseBonus;
    public int combatSpeedBonus;
    public float blackHoleEffectMod;

    public TechShipInertial(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.SHIP_INERTIAL;

        switch(typeSeq) {
            case 0:
                defenseBonus = 2;
                combatSpeedBonus = 1;
                blackHoleEffectMod = .15f;
                break;
            case 1:
                defenseBonus = 4;
                combatSpeedBonus = 2;
                blackHoleEffectMod = .3f;
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topShipInertialTech() != null) && (level < c.tech().topShipInertialTech().level);
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (! isObsolete(c))
            c.tech().topShipInertialTech(this);

        ShipSpecialInertial sh = new ShipSpecialInertial(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 2;
                    case ShipDesign.MEDIUM: return 7.5f;
                    case ShipDesign.LARGE: return 50;
                    case ShipDesign.HUGE: return 270;
                }
                break;
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 6;
                    case ShipDesign.MEDIUM: return 20;
                    case ShipDesign.LARGE: return 150;
                    case ShipDesign.HUGE: return 500;
                }
                break;
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 4;
                    case ShipDesign.MEDIUM: return 20;
                    case ShipDesign.LARGE: return 100;
                    case ShipDesign.HUGE: return 500;
                }
                break;
            case 1:
            default:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 6;
                    case ShipDesign.MEDIUM: return 30;
                    case ShipDesign.LARGE: return 150;
                    case ShipDesign.HUGE: return 750;
                }
                break;
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 8;
                    case ShipDesign.MEDIUM: return 40;
                    case ShipDesign.LARGE: return 200;
                    case ShipDesign.HUGE: return 1000;
                }
                break;
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 12;
                    case ShipDesign.MEDIUM: return 60;
                    case ShipDesign.LARGE: return 300;
                    case ShipDesign.HUGE: return 1500;
                }
                break;
        }
        return 0;
    }
}
