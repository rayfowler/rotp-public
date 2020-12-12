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

import rotp.model.combat.CombatStack;
import rotp.model.tech.TechEnergyPulsar;

public final class ShipSpecialPulsar extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialPulsar(TechEnergyPulsar t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public String designGroup()              { return "Pulsar"; }
    @Override
    public boolean isWeapon()      { return true; }
    @Override
    public TechEnergyPulsar tech() { return (TechEnergyPulsar) super.tech(); }
    @Override
    public int range()             { return tech().range; }
    @Override
    public float estimatedKills(CombatStack source, CombatStack target, int num) {
        float maxDam = tech().firstShipDamage + (tech().extraShipDamage * (num-1));
        float dam = maxDam/2;
        return target.num * Math.min(1,(dam/target.maxHits));
    }
    @Override
    public void fireUpon(CombatStack source, CombatStack target, int count) {
		 // modnar: correct Pulsar damage with shipsPerExtraDamage
        float maxDam = tech().firstShipDamage + (tech().extraShipDamage * (source.num-1) / tech().shipsPerExtraDamage);
        float dam = roll(1,(int)maxDam);

        tech().drawSpecialAttack(source, target, count, dam);

        for (int x0=source.x-1; x0<=source.x+1; x0++) {
            for (int y0=source.y-1; y0<=source.y+1; y0++) {
                CombatStack st = source.mgr.stackAt(x0,y0);
                if ((st != null) && st.isShip() && (st != source)) 
                    st.takePulsarDamage(dam, 1);
            }
        }
    }
}
