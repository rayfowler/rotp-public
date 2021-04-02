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
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;

public class InvadersKilledAlert  extends GameAlert {
    private final Empire empire;
    private final StarSystem system;
    private final int num;
    public static void create(Empire e, StarSystem s, int n) {
        GameSession.instance().addAlert(new InvadersKilledAlert(e,s,n));
    }
    @Override
    public String description() {
        String desc = text("MAIN_ALERT_INVADERS_KILLED", systemName(), str(num));
        desc = empire.replaceTokens(desc, "alien");
        return desc;
    }
    private String systemName() { return player().sv.name(system.id); }
    private InvadersKilledAlert(Empire e, StarSystem s, int n) {
        empire = e;
        system = s;
        num = n;
    }
}
