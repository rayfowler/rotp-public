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
import rotp.model.ships.ShipSpecialScanner;

public final class TechScanner extends Tech {
    public static int BATTLE_SCANNER_INITIATIVE = 3;
    public int type;
    public float planetRange = 0;
    public float shipRange = 0;
    public int shipAttackBonus = 0;
    public int shipInitiativeBonus = 0;
    public boolean knowETA = false;
    public boolean scanPlanets = false;
    public boolean special = false;

    public TechScanner (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.SCANNER;

        switch(typeSeq) {
            case 0:
                special=true;
                cost = 30;
                size = 50;
                power = 50;
                planetRange = 0;
                shipRange = 0;
                shipAttackBonus = 1;
                shipInitiativeBonus = BATTLE_SCANNER_INITIATIVE;
                break;
            case 1:
                planetRange = 5;
                shipRange = 1;
                break;
            case 2:
                planetRange = 7;
                shipRange = 2;
                knowETA = true;
                break;
            case 3:
                planetRange = 9;
                shipRange = 3;
                knowETA = true;
                scanPlanets = true;
                break;
        }
    }
    @Override
    public boolean providesShipComponent()  { return special; }
    @Override
    public boolean isObsolete(Empire c) {
        return (shipRange < c.shipScanningRange()) && (planetRange < c.planetScanningRange());
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        c.shipScanningRange(max(c.shipScanningRange(), shipRange));
        c.planetScanningRange(max(c.planetScanningRange(), planetRange));
        c.knowShipETA(knowETA);
        c.scanPlanets(scanPlanets);
        if (special) {
            ShipSpecialScanner sh = new ShipSpecialScanner(this);
            c.shipLab().addSpecial(sh);
        }
    }
}
