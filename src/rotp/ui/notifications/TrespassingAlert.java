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
import rotp.model.game.GameSession;

public class TrespassingAlert extends GameAlert {
    private final int empHostId;
    private final int empShipId;
    private final int sysId;
    public static void create(int emp1, int emp2, int sys) {
        GameSession.instance().addAlert(new TrespassingAlert(emp1, emp2, sys));
    }
    @Override
    public String description() {
        Empire empHost = galaxy().empire(empHostId);
        Empire empShip = galaxy().empire(empShipId);
        String sysName = player().sv.name(sysId);
        String desc;
        if (empHost.isPlayer()) {
           desc = text("MAIN_ALERT_TRESPASSING1", sysName);
           desc = empShip.replaceTokens(desc, "alien");
        }
        else {
           desc = text("MAIN_ALERT_TRESPASSING2", sysName);   
           desc = empHost.replaceTokens(desc, "alien");
        }
        return desc;
    }
    private TrespassingAlert(int emp1, int emp2, int sys) {
        empHostId = emp1;
        empShipId = emp2;
        sysId = sys;
    }
}
