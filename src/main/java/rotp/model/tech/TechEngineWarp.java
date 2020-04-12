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
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;

public final class TechEngineWarp extends Tech {
    public static int MAX_SPEED = 9;
    private int warp;
    public String shName;

    public TechEngineWarp(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.ENGINE_WARP;

        switch(typeSeq) {
            case 0: warp = 1; break;
            case 1: warp = 2; break;
            case 2: warp = 3; break;
            case 3: warp = 4; break;
            case 4: warp = 5; break;
            case 5: warp = 6; break;
            case 6: warp = 7; break;
            case 7: warp = 8; break;
            case 8: warp = 9; break;
        }
    }
    public int warp()                    { return (int) (session().propulsionBonus() * warp); }
    @Override
    public float warModeFactor()        { return 1.5f; }
    @Override
    public float expansionModeFactor()  { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
            return warp < c.tech().topSpeed();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topEngineWarpTech(this);

        ShipEngine sh = new ShipEngine(this);
        c.shipLab().addEngine(sh);

        // when engine tech is learned, ship maneuvers for all
        // lower engine techs become available. Add them if the
        // design lab does nothave them yet.
        for (Tech t: c.tech().allTechsOfType(techType)) {
            TechEngineWarp tech = (TechEngineWarp) t;
            if (tech.level <= level) {
                if (!c.shipLab().hasManeuverForTech(tech)) {
                    ShipManeuver sh2 = new ShipManeuver(tech);
                    c.shipLab().addManeuver(sh2);
                }
            }
        }

        if (c.isPlayer() && (warp() > 1))
            galaxy().giveAdvice("MAIN_ADVISOR_SHIP_ENGINE");
    }
    @Override
    public float baseCost() {
        return warp * 2;
    }
    public float powerOutput() { return warp * 10; }
    @Override
    public float baseSize(ShipDesign d) {
        switch(warp) {
            case 1: return 10;
            case 2: return 18;
            case 3: return 26;
            case 4: return 33;
            case 5: return 36;
            case 6: return 40;
            case 7: return 44;
            case 8: return 47;
            case 9: return 50;
        }
        return (23 + (warp * 3));
    }
    public float baseManeuverSize(int size, int engineWarp) {
        switch (size) {
            case ShipDesign.SMALL:  return 2;
            case ShipDesign.MEDIUM: return 15;
            case ShipDesign.LARGE:  return 100;
            case ShipDesign.HUGE:   return 700;
        }
        return 0;
    }
    public float baseManeuverPower(int size, int engineWarp) {
        switch (size) {
            case ShipDesign.SMALL:  return 2 * warp / engineWarp;
            case ShipDesign.MEDIUM: return 15 * warp / engineWarp;
            case ShipDesign.LARGE:  return 100 * warp / engineWarp;
            case ShipDesign.HUGE:   return 700 * warp / engineWarp;
        }
        return 0;
    }
}
