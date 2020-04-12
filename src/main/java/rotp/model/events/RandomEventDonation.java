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
package rotp.model.events;

import rotp.model.empires.Empire;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;

public class RandomEventDonation implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int donationAmount = 0;
    private int empId;
    @Override
    public boolean goodEvent()    		{ return true; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_DONATION");
        s1 = s1.replace("[amt]", str(donationAmount));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        int turnNum = galaxy().currentTurn();
        empId = emp.id;
        donationAmount = turnNum * 10;
        emp.addToTreasury(donationAmount);

        if (player().knowsOf(emp))
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Donation");
    }
}
