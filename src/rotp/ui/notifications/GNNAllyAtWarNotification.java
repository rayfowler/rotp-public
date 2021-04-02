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

public class GNNAllyAtWarNotification implements Base {
    public static void create(Empire ally, Empire enemy) {
        new GNNAllyAtWarNotification(ally, enemy);
    }
    private GNNAllyAtWarNotification(Empire ally, Empire enemy) {
        String title = text("GNN_ALLY_AT_WAR");
        title = ally.replaceTokens(title, "ally");
        title = enemy.replaceTokens(title, "other");
        GNNNotification.notifyGenocide(title);
    }
}
