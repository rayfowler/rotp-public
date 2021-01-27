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

import rotp.model.game.GameSession;

public class SpyNewTechAlert extends GameAlert {
    private final int empId;
    private final String techId;
     public static void create(int emp, String t) {
        GameSession.instance().addAlert(new SpyNewTechAlert(emp,t));
    }
    @Override
    public String description() {
        String empName = galaxy().empire(empId).name();
        String techName = tech(techId).name();
        return text("MAIN_ALERT_SPY_NEW_TECH", empName, techName);
    }
    private SpyNewTechAlert(int emp, String t) {
        empId = emp;
        techId = t;
    }
}
