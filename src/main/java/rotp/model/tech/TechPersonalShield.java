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

public final class TechPersonalShield extends Tech {
    public int groundAttackBonus = -1;

    public TechPersonalShield (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.PERSONAL_SHIELD;

        switch(typeSeq) {
            case 0: groundAttackBonus = 0; break;
            case 1: groundAttackBonus = 10; break;
            case 2:	groundAttackBonus = 20; break;
            case 3:	groundAttackBonus = 30; break;
        }
    }
    @Override
    public float warModeFactor()        { return 3; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topPersonalShieldTech() != null) && (groundAttackBonus < c.tech().shieldGroundBonus());
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topPersonalShieldTech(this);
    }
}
