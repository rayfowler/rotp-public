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

        String empTitle1;
        if (knowsVictim) {
            empTitle1 = text("GNN_EXTINCTION_KNOWN_VICTIM");
            empTitle1 = victim.replaceTokens(empTitle1, "victim");
        }
        else
            empTitle1 = text("GNN_EXTINCTION_UNKNOWN_VICTIM");
        
        if (killer == null)
            GNNNotification.notifyGenocide(text("GNN_EXTINCTION_NO_KILLER", empTitle1));
        else {
            String empTitle2;
            if (knowsKiller) {
                empTitle2 = text("GNN_EXTINCTION_KNOWN_KILLER");
                if (killer instanceof Empire) 
                    empTitle2 = ((Empire)killer).replaceTokens(empTitle2, "killer");     
                else
                    empTitle2 = empTitle2.replace("[killer_race]", killer.name());
            }
            else
                empTitle2 = text("GNN_EXTINCTION_UNKNOWN_KILLER");
            String title = text("GNN_EXTINCTION", empTitle1, empTitle2);
            GNNNotification.notifyGenocide(title);
        }
    }
}
