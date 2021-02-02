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

public class SpyCapturedAlert extends GameAlert {
    private final Empire spy;
    private final Empire target;
    private final String mission;
     public static void create(Empire spyE, Empire targetE, String t) {
        if (!spyE.atWarWith(targetE.id))
            GameSession.instance().addAlert(new SpyCapturedAlert(spyE, targetE,t));
    }
    @Override
    public String description() {
        if (spy.isAI())
            return text("MAIN_ALERT_SPY_CAPTURED", spy.name(), mission);
        else
            return text("MAIN_ALERT_SPY_CONFESSED", target.name(), mission);
    }
    private SpyCapturedAlert(Empire spyE, Empire targetE, String t) {
        spy = spyE;
        target = targetE;
        mission = t;
    }
}
