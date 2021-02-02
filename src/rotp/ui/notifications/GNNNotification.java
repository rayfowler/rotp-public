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

import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.game.GameSession;
import rotp.ui.RotPUI;

public class GNNNotification implements TurnNotification {
    private final String message;
    private final String eventId;

    public static void notifyAllianceFormed(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Alliance_Formed"));
    }
    public static void notifyAllianceBroken(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Alliance_Broken"));
    }
    public static void notifyCouncil(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Expansion"));
    }
    public static void notifyExpansion(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Expansion"));
    }
    public static void notifyRebellion(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Rebellion"));
    }
    public static void notifyGenocide(String message) {
        GameSession.instance().addTurnNotification(new GNNNotification(message, "GNN_Genocide"));
    }
    public static void notifyImmediateEvent(String message, String id) {
        RotPUI.instance().processNotification(new GNNRandomEventNotification(message, id));
    }
    public static void notifyRandomEvent(String message, String id) {
        GameSession.instance().addTurnNotification(new GNNRandomEventNotification(message, id));
    }
    public static void notifyRanking(String message, List<Empire> empireList) {
        GameSession.instance().addTurnNotification(new GNNRankingNotification(message, empireList, "GNN_Ranking"));
    }
    private GNNNotification(String msg, String id) {
        message = msg;
        eventId = id;
    }
    @Override
    public String displayOrder() { return GNN_NOTIFY; }
    @Override
    public void notifyPlayer() {
        RotPUI.instance().selectGNNPanel(message, eventId, null);
    }
}
