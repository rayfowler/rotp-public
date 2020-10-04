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
import rotp.model.tech.TechArmor;

public final class ShipArmor extends ShipComponent {
    private static final long serialVersionUID = 1L;
    private boolean reinforced = false;

    public boolean reinforced()   { return reinforced; }
    public ShipArmor() {
        sequence(0);
    }
    public ShipArmor(TechArmor t, boolean r) {
        tech(t);
        reinforced = r;
        sequence(t.level);
        if (reinforced)
            sequence(sequence() + .5f);
    }
    @Override
    public TechArmor tech() { return (TechArmor) super.tech(); }
    @Override
    public String name() { return reinforced ? tech().item() + " II" : tech().item(); }
    public float hits(ShipDesign design) {
        float hits = design.baseHits() * tech().hitsAdj;
        if (reinforced)
            hits *= 1.5f;
        return (float) Math.ceil(hits);
    }
    @Override
    public String desc(ShipDesign d) { return text(desc(), (int) hits(d)); }
    @Override
	// modnar: add in cost miniaturization for Armor
    public float cost(ShipDesign d) {
		Empire emp = d == null ? null : d.empire();
        return tech().costMiniaturization(emp) * tech().baseCost(d.size(), reinforced);
	}
    @Override
    public float size(ShipDesign d) {
        Empire emp = d == null ? null : d.empire();
        int size = d == null? ShipDesign.SMALL : d.size();
        return tech().sizeMiniaturization(emp) * tech().baseSize(size, reinforced);
    }
    @Override
    public String fieldValue(int n, ShipDesign d, int bank) {
        switch(n) {
            case 0: return name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : name();
            case 1: return str((int)(cost(d)+(enginesRequired(d)*d.engine().cost(d))));
            case 2: return str((int)size(d));
        }
        return "";
    }
}
