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
import rotp.model.ships.ShipSpecialTeleporter;

public final class TechTeleporter extends Tech {
    public TechTeleporter(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.TELEPORTER;
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);

        ShipSpecialTeleporter sh = new ShipSpecialTeleporter(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 2.5f;
                    case ShipDesign.MEDIUM: return 10;
                    case ShipDesign.LARGE: return 45;
                    case ShipDesign.HUGE: return 225;
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
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 16;
                    case ShipDesign.MEDIUM: return 80;
                    case ShipDesign.LARGE: return 400;
                    case ShipDesign.HUGE: return 2000;
                }
                break;
        }
        return 0;
    }
}
