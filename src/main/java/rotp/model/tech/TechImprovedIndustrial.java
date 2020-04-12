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

public final class TechImprovedIndustrial extends Tech {
    public static final int BASE_FACTORY_COST = 10;
    public float factoryCost;

    public TechImprovedIndustrial (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.IMPROVED_INDUSTRIAL;

        switch(typeSeq) {
            case 0: factoryCost = 9; break;
            case 1: factoryCost = 8; break;
            case 2: factoryCost = 7; break;
            case 3: factoryCost = 6; break;
            case 4: factoryCost = 5; break;
            case 5: factoryCost = 4; break;
            case 6: factoryCost = 3; break;
            case 7: factoryCost = 2; break;
        }
    }
    @Override
    public boolean isObsolete(Empire c) { return factoryCost > c.tech().baseFactoryCost(); }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topImprovedIndustrialTech(this);
    }
}
