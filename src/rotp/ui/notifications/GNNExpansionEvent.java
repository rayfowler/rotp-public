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
import rotp.model.galaxy.Galaxy;
import rotp.util.Base;

public class GNNExpansionEvent implements Base {
    private static GNNExpansionEvent instance;
    private int triggers = 0;

    public static GNNExpansionEvent instance() {
        if (instance == null)
            instance = new GNNExpansionEvent();
        return instance;
    }
    public static void nextTurn() {
        instance().trigger();
    }
    public void validate(Galaxy gal) {
        // deal with fact that trigger isn't stored in save-game file
        // so when reloading we need to figure out what it should be
        // so we don't redo previous GNN notifications
        int numSystems = gal.numStarSystems();
        triggers = 0;
        int trigger1 = 6;
        int trigger2 = numSystems/3;
        int trigger3 = numSystems/2;
        for (Empire emp : gal.empires()) {
            int cols = emp.allColonizedSystems().size();
            if (cols >= trigger3)
                triggers = max(triggers,3);
            else if (cols >= trigger2)
                triggers = max(triggers,2);
            else if (cols >= trigger1)
                triggers = max(triggers,1);
        }
    }
    public void trigger() {
        if (galaxy().numActiveEmpires() < 2)
            return;
        if (triggers >= numTriggers())
            return;

        int numColonies = thresholdCount();

        String title = null;
        for (Empire emp : galaxy().empires()) {
            if (emp.allColonizedSystems().size() >= numColonies) {
                switch (triggers) {
                    case 0:
                        title = text("GNN_EXPANSION_1", numColonies);
                        break;
                    case 1:
                        title = text("GNN_EXPANSION_2", numColonies);
                        break;
                    case 2:
                        title = text("GNN_EXPANSION_3");
                        break;
                }
                title = emp.replaceTokens(title,"expanding");
                triggers++;
                if (player().knowsOf(emp))
                    GNNNotification.notifyExpansion(title);
                break;
            }
        }
    }
    private int numTriggers() {
        return galaxy().numStarSystems() < 30 ? 1 : 3;
    }
    private int thresholdCount() {
        if (triggers == 0)
            return 6;

        int numSystems = galaxy().numStarSystems();
        if (triggers == 1)
            return numSystems/3;

        return numSystems / 2;
    }
}
