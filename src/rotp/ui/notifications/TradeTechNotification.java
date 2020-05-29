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

import java.io.Serializable;
import rotp.ui.RotPUI;

public class TradeTechNotification implements TurnNotification, Serializable {
    private static final long serialVersionUID = 1L;
    public String techId;
    public int empId;

    public static TradeTechNotification create(String t, int e) {
        return new TradeTechNotification(t, e);
    }
    private TradeTechNotification(String t, int e) {
        techId = t;
        empId = e;
    }
    @Override
    public String displayOrder() { return DISCOVER_TECH; }
    @Override
    public void notifyPlayer() {
        RotPUI.instance().selectTradeTechPanel(techId, empId);
    }
}
