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
    enum Status { NO_GAME, IN_PROGRESS, LOSS_OVERTHROWN, LOSS_MILITARY, LOSS_DIPLOMATIC, 
        LOSS_NEW_REPUBLIC, LOSS_REBELLION, WIN_DIPLOMATIC, WIN_MILITARY, WIN_MILITARY_ALLIANCE, 
        WIN_NEW_REPUBLIC, WIN_REBELLION, WIN_REBELLION_ALLIANCE, WIN_COUNCIL_ALLIANCE, LOSS_NO_COLONIES; }
    private Status status = Status.NO_GAME;

    public boolean inProgress()       { return status == Status.IN_PROGRESS; }
    public boolean lost() {
    	return lostOverthrown() || lostMilitary() || lostDiplomatic()
    		|| lostNewRepublic() || lostRebellion() || lostNoColonies(); 
    }
    public boolean won() {
    	return wonCouncilAlliance() || wonMilitary() || wonMilitaryAlliance() 
            || wonNewRepublic() || wonRebellion() || wonRebellionAlliance() || wonDiplomatic();
    }
    public boolean lostOverthrown()       { return status == Status.LOSS_OVERTHROWN; }
    public boolean lostMilitary()         { return status == Status.LOSS_MILITARY; }
    public boolean lostDiplomatic()       { return status == Status.LOSS_DIPLOMATIC; }
    public boolean lostNewRepublic()      { return status == Status.LOSS_NEW_REPUBLIC; }
    public boolean lostRebellion()        { return status == Status.LOSS_REBELLION; }
    public boolean lostNoColonies()       { return status == Status.LOSS_NO_COLONIES; }
    public boolean wonDiplomatic()        { return status == Status.WIN_DIPLOMATIC; }
    public boolean wonMilitary()          { return status == Status.WIN_MILITARY; }
    public boolean wonMilitaryAlliance()  { return status == Status.WIN_MILITARY_ALLIANCE; }
    public boolean wonNewRepublic()       { return status == Status.WIN_NEW_REPUBLIC; }
    public boolean wonRebellion()         { return status == Status.WIN_REBELLION; }
    public boolean wonRebellionAlliance() { return status == Status.WIN_REBELLION_ALLIANCE; }
    public boolean wonCouncilAlliance()   { return status == Status.WIN_COUNCIL_ALLIANCE; }

    public void startGame()               { status = Status.IN_PROGRESS; }
    public void loseOverthrown()          { status = Status.LOSS_OVERTHROWN; }
    public void loseMilitary()            { status = Status.LOSS_MILITARY; }
    public void loseDiplomatic()          { status = Status.LOSS_DIPLOMATIC; }
    public void loseNewRepublic()         { status = Status.LOSS_NEW_REPUBLIC; }
    public void loseRebellion()           { status = Status.LOSS_REBELLION; }
    public void loseNoColonies()          { status = Status.LOSS_NO_COLONIES; }
    public void winDiplomatic()           { status = Status.WIN_DIPLOMATIC; }
    public void winMilitary()             { status = Status.WIN_MILITARY; }
    public void winMilitaryAlliance()     { status = Status.WIN_MILITARY_ALLIANCE; }
    public void winNewRepublic()          { status = Status.WIN_NEW_REPUBLIC; }
    public void winRebellion()            { status = Status.WIN_REBELLION; }
    public void winRebellionAlliance()    { status = Status.WIN_REBELLION_ALLIANCE; }
    public void winCouncilAlliance()      { status = Status.WIN_COUNCIL_ALLIANCE; }
    
}
