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

import rotp.model.colony.MissileBaseMissileShield;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipSpecialMissileShield;

public final class TechMissileShield extends Tech {
    public float baseBlockPct;
    public float baseBlockAdjPerLevel;
    public MissileBaseMissileShield baseMissileShield;

    public TechMissileShield (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.MISSILE_SHIELD;
        baseMissileShield = new MissileBaseMissileShield(this);
        baseBlockAdjPerLevel = .01f;
        switch(typeSeq) {
            case 0: baseBlockPct = .40f; break;
            case 1: baseBlockPct = .75f; break;
            case 2: baseBlockPct = 1.00f; break;
        }
    }
    @Override
    public float warModeFactor()        { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topMissileShieldTech() != null) && (level < c.tech().topMissileShieldTech().level);
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topMissileShieldTech(this);

        ShipSpecialMissileShield sh = new ShipSpecialMissileShield(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseCost(ShipDesign d) {
        switch (typeSeq) {
            case 0:  return 10;
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 5;
                    case ShipDesign.MEDIUM: return 10;
                    case ShipDesign.LARGE: return 20;
                    case ShipDesign.HUGE: return 30;
                }
            case 2:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 20;
                    case ShipDesign.MEDIUM: return 30;
                    case ShipDesign.LARGE: return 40;
                    case ShipDesign.HUGE: return 50;
                }
        }
        return 0;
    }
    @Override
    public float baseSize(ShipDesign d) {
        switch (typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 2;
                    case ShipDesign.MEDIUM: return 10;
                    case ShipDesign.LARGE: return 50;
                    case ShipDesign.HUGE: return 250;
                }
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 4;
                    case ShipDesign.MEDIUM: return 20;
                    case ShipDesign.LARGE: return 100;
                    case ShipDesign.HUGE: return 500;
                }
            case 2:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 6;
                    case ShipDesign.MEDIUM: return 30;
                    case ShipDesign.LARGE: return 150;
                    case ShipDesign.HUGE: return 750;
                }
        }
        return 0;
    }
    @Override
    public float basePower(ShipDesign d) {
        switch (typeSeq) {
            case 0:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 8;
                    case ShipDesign.MEDIUM: return 40;
                    case ShipDesign.LARGE: return 200;
                    case ShipDesign.HUGE: return 1000;
                }
            case 1:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 12;
                    case ShipDesign.MEDIUM: return 60;
                    case ShipDesign.LARGE: return 300;
                    case ShipDesign.HUGE: return 1500;
                }
            case 2:
                switch(d.size()) {
                    case ShipDesign.SMALL: return 15;
                    case ShipDesign.MEDIUM: return 70;
                    case ShipDesign.LARGE: return 350;
                    case ShipDesign.HUGE: return 1750;
                }
        }
        return 0;
    }
}
