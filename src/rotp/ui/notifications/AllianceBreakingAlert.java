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

public class AllianceBreakingAlert  extends GameAlert {
    private final Empire empire;
    public static void create(Empire e) {
        GameSession.instance().addAlert(new AllianceBreakingAlert(e));
    }
    @Override
    public String description() {
        String desc = text("MAIN_ALERT_ALLIANCE_BREAKING");
        desc = empire.replaceTokens(desc, "ally");
        return desc;
    }
    private AllianceBreakingAlert(Empire e) {
        empire = e;
    }
}
