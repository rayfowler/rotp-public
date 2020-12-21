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

import rotp.model.tech.TechEngineWarp;

public final class ShipManeuver extends ShipComponent {
    private static final long serialVersionUID = 1L;
    public ShipManeuver() {
        sequence(0);
    }
    public ShipManeuver(TechEngineWarp t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public TechEngineWarp tech()      { return (TechEngineWarp) super.tech(); }
    @Override
    public String name()              { return tech().item2(); }
    public int level()                { return tech().baseWarp(); }
    @Override
    public float power(ShipDesign d) { return tech().baseManeuverPower(d.size(), d.engine().baseWarp()); }
    @Override
    public String desc()             { return tech().brief2(); }
    @Override
    public String desc(ShipDesign d) { return text(desc(), (int) d.combatSpeed()); }
    @Override
    public float size(ShipDesign d)  { return 0; }
    @Override
    public float cost(ShipDesign d)  { return (enginesRequired(d) * d.engine().cost(d)); }
    public int combatSpeed()          { return tech() == null ? 0 : (int) ((tech().baseWarp() + 2) / 2); }
    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
                    case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
                    case 1: return str(combatSpeed());
                    case 2: return str((int)(cost(d)+(enginesRequired(d)*d.engine().cost(d))));
                    case 3: return str((int)power(d));
                    case 4: return str((int)space(d));
        }
        return "";
    }
}
