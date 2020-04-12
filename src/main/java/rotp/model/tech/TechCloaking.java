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
import rotp.model.ships.ShipSpecialCloaking;

public final class TechCloaking extends Tech {
    public static String baseFlavor = "Cloaking Devices hide ships from enemy detection. Ships must uncloak to attack, and must wait one complete turn before recloaking.";

    public static float TRANSPARENCY = .2f;
    public float range = 0;
    public float move = 0;

    public TechCloaking (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.CLOAKING;

        switch(typeSeq) {
            case 0:
                range = 1;
                move = 1;
                break;
        }
    }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipSpecialCloaking sh = new ShipSpecialCloaking(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 3;
                    case ShipDesign.MEDIUM: return 15;
                    case ShipDesign.LARGE: return 75;
                    case ShipDesign.HUGE: return 375;
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
                    case ShipDesign.SMALL: return 5;
                    case ShipDesign.MEDIUM: return 25;
                    case ShipDesign.LARGE: return 120;
                    case ShipDesign.HUGE: return 600;
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
                    case ShipDesign.SMALL: return 10;
                    case ShipDesign.MEDIUM: return 50;
                    case ShipDesign.LARGE: return 250;
                    case ShipDesign.HUGE: return 1250;
                }
                break;
        }
        return 0;
    }
}
