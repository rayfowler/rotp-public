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
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComponent;

public class DesignArmorSelectionUI extends DesignSelectionUI {
    private static final long serialVersionUID = 1L;
    @Override
    String title()                    { return text("SHIP_DESIGN_ARMOR_TITLE"); }
    @Override
    int numColumns()                  { return 3; }
    @Override
    String header(int i) {
        switch(i) {
            case 0: return text("SHIP_DESIGN_ARMOR_TYPE");
            case 1: return text("SHIP_DESIGN_ARMOR_COST");
            case 2: return text("SHIP_DESIGN_ARMOR_SIZE");
        }
        return "";
    }
    @Override
    int alignment(int i) {
        switch(i) {
            case 1:
            case 2:
                return JLabel.RIGHT;
        }
        return super.alignment(i);
    }
    @Override
    int minimumWidth(int i) {
        switch(i) {
            case 0: return 175;
        }
        return super.minimumWidth(i);
    }
    @Override
    List<? extends ShipComponent> baseComponents() { return player().shipLab().armors(); }
    @Override
    ShipComponent selectedComponent()              { return selectedDesign.armor(); }
    @Override
    void select(int compNum)   { selectedDesign.armor((ShipArmor)components().get(compNum)); }
}