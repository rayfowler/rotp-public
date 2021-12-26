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
package rotp.ui.design;

import java.util.List;
import javax.swing.JLabel;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipWeapon;

public class DesignWeaponSelectionUI extends DesignSelectionUI {
    private static final long serialVersionUID = 1L;
    private int bank = 0;
    public void bank(int i)           { bank = i; }
    @Override
    String title()                    { return text("SHIP_DESIGN_WEAPON_TITLE"); }
    @Override
    int numColumns()                  { return 8; }
    @Override
    int bank()                        { return bank; }
    @Override
    String header(int i) {
        switch(i) {
            case 0: return text("SHIP_DESIGN_WEAPON_TYPE");
            case 1: return text("SHIP_DESIGN_WEAPON_MAX");
            case 2: return text("SHIP_DESIGN_WEAPON_DAMAGE");
            case 3: return text("SHIP_DESIGN_WEAPON_COST");
            case 4: return text("SHIP_DESIGN_WEAPON_SIZE");
            case 5: return text("SHIP_DESIGN_WEAPON_POWER");
            case 6: return text("SHIP_DESIGN_WEAPON_SPACE");
            case 7: return text("SHIP_DESIGN_WEAPON_DESCRIPTION");
        }
        return "";
    }
    @Override
    int alignment(int i) {
        switch(i) {
            case 1:
            case 3:
            case 4:
            case 5:
            case 6:
                return JLabel.RIGHT;
            case 2:
                return JLabel.CENTER;
        }
        return super.alignment(i);
    }
    @Override
    int minimumWidth(int i) {
        switch(i) {
            case 0: return 250;
            case 2: return 100;
            case 7: return 300;
        }
        return super.minimumWidth(i);
    }
    @Override
    List<? extends ShipComponent> baseComponents() { return player().shipLab().weapons(); }
    @Override
    ShipComponent selectedComponent()              { return selectedDesign.weapon(bank); }
    @Override
    void select(int compNum)   { 
        ShipComponent newComp = (ShipWeapon)components().get(compNum);
        int minCount = newComp.isNone() ? 0 : 1;
        String valStr = value(compNum, 1, bank);
        int val = max(minCount,Integer.valueOf(valStr));
        selectedDesign.weapon(bank, (ShipWeapon)newComp); 
        selectedDesign.wpnCount(bank, val);
    }
}

