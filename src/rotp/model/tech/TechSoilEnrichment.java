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

public final class TechSoilEnrichment extends Tech {
    public float growthMod;
    public int planetaryIncrease;
    public int environment;
    public TechSoilEnrichment(String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public boolean promptToReallocate()     { return super.promptToReallocate() && !player().ignoresPlanetEnvironment(); }
    @Override
    public Colony.Orders followup()         { return Colony.Orders.SOIL; }
    @Override
    public void init() {
        super.init();
        techType = Tech.SOIL_ENRICHMENT;

        switch(typeSeq) {
            case 0:
                growthMod = 1.5f;
                planetaryIncrease = 25;
                cost = 150;
                environment = 2;
                break;
            case 1:
                growthMod = 2;
                planetaryIncrease = 50;
                cost = 300;
                environment = 3;
                break;
        }
    }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topSoilEnrichmentTech() != null) && (level  < c.tech().topSoilEnrichmentTech().level);
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if ((c.tech().topSoilEnrichmentTech() == null) || ((c.tech().topSoilEnrichmentTech()).typeSeq < typeSeq))
            c.tech().topSoilEnrichmentTech(this);
    }
}
