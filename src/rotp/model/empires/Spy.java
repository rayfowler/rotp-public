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
package rotp.model.empires;

import java.io.Serializable;
import rotp.util.Base;

public class Spy implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    final static int IDENTITY_CAN_FRAME = 0;
    final static int IDENTITY_UNDISCOVERED = 1;
    final static int IDENTITY_WILL_BE_CAUGHT = 2;
    final static int IDENTITY_CAUGHT = 3;
    final static int IDENTITY_CONFESSES = 4;

    final static int MISSION_UNATTEMPTED = 0;
    final static int MISSION_FAILED = 1;
    final static int MISSION_SUCCEEDS = 2;

    final static int FATE_CLEARED = 0;
    final static int FATE_STOPPED = 1;

    private int fate;
    private int mission;
    private int identity;
    public Spy(SpyNetwork sn) { }
    public Spy makeSuper()  {
        fate = FATE_CLEARED;
        identity = IDENTITY_CAN_FRAME;
        mission = MISSION_SUCCEEDS;
        return this;
    }
    public void attemptInfiltration(float securityAdj) {
        // determine spy's fate.. p271 of Strategy Guide, Table 12-1
        float r = random() + securityAdj;
        mission = MISSION_UNATTEMPTED;
        if (r < 0) {
            fate = FATE_CLEARED;
            identity = IDENTITY_CAN_FRAME;
        }
        else if (r <= .3) {
            fate = FATE_CLEARED;
            identity = IDENTITY_UNDISCOVERED;
        }
        else if (r <= .5) {
            fate = FATE_CLEARED;
            identity = IDENTITY_WILL_BE_CAUGHT;
        }
        else if (r <= .7)  {
            fate = FATE_STOPPED;
            identity = IDENTITY_UNDISCOVERED;
        }
        else if (r <= 1) {
            fate = FATE_STOPPED;
            identity = IDENTITY_CAUGHT;
        }
        else {
            fate = FATE_STOPPED;
            identity = IDENTITY_CONFESSES;
        }
    }
    public void attemptMission(float adj) {
        float r = random() + adj;
        mission = r <= 0.85 ? MISSION_FAILED : MISSION_SUCCEEDS;

        if (r  > 1)
            identity = IDENTITY_CAN_FRAME;
        else if (identity == IDENTITY_WILL_BE_CAUGHT)
            identity = IDENTITY_CAUGHT;
    }
    public boolean clearedToInfiltrate() { return fate == FATE_CLEARED; }
    public boolean stopped()             { return fate == FATE_STOPPED; }
    public boolean eliminated()          { return (identity == IDENTITY_CAUGHT) || (identity == IDENTITY_CONFESSES); }
    public boolean caught()              { return identity == IDENTITY_CAUGHT; }
    public boolean confesses()           { return identity == IDENTITY_CONFESSES; }
    public boolean missionSucceeds()     { return mission == MISSION_SUCCEEDS; }
    public boolean canFrame()            { return identity == IDENTITY_CAN_FRAME; }
}
