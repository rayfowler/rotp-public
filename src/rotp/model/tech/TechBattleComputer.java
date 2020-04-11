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

import rotp.model.colony.MissileBaseComputer;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;

public final class TechBattleComputer extends Tech {
    public int mark;
    public MissileBaseComputer baseComputer;

    public TechBattleComputer (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.BATTLE_COMPUTER;
        baseComputer = new MissileBaseComputer(this);

        switch(typeSeq) {
                case 0:
                mark = 0;
                baseComputer.baseCost(0);
                break;
            case 1:
                mark = 1;
                baseComputer.baseCost(61);
                break;
            case 2:
                mark = 2;
                baseComputer.baseCost(73);
                break;
            case 3:
                mark = 3;
                baseComputer.baseCost(86);
                break;
            case 4:
                mark = 4;
                baseComputer.baseCost(99);
                break;
            case 5:
                mark = 5;
                baseComputer.baseCost(111);
                break;
            case 6:
                mark = 6;
                baseComputer.baseCost(124);
                break;
            case 7:
                mark = 7;
                baseComputer.baseCost(136);
                break;
            case 8:
                mark = 8;
                baseComputer.baseCost(149);
                break;
            case 9:
                mark = 9;
                baseComputer.baseCost(162);
                break;
            case 10:
                mark = 10;
                baseComputer.baseCost(174);
                break;
            case 11:
                mark = 11;
                baseComputer.baseCost(187);
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topBattleComputerTech() != null) && (level < c.tech().topBattleComputerTech().level);
    }
    @Override
    public float baseValue(Empire c)   { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c)) {
            c.tech().topBattleComputerTech(this);
            c.tech().updateMissileBase();
        }
        ShipComputer sh = new ShipComputer(this);
        c.shipLab().addComputer(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL:  return 3 + mark;
            case ShipDesign.MEDIUM: return 16 + (4*mark);
            case ShipDesign.LARGE:  return 80 + (20*mark);
            case ShipDesign.HUGE:   return 400 + (100*mark);
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL:  return 1 + (2*mark);
            case ShipDesign.MEDIUM: return 5*mark;
            case ShipDesign.LARGE:  return 10 + (10*mark);
            case ShipDesign.HUGE:   return 50 + (50*mark);
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL:  return 1 + (2*mark);
            case ShipDesign.MEDIUM: return 5*mark;
            case ShipDesign.LARGE:  return 10 + (10*mark);
            case ShipDesign.HUGE:   return 50 + (50*mark);
        }
        return 0;
    }
}
