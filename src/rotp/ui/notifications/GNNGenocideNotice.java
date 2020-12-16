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
import rotp.model.galaxy.NamedObject;
import rotp.model.galaxy.SpaceMonster;
import rotp.util.Base;

public class GNNGenocideNotice implements Base {
    public static void create(Empire victim, NamedObject killer) {
        new GNNGenocideNotice(victim, killer);
    }
    private GNNGenocideNotice(Empire victim, NamedObject killer) {
        Empire pl = player();
        boolean knowsVictim = (victim == pl) || pl.hasContacted(victim.id);
        boolean knowsKiller = false;
        
        if (killer instanceof Empire) {
            Empire attacker = (Empire) killer;
            knowsKiller = (attacker == pl) || pl.knowsOf(attacker) || pl.knowsOf(victim);
        }
        else if (killer instanceof SpaceMonster) 
            knowsKiller = pl.knowsOf(victim);

        // if player doesn't know either race, then don't notify
        if (!knowsVictim && !knowsKiller)
            return;

        String empTitle1 = knowsVictim ? text("GNN_KNOWN_SUBJECT", victim.name()) : text("GNN_UNKNOWN_SUBJECT");
        if (killer == null)
            GNNNotification.notifyGenocide(text("GNN_EXTINCTION_UNKNOWN", empTitle1));
        else {
            String empTitle2 = knowsKiller ? text("GNN_KNOWN_PREDICATE", killer.name()) : text("GNN_UNKNOWN_PREDICATE");
            String title = text("GNN_EXTINCTION", empTitle1, empTitle2);
            GNNNotification.notifyGenocide(title);
        }
    }
}
