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
package rotp.model.empires;

import rotp.util.Base;

public class TreatyPeace extends DiplomaticTreaty implements Base {
    private static final long serialVersionUID = 1L;
    private int duration;
    public TreatyPeace(Empire e1, Empire e2, int d) {
        super(e1,e2,"RACES_PEACE");
        duration = d;
        recallAttackingForces(e1, e2);
    }    
    @Override
    public void nextTurn(Empire emp)      { 
        duration--;
        if (duration <= 0) {
            galaxy().empire(empire2).viewForEmpire(empire1).embassy().setNoTreaty();
            galaxy().empire(empire1).viewForEmpire(empire2).embassy().setNoTreaty();
        }
    }
    @Override
    public boolean isPeace()                 { return true; }
    @Override
    public int listOrder()                      { return 5; }
    private void recallAttackingForces(Empire e1, Empire e2) {
        e1.retreatShipsFrom(e2.id);
        e2.retreatShipsFrom(e1.id);
    }
}
