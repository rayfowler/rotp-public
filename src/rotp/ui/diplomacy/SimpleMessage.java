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
package rotp.ui.diplomacy;

import rotp.ui.RotPUI;

public class SimpleMessage extends DiplomaticMessage {
    String nextView;
    public SimpleMessage(String s) {
        messageType = s;
        nextView = null;
    }
    public SimpleMessage(String s, String next) {
        messageType = s;
        nextView = next;
    }
    @Override
    public void escape()                         { select(0); }
    @Override
    public void select(int i) {
        if (nextView == null)
            RotPUI.instance().selectRacesPanel();
        else
            DiplomaticMessage.show(view(), nextView);
    }
}