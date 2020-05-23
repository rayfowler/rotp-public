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
package rotp.ui.notifications;

import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.game.GameSession;
import rotp.ui.RotPUI;
import rotp.util.Base;

public class BombardSystemNotification implements TurnNotification, Base {
    private final ShipFleet fleet;
    private final int sysId;

    public static void create(int systemId, ShipFleet fl) {
        Empire pl = GameSession.instance().player();
        int sysId = systemId;
        // sanity check to avoid unallowable bombings
        // fleet must have ability to attack planets and must be in orbit
        // planet must be colonized
        // fleet & planet empires must be on aggressive terms (war or no treaty)
        if (!fl.canAttackPlanets() || fl.launched() || !pl.sv.isColonized(sysId) || !fl.empire().aggressiveWith(pl.sv.empId(sysId)))
            return;

        // bomb immediately instead of queueing notification
        // this is at end-of-turn anyway so no point in waiting
        //GameSession.instance().addTurnNotification(new BombardSystemNotification(sv, fl));

        // if player is bombing, show bombard panel
        // if player getting bombed, show other bombard panel
        // else just bomb and not show anyone (ai vs ai)

        Empire emp1 = fl.empire();
        emp1.sv.refreshFullScan(sysId);
        Empire emp2 = emp1.sv.empire(sysId);

        if (emp1.isPlayerControlled())
            RotPUI.instance().promptForBombardment(sysId, fl);
        else if ((emp2 != null) && emp2.isPlayerControlled())
            RotPUI.instance().showBombardmentNotice(sysId, fl);
        else
            fl.bombard();

        // refresh the SystemViews for the involved empires
        emp1.sv.refreshFullScan(sysId);
        emp2.sv.refreshFullScan(sysId);
    }
    private BombardSystemNotification(int systemId, ShipFleet fl) {
        fleet = fl;
        sysId = systemId;
    }
    @Override
    public String displayOrder() { return PROMPT_BOMBARD; }
    @Override
    public void notifyPlayer() {
        Empire pl = player();
        // last minute check to ensure system is still colonized and that the
        // fleet is still aggressive with the planet's empire
        if (fleet.canAttackPlanets() && fleet.inOrbit() && pl.sv.isColonized(sysId) && fleet.empire().aggressiveWith(pl.sv.empId(sysId)))
            RotPUI.instance().promptForBombardment(sysId, fleet);
    }
}
