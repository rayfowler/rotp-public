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
package rotp.model.ships;

import rotp.model.tech.TechECMJammer;

public final class ShipECM extends ShipComponent { 
    private static final long serialVersionUID = 1L;
    public ShipECM() {
        sequence(0);
    }
    public ShipECM(TechECMJammer t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public TechECMJammer tech() { return (TechECMJammer) super.tech(); }

    public int level()          { return tech() == null ? 0 : tech().mark; }
    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
            case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
            case 1: return str((int)(cost(d)+(enginesRequired(d)*d.engine().cost(d))));
            case 2: return str((int)size(d));
            case 3: return str((int)power(d));
            case 4: return str((int)space(d));
        }
        return "";
    }
}
