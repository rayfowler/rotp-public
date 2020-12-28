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
package rotp.model.game;

import java.io.Serializable;
import rotp.model.empires.Race;

public class NewPlayer implements Serializable {
    private static final long serialVersionUID = 1L;
    public String race;
    public String leaderName;
    public String homeWorldName;
    public int color = 0;

    public NewPlayer() {
        Race def = Race.races().get(0);
        race = def.id;
        leaderName = def.randomLeaderName();
        homeWorldName = def.defaultHomeworldName();
        color = 0;	
    }
    public void copy(NewPlayer p) {
        race = p.race;
        leaderName = p.leaderName;
        homeWorldName = p.homeWorldName;
        color = p.color;
    }
}
