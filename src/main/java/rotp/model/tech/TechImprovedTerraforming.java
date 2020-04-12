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

public final class TechImprovedTerraforming extends Tech {
    public static int BASE_COST = 5;
    private int increase;
    public int costPerMillion;

    public int increase()   { return (int) (increase * session().populationBonus()); }
    public TechImprovedTerraforming(String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public Colony.Orders followup()			       { return Colony.Orders.TERRAFORM; }
    @Override
    public void init() {
        super.init();
        techType = Tech.IMPROVED_TERRAFORMING;

        switch(typeSeq) {
            case 0:
                increase = 10;
                costPerMillion = 5;
                break;
            case 1:
                increase = 20;
                costPerMillion = 5;
                break;
            case 2:
                increase = 30;
                costPerMillion = 4;
                break;
            case 3:
                increase = 40;
                costPerMillion = 4;
                break;
            case 4:
                increase = 50;
                costPerMillion = 3;
                break;
            case 5:
                increase = 60;
                costPerMillion = 3;
                break;
            case 6:
                increase = 80;
                costPerMillion = 2;
                break;
            case 7:
                increase = 100;
                costPerMillion = 2;
                break;
            case 8:
                increase = 120;
                costPerMillion = 2;
                break;
        }
    }
    @Override
    public boolean isObsolete(Empire c) {
        return increase < c.tech().terraformAdj();
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topTerraformingTech(this);
    }
}
