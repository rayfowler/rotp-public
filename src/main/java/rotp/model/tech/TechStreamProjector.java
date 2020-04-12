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
import rotp.model.ships.ShipSpecialProjector;

public final class TechStreamProjector extends Tech {
    public int range = 1;
    public float enemyArmorMod = 1;
    public float enemyArmorModMax = 1;
    public float extraArmorMod = 0;
    public float shipsPerExtraArmorMod = 1;

    public TechStreamProjector(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.STREAM_PROJECTOR;

        switch(typeSeq) {
            case 0:
                range = 2;
                cost = 100;
                size = 250;
                power = 500;
                enemyArmorMod = .80f;
                enemyArmorModMax = .50f;
                extraArmorMod = .01f;
                shipsPerExtraArmorMod = 2;
                break;
            case 1:
                range = 2;
                cost = 200;
                size = 500;
                power = 1250;
                enemyArmorMod = .60f;
                enemyArmorModMax = .25f;
                extraArmorMod = .01f;
                shipsPerExtraArmorMod = 1;
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    public float armorMod(int ships) {
        float mod = enemyArmorMod-(extraArmorMod*ships/shipsPerExtraArmorMod);
        return max(mod,enemyArmorModMax);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipSpecialProjector sh = new ShipSpecialProjector(this);
        c.shipLab().addSpecial(sh);
    }
}
