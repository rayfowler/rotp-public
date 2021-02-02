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

public final class TechFuelRange extends Tech {
    private int range;
    public boolean unlimited = false;

    public float range()  { return range*options().fuelRangeMultiplier(); }

    public TechFuelRange(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.FUEL_RANGE;

        switch(typeSeq) {
            case 0: range = 3;   break;
            case 1: range = 4;   break;
            case 2: range = 5;   break;
            case 3: range = 6;   break;
            case 4: range = 7;   break;
            case 5: range = 8;   break;
            case 6: range = 9;   break;
            case 7: range = 10;  break;
            case 8: range = 99999; 
                    unlimited = true;
                    break;
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
    public boolean isFuelRangeTech()     { return true; }
    @Override
    public float expansionModeFactor()  { return 3; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return range() < c.tech().shipRange();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (! isObsolete(c)) 
            c.tech().topFuelRangeTech(this);
    }
}
