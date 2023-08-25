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
import rotp.util.Base;

public class GameStatus implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    enum Status { NO_GAME, IN_PROGRESS, LOSS_MILITARY, LOSS_DIPLOMATIC, 
        WIN_DIPLOMATIC, WIN_MILITARY, WIN_MILITARY_ALLIANCE, WIN_COUNCIL_ALLIANCE, LOSS_NO_COLONIES; }
    private Status status = Status.NO_GAME;

    public boolean inProgress()       { return status == Status.IN_PROGRESS; }
    public boolean lost() {
    	return lostMilitary() || lostDiplomatic() || lostNoColonies(); 
    }
    public boolean won() {
    	return wonCouncilAlliance() || wonMilitary() || wonMilitaryAlliance() || wonDiplomatic();
    }
    public boolean lostMilitary()         { return status == Status.LOSS_MILITARY; }
    public boolean lostDiplomatic()       { return status == Status.LOSS_DIPLOMATIC; }
    public boolean lostNoColonies()       { return status == Status.LOSS_NO_COLONIES; }
    public boolean wonDiplomatic()        { return status == Status.WIN_DIPLOMATIC; }
    public boolean wonMilitary()          { return status == Status.WIN_MILITARY; }
    public boolean wonMilitaryAlliance()  { return status == Status.WIN_MILITARY_ALLIANCE; }
    public boolean wonCouncilAlliance()   { return status == Status.WIN_COUNCIL_ALLIANCE; }

    public void startGame()               { status = Status.IN_PROGRESS; }
    public void loseMilitary()            { status = Status.LOSS_MILITARY; }
    public void loseDiplomatic()          { status = Status.LOSS_DIPLOMATIC; }
    public void loseNoColonies()          { status = Status.LOSS_NO_COLONIES; }
    public void winDiplomatic()           { status = Status.WIN_DIPLOMATIC; }
    public void winMilitary()             { status = Status.WIN_MILITARY; }
    public void winMilitaryAlliance()     { status = Status.WIN_MILITARY_ALLIANCE; }
    public void winCouncilAlliance()      { status = Status.WIN_COUNCIL_ALLIANCE; }
    
}
