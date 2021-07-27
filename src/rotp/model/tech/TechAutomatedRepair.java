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
import rotp.model.ships.ShipSpecialRepair;

public final class TechAutomatedRepair extends Tech {
    public float repairAdj;

    public TechAutomatedRepair (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.AUTOMATED_REPAIR;

        switch(typeSeq) {
            case 0:
                repairAdj = .15f;
                break;
            case 1: default:
                repairAdj = .3f;
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public float baseValue(Empire c)   { return c.ai().scientist().baseValue(this); }
    @Override
    public boolean isObsolete(Empire c) {
        return repairAdj < c.tech().shipDamageRepairPct();
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (! isObsolete(c))
            c.tech().topAutomatedRepairTech(this);

        ShipSpecialRepair sh = new ShipSpecialRepair(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch (typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 0.2f;
                    case ShipDesign.MEDIUM: return 0.8f;
                    case ShipDesign.LARGE: return 5;
                    case ShipDesign.HUGE: return 30;
                }
            case 1: default:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 4;
                    case ShipDesign.MEDIUM: return 20;
                    case ShipDesign.LARGE: return 100;
                    case ShipDesign.HUGE: return 500;
                }
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch (typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 3;
                    case ShipDesign.MEDIUM: return 15;
                    case ShipDesign.LARGE: return 100;
                    case ShipDesign.HUGE: return 600;
                }
            case 1: default:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 9;
                    case ShipDesign.MEDIUM: return 45;
                    case ShipDesign.LARGE: return 300;
                    case ShipDesign.HUGE: return 1800;
                }
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch (typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 3;
                    case ShipDesign.MEDIUM: return 10;
                    case ShipDesign.LARGE: return 50;
                    case ShipDesign.HUGE: return 300;
                }
            case 1: default:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 9;
                    case ShipDesign.MEDIUM: return 30;
                    case ShipDesign.LARGE: return 150;
                    case ShipDesign.HUGE: return 450;
                }
        }
        return 0;
    }
}
