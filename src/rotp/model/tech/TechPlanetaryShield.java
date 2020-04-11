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

import rotp.model.colony.Colony;
import rotp.model.empires.Empire;

public final class TechPlanetaryShield extends Tech {
    public int damage;

    public TechPlanetaryShield (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public Colony.Orders followup()                   { return Colony.Orders.SHIELD; }
    @Override
    public void init() {
        super.init();
        techType = Tech.PLANETARY_SHIELD;

        switch(typeSeq) {
            case 0: damage = 5;  break;
            case 1: damage = 10; break;
            case 2: damage = 15; break;
            case 3: damage = 20; break;
        }
    }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean isPlanetaryShieldTech()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return damage < c.tech().maxPlanetaryShieldLevel();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topPlanetaryShieldTech(this);
    }
}
