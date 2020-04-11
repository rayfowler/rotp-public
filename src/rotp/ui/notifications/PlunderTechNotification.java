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

import rotp.model.game.GameSession;
import rotp.ui.RotPUI;

public class PlunderTechNotification implements TurnNotification {
    String techId;
    int sysId;
    int empId;

    public static void create(String t, int s, int e) {
        GameSession.instance().addTurnNotification(new PlunderTechNotification(t,s,e));
    }
    public PlunderTechNotification(String t, int s, int e) {
        techId = t;
        sysId = s;
        empId = e;
    }
    @Override
    public String displayOrder() { return PLUNDER_TECH; }

    @Override
    public void notifyPlayer() {
        RotPUI.instance().selectPlunderTechPanel(techId, sysId, empId);
    }
}
