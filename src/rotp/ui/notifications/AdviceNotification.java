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
import rotp.model.game.GameSession;
import rotp.ui.RotPUI;

public class AdviceNotification implements TurnNotification {
    private final String message;
    private Empire emp1;
    private String var1, var2, var3;
    public static void create(String message) {
        GameSession.instance().addTurnNotification(new AdviceNotification(message));
    }
    public static void create(String message, Empire e1, String s1) {
        GameSession.instance().addTurnNotification(new AdviceNotification(message, e1, s1));
    }
    public static void create(String message, String s1) {
        GameSession.instance().addTurnNotification(new AdviceNotification(message, s1));
    }
    public static void create(String message, String s1, String s2) {
        GameSession.instance().addTurnNotification(new AdviceNotification(message, s1, s2));
    }
    public static void create(String message, String s1, String s2, String s3) {
        GameSession.instance().addTurnNotification(new AdviceNotification(message, s1, s2, s3));
    }
    private AdviceNotification(String msg) {
        message = msg;
    }
    private AdviceNotification(String msg, Empire e1, String s1) {
        message = msg;
        emp1 = e1;
        var1 = s1;
    }
    private AdviceNotification(String msg, String s1) {
        message = msg;
        var1 = s1;
    }
    private AdviceNotification(String msg, String s1, String s2) {
        message = msg;
        var1 = s1;
        var2 = s2;
    }
    private AdviceNotification(String msg, String s1, String s2, String s3) {
        message = msg;
        var1 = s1;
        var2 = s2;
        var3 = s3;
    }
    @Override
    public String displayOrder() { return ADVICE; }
    @Override
    public void notifyPlayer()   { RotPUI.instance().showAdvice(message, emp1, var1, var2, var3); }
}
