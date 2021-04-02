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
import rotp.model.empires.EmpireView;
import rotp.model.game.GameSession;
import rotp.model.incidents.DiplomaticIncident;
import rotp.ui.RotPUI;
import rotp.util.Base;

public class DiplomaticNotification implements TurnNotification, Base {
    private EmpireView view;
    private Empire talker;
    private Empire other;
    private String type;
    private DiplomaticIncident incident;
    private boolean returnToMap = false;

    public static DiplomaticNotification create(EmpireView v, String messageType) {
        DiplomaticNotification notif = new DiplomaticNotification(v, messageType);
        GameSession.instance().addTurnNotification(notif);
        return notif;
    }
    public static DiplomaticNotification create(EmpireView v, String messageType, Empire otherEmp) {
        DiplomaticNotification notif = new DiplomaticNotification(v, messageType);
        notif.other = otherEmp;
        GameSession.instance().addTurnNotification(notif);
        return notif;
    }
    public static void createAndNotify(EmpireView v, String messageType) {
        new DiplomaticNotification(v, messageType).notifyPlayer();
    }
    public static DiplomaticNotification create(EmpireView v, DiplomaticIncident inc, String messageType) {
        DiplomaticNotification notif = new DiplomaticNotification(v, inc, messageType);
        GameSession.instance().addTurnNotification(notif);
        return notif;
    }
    public static DiplomaticNotification create(EmpireView v, DiplomaticIncident inc) {
        DiplomaticNotification notif = new DiplomaticNotification(v, inc);
        GameSession.instance().addTurnNotification(notif);
        return notif;
    }
    public static DiplomaticNotification create(Empire talk, DiplomaticIncident inc) {
        DiplomaticNotification notif = new DiplomaticNotification(talk, inc);
        GameSession.instance().addTurnNotification(notif);
        return notif;
    }
    public DiplomaticNotification() { }

    public DiplomaticNotification(EmpireView v, String messageType) {
        view = v;
        talker = v.owner();
        type = messageType;
    }
    protected DiplomaticNotification(EmpireView v, DiplomaticIncident inc, String messageType) {
        view = v;
        talker = v.owner();
        type = messageType;
        incident = inc;
    }
    protected DiplomaticNotification(EmpireView v, DiplomaticIncident inc) {
        view = v;
        talker = v.owner();
        type = inc.warningMessageId();
        incident = inc;
    }
    protected DiplomaticNotification(Empire talk, DiplomaticIncident inc) {
        view = null;
        talker = talk;
        type = inc.warningMessageId();
        incident = inc;
    }
    public Empire talker()               { return talker; }
    public Empire otherEmpire()          { return other; }
    public DiplomaticIncident incident() { return incident; }
    public String type()                 { return type; }
    public EmpireView view()             { return view; }
    public void setReturnToMap()         { returnToMap = true; }
    public boolean returnToMap()         { return returnToMap; }
    public void view(EmpireView v)       { view = v; talker = v.owner(); }
    @Override
    public String displayOrder() { return incident == null ? DIPLOMATIC_MESSAGE : incident.displayOrder(); }
    @Override
    public void notifyPlayer()   {
        RotPUI.instance().selectDiplomaticMessagePanel(this);
    }
}
