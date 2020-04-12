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

import java.util.Collections;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.util.Base;

public class GNNRankingNoticeCheck implements Base {
    public static GNNRankingNoticeCheck instance = new GNNRankingNoticeCheck();
    private static final int RANK_POPULATION = 1;
    private static final int RANK_PRODUCTION = 2;
    private static final int RANK_TECHNOLOGY = 3;
    private static final int RANK_FLEETS = 4;

    public static void nextTurn() {
        instance.trigger();
    }
    public void trigger() {
        boolean showRanking = (galaxy().activeEmpires().size() > 1)
                        && !galaxy().council().inactive()
                        && ((galaxy().currentYear() % 25) == 0);
        if (showRanking)
            showRanking();
    }
    public void showRanking() {
        int rankingToShow = roll(1,4);
        String title = null;
        List<Empire> empires = galaxy().activeEmpires();
        switch (rankingToShow) {
            case RANK_POPULATION:
                title = text("GNN_CENSUS_RANKING");
                Collections.sort(empires, Empire.TOTAL_POPULATION);
                break;
            case RANK_PRODUCTION:
                title = text("GNN_PRODUCTION_RANKING");
                Collections.sort(empires, Empire.TOTAL_PRODUCTION);
                break;
            case RANK_TECHNOLOGY:
                title = text("GNN_TECHNOLOGY_RANKING");
                Collections.sort(empires, Empire.AVG_TECH_LEVEL);
                break;
            case RANK_FLEETS:
                title = text("GNN_FLEETS_RANKING");
                Collections.sort(empires, Empire.TOTAL_FLEET_SIZE);
                break;
        }
        GNNNotification.notifyRanking(title, empires);
    }
}
