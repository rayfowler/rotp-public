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

import rotp.model.colony.MissileBaseArmor;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipDesign;

public final class TechArmor extends Tech {
    public float hitsAdj;
    public int groundAttackBonus;
    public int transportHP;
    public MissileBaseArmor baseArmor;

    public TechArmor (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    public String shortName()    {  return item(); }
    @Override
    public boolean canBeMiniaturized()      { return true; }
    @Override
    public void init() {
        super.init();

        techType = Tech.ARMOR;
        baseArmor = new MissileBaseArmor(this);

        switch(typeSeq) {
            case 0:
                hitsAdj = 1;
                groundAttackBonus = 0;
                transportHP = 15;
                baseArmor.baseCost(0);
                break;
            case 1:
                hitsAdj = 1.5f;
                groundAttackBonus = 5;
                transportHP = 22;
                baseArmor.baseCost(36);
                break;
            case 2:
                hitsAdj = 2;
                groundAttackBonus = 10;
                transportHP = 30;
                baseArmor.baseCost(60);
                break;
            case 3:
                hitsAdj = 2.5f;
                groundAttackBonus = 15;
                transportHP = 37;
                baseArmor.baseCost(90);
                break;
            case 4:
                hitsAdj = 3;
                groundAttackBonus = 20;
                transportHP = 45;
                baseArmor.baseCost(120);
                break;
            case 5:
                hitsAdj = 3.5f;
                groundAttackBonus = 25;
                transportHP = 52;
                baseArmor.baseCost(150);
                break;
            case 6:
                hitsAdj = 4;
                groundAttackBonus = 30;
                transportHP = 60;
                baseArmor.baseCost(180);
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 3; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c)     { return groundAttackBonus < c.tech().armorGroundBonus(); }
    @Override
    public float baseValue(Empire c)   { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c)) {
            c.tech().topArmorTech(this);
            c.tech().updateMissileBase();
        }
        ShipArmor sh = new ShipArmor(this, false);
        c.shipLab().addArmor(sh);
        ShipArmor sh2 = new ShipArmor(this, true);
        c.shipLab().addArmor(sh2);
    }
    public float baseCost(int size, boolean reinforced) {
        int baseCost;

        switch (size) {
            case ShipDesign.SMALL:
                return reinforced ? Math.max(2, typeSeq * 3) : typeSeq *2;
            case ShipDesign.MEDIUM:
                return reinforced ? Math.max(10, typeSeq * 15) : typeSeq * 10;
            case ShipDesign.LARGE:
                baseCost = typeSeq == 1 ? 60 : typeSeq * 50;
                if (reinforced)
                    baseCost = (int) Math.max(50, baseCost * 1.5);
                return baseCost;
            case ShipDesign.HUGE:
                baseCost = typeSeq == 1 ? 300 : typeSeq * 250;
                if (reinforced)
                    baseCost = (int) Math.max(250, baseCost * 1.5);
                return baseCost;
        }
        return 1;
    }
    public float baseSize(int s, boolean reinforced) {
        switch (s) {
            case ShipDesign.SMALL:
                if (reinforced)
                    return (typeSeq <= 4) ? 14 + (typeSeq * 3) : 5 + (typeSeq * 5);
                else
                    return typeSeq * 2;
            case ShipDesign.MEDIUM:
                if (reinforced) {
                    switch (typeSeq) {
                        case 0: return 80;
                        case 1: return 85;
                        case 2: return 100;
                        case 3: return 115;
                        case 4: return 130;
                        case 5: return 150;
                        case 6: return 175;
                        default: return 25 + (25*typeSeq);
                    }
                }
                else
                    return typeSeq * 10;
            case ShipDesign.LARGE:
                if (reinforced) {
                    switch (typeSeq) {
                        case 0: return 400;
                        case 1: return 425;
                        case 2: return 500;
                        case 3: return 575;
                        case 4: return 650;
                        case 5: return 750;
                        case 6: return 875;
                        default: return 125 + (125*typeSeq);
                    }
                }
                else
                    return typeSeq == 1 ? 60 : typeSeq * 50;
            case ShipDesign.HUGE:
                if (reinforced) {
                    switch (typeSeq) {
                        case 0: return 2000;
                        case 1: return 2100;
                        case 2: return 2500;
                        case 3: return 2875;
                        case 4: return 3250;
                        case 5: return 3750;
                        case 6: return 4375;
                        default: return 625 + (625*typeSeq);
                    }
                }
                else
                    return typeSeq == 1 ? 300 : typeSeq * 250;
        }
        return 1;
    }
}
