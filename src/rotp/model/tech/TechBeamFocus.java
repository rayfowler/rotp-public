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
import rotp.model.ships.ShipSpecialBeamFocus;

public final class TechBeamFocus extends Tech {
    public float shieldAdj = 1;
    public int rangeAdj = 0;

    public TechBeamFocus (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.BEAM_FOCUS;
        switch(typeSeq) {
            case 0:
                rangeAdj = 3;
                break;
            case 1:
                shieldAdj = 0.5f;
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipSpecialBeamFocus sh = new ShipSpecialBeamFocus(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 3;
                    case ShipDesign.MEDIUM: return 13.5f;
                    case ShipDesign.LARGE: return 62.5f;
                    case ShipDesign.HUGE: return 350;
                }
                break;
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 3;
                    case ShipDesign.MEDIUM: return 15;
                    case ShipDesign.LARGE: return 60;
                    case ShipDesign.HUGE: return 275;
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
                    case ShipDesign.SMALL: return 35;
                    case ShipDesign.MEDIUM: return 100;
                    case ShipDesign.LARGE: return 150;
                    case ShipDesign.HUGE: return 500;
                }
                break;
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 8;
                    case ShipDesign.MEDIUM: return 40;
                    case ShipDesign.LARGE: return 200;
                    case ShipDesign.HUGE: return 1000;
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
                    case ShipDesign.SMALL: return 65;
                    case ShipDesign.MEDIUM: return 200;
                    case ShipDesign.LARGE: return 350;
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
