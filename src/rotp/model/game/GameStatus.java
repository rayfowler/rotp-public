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
        WIN_NEW_REPUBLIC, WIN_REBELLION, WIN_REBELLION_ALLIANCE, WIN_COUNCIL_ALLIANCE; }
    private Status status = Status.NO_GAME;

    public boolean inProgress()       { return status == Status.IN_PROGRESS; }
    public boolean lost() {
    	return lostOverthrown() || lostMilitary() || lostDiplomatic()
    		|| lostNewRepublic() || lostRebellion(); 
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
    public void winDiplomatic()           { status = Status.WIN_DIPLOMATIC; }
    public void winMilitary()             { status = Status.WIN_MILITARY; }
    public void winMilitaryAlliance()     { status = Status.WIN_MILITARY_ALLIANCE; }
    public void winNewRepublic()          { status = Status.WIN_NEW_REPUBLIC; }
    public void winRebellion()            { status = Status.WIN_REBELLION; }
    public void winRebellionAlliance()    { status = Status.WIN_REBELLION_ALLIANCE; }
    public void winCouncilAlliance()      { status = Status.WIN_COUNCIL_ALLIANCE; }
    
    private float sizeVal() {
    	switch(options().selectedGalaxySize()) {
    	case IGameOptions.SIZE_TINY:       return 100;
    	case IGameOptions.SIZE_SMALL:      return 200;
    	case IGameOptions.SIZE_SMALL2:     return 250;
    	case IGameOptions.SIZE_MEDIUM:     return 300;
    	case IGameOptions.SIZE_MEDIUM2:    return 350;
    	case IGameOptions.SIZE_LARGE:      return 400;
    	case IGameOptions.SIZE_LARGE2:     return 450;
    	case IGameOptions.SIZE_HUGE:       return 500;
    	case IGameOptions.SIZE_HUGE2:      return 550;
    	case IGameOptions.SIZE_MASSIVE:    return 600;
    	case IGameOptions.SIZE_MASSIVE2:   return 620;
    	case IGameOptions.SIZE_MASSIVE3:   return 640;
    	case IGameOptions.SIZE_MASSIVE4:   return 660;
    	case IGameOptions.SIZE_MASSIVE5:   return 680;
    	case IGameOptions.SIZE_INSANE:     return 700;
    	case IGameOptions.SIZE_LUDICROUS:  return 1000;
    	}
    	return 0;
    }
    private float opponentsVal() {
    	return sqrt(options().selectedNumberOpponents());
    }
    private float difficultyVal() {
    	return 1.0f;
    }
    private float endConditionVal() {
    	switch(status) {
    	case LOSS_OVERTHROWN:        return 1;
    	case LOSS_NEW_REPUBLIC:      return 1;
    	case LOSS_MILITARY:          return 3;
    	case LOSS_DIPLOMATIC:        return 3;
    	case LOSS_REBELLION:         return 3;
    	case WIN_COUNCIL_ALLIANCE:   return 7;
    	case WIN_DIPLOMATIC:         return 10;
    	case WIN_NEW_REPUBLIC:       return 12;
    	case WIN_MILITARY:           return 15;
    	case WIN_MILITARY_ALLIANCE:  return 15;
    	case WIN_REBELLION:          return 20;
    	case WIN_REBELLION_ALLIANCE: return 20;
    	default:                     return 0;
    	}
    }
    private int numTurns() {
    	return galaxy().numberTurns();
    }
}
