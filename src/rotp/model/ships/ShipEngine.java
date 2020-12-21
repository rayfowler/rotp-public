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

import rotp.model.empires.Empire;
import rotp.model.tech.TechEngineWarp;

public final class ShipEngine extends ShipComponent {
    private static final long serialVersionUID = 1L;
    public ShipEngine() {
        sequence(0);
    }
    public ShipEngine(TechEngineWarp t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public TechEngineWarp tech()      { return (TechEngineWarp) super.tech(); }
    public float powerOutput()       { return tech().powerOutput(); }
    @Override
	// modnar: add in cost miniaturization for Ship Engine
    public float cost(ShipDesign d)  {
		Empire emp = d == null ? null : d.empire();
        return tech().costMiniaturization(emp) * tech().baseCost();
	}
    @Override
    public float space(ShipDesign d) { return d.enginesRequired()* size(d); }
    public int warp()                 { return tech().warp();  }
    public int baseWarp()             { return tech().baseWarp();  }
    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
            case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
            case 1: return str((int)cost(d));
            case 2:
                ShipEngine prev2 = d.engine();
                d.engine(this);
                float sz = size(d);
                d.engine(prev2);
                return str((int) sz);
            case 3:
                ShipEngine prev3 = d.engine();
                d.engine(this);
                float num = d.enginesRequired();
                d.engine(prev3);
                return fmt(num, 2);
            case 4:
                ShipEngine prev4 = d.engine();
                d.engine(this);
                float sp = space(d);
                d.engine(prev4);
                return str(Math.round(sp));
        }
        return "";
    }
}
