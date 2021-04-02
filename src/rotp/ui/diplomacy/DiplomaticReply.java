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

import rotp.util.Base;

public class DiplomaticReply implements Base {
    public static DiplomaticReply answer(boolean b, String s) {
        DiplomaticReply reply = new DiplomaticReply();
        reply.accepted(b);
        reply.remark(s);
        return reply;
    }
    boolean accepted = false;
    String remark;
    String returnMenu;
    boolean resumeTurn = false;
    boolean returnToMap = false;

    public boolean accepted()         { return accepted; }
    public void accepted(boolean b)   { accepted = b; }
    public void resumeTurn(boolean b) { resumeTurn = b; }
    public String remark()            { return remark; }
    public String text() {
        return remark;
    }
    public void remark(String s)      { remark = s; }
    public String returnMenu()        { return returnMenu; }
    public void returnMenu(String s)  { returnMenu = s; }
    public void returnToMap(boolean b) { returnToMap = b; }

    public void decode(String key, String value) {
        remark = remark.replace(key, value);
    }	
}
