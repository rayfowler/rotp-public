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
import rotp.model.ships.ShipSpecialFuelRange;

public final class TechReserveFuelRange extends Tech {
    private int range;
    public boolean unlimited = false;

    public float range() { return range*options().fuelRangeMultiplier(); }

    public TechReserveFuelRange (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public void init() {
        super.init();
        techType = Tech.RESERVE_FUEL_RANGE;

        switch(typeSeq) {
            case 0: range = 3; break;
        }
    }
    @Override
    public String detail() {
        float rng = range();
        if (rng == (int) rng) 
            return text(detail, (int) rng);
        else
            return text(detail, df1.format(range())); 
    }
    @Override
    public String brief() { 
        float rng = range();
        if (rng == (int) rng) 
            return text(shDesc, (int) rng);
        else
            return text(shDesc, df1.format(range())); 
    }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topReserveFuelRangeTech() != null)
            && (range < c.tech().topReserveFuelRangeTech().range);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topReserveFuelRangeTech(this);
        ShipSpecialFuelRange sh = new ShipSpecialFuelRange(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 2;
                    case ShipDesign.MEDIUM: return 10;
                    case ShipDesign.LARGE: return 50;
                    case ShipDesign.HUGE: return 250;
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
                    case ShipDesign.SMALL: return 20;
                    case ShipDesign.MEDIUM: return 100;
                    case ShipDesign.LARGE: return 500;
                    case ShipDesign.HUGE: return 2500;
                }
                break;
        }
        return 0;
    }
}
