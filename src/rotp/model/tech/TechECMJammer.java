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

import rotp.model.colony.MissileBaseECM;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipECM;

public final class TechECMJammer extends Tech {
    public int mark;
    public MissileBaseECM baseECM;

    public TechECMJammer (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.ECM_JAMMER;
        baseECM = new MissileBaseECM(this);

        switch(typeSeq) {
            case 0:
                mark = 0;
                baseECM.baseCost(0);
                break;
            case 1:
                mark = 1;
                baseECM.baseCost(62);
                break;
            case 2:
                mark = 2;
                baseECM.baseCost(69);
                break;
            case 3:
                mark = 3;
                baseECM.baseCost(76);
                break;
            case 4:
                mark = 4;
                baseECM.baseCost(84);
                break;
            case 5:
                mark = 5;
                baseECM.baseCost(91);
                break;
            case 6:
                mark = 6;
                baseECM.baseCost(98);
                break;
            case 7:
                mark = 7;
                baseECM.baseCost(105);
                break;
            case 8:
                mark = 8;
                baseECM.baseCost(112);
                break;
            case 9:
                mark = 9;
                baseECM.baseCost(120);
                break;
            case 10:
                mark = 10;
                baseECM.baseCost(127);
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topECMJammerTech() != null) && (level < c.tech().topECMJammerTech().level);
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c)) {
            c.tech().topECMJammerTech(this);
            c.tech().updateMissileBase();
        }
        ShipECM sh = new ShipECM(this);
        c.shipLab().addECM(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return (2.25f + (.25f*mark));
            case ShipDesign.MEDIUM : return (13.5f + (1.5f*mark));
            case ShipDesign.LARGE  : return (90 + (10*mark));
            case ShipDesign.HUGE   : return (562.5f + (62.5f*mark));
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return (5 + (5*mark));
            case ShipDesign.MEDIUM : return (10 + (10*mark));
            case ShipDesign.LARGE  : return (20 + (20*mark));
            case ShipDesign.HUGE   : return (90 + (80*mark));
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch(d.size()) {
            case ShipDesign.SMALL  : return (5 + (5*mark));
            case ShipDesign.MEDIUM : return (10 + (10*mark));
            case ShipDesign.LARGE  : return (20 + (20*mark));
            case ShipDesign.HUGE   : return (90 + (80*mark));
        }
        return 0;
    }
}
