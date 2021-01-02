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

public final class TechIndustrialWaste extends Tech {
    public static final int BASE_FACTORY_WASTE_MOD = 1;
    public float wasteModifier;

    public TechIndustrialWaste(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.INDUSTRIAL_WASTE;

        switch(typeSeq) {
            case 0: wasteModifier = .80f; break;
            case 1: wasteModifier = .60f; break;
            case 2: wasteModifier = .40f; break;
            case 3: wasteModifier = .20f; break;
            case 4: wasteModifier = 0;   break;
        }
    }
    @Override
    public boolean isObsolete(Empire c) {
        return wasteModifier > c.tech().factoryWasteMod();
    }
    @Override
    public boolean reducesEcoSpending()    { return true; }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topIndustrialWasteTech(this);
    }
}
