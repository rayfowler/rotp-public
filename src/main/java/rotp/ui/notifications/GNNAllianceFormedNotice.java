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
import rotp.util.Base;

public class GNNAllianceFormedNotice implements Base {
    public static void create(Empire emp1, Empire emp2) {
        new GNNAllianceFormedNotice(emp1, emp2);
    }
    private GNNAllianceFormedNotice(Empire emp1, Empire emp2) {
        Empire pl = player();
        if ((emp1 == pl) || (emp2 == pl))
            return;
        if (!pl.hasContact(emp1) && !pl.hasContact(emp2))
            return;

        String empTitle1 = emp1.name();
        String empTitle2 = emp2.name();
        if (!pl.hasContact(emp1)) {
            empTitle1 = empTitle2;
            empTitle2 = emp1.name();
        }
        String title = text("GNN_ALLIANCE_FORMED", empTitle1, empTitle2);
        GNNNotification.notifyAllianceFormed(title);
    }
}
