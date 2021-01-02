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
import rotp.model.empires.Race;

public final class TechEcoRestoration extends Tech {
    public static final int BASE_WASTE_ELIMINATED = 2;

    public int type;
    public int wasteEliminated;

    public TechEcoRestoration (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public boolean canBeResearched(Race r)  {
        return !r.ignoresPlanetEnvironment();  // silicoids don't research these techs
    }
    @Override
    public void init() {
        super.init();
        techType = Tech.ECO_RESTORATION;

        switch(typeSeq) {
            case 0: wasteEliminated = 2;  break;
            case 1: wasteEliminated = 3;  break;
            case 2: wasteEliminated = 5;  break;
            case 3: wasteEliminated = 10; break;
            case 4: wasteEliminated = 20; break;
        }
    }
    @Override
    public boolean reducesEcoSpending()    { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return wasteEliminated < c.tech().wasteElimination();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topEcoRestorationTech(this);
    }
}
