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

import java.util.List;
import rotp.model.empires.Empire;

public class DiplomaticCounterReply extends DiplomaticReply {
    List<String> techs;
    int bribe;
    int targetEmpId;
    public static DiplomaticCounterReply answer(boolean b, String s,  int empId, List<String> techList, float bribeAmt) {
        DiplomaticCounterReply reply = new DiplomaticCounterReply();
        reply.accepted(b);
        reply.remark(s);
        reply.targetEmpId = empId;
        reply.techs = techList;
        reply.bribe = bribeAmt < 100 ? 0: ((int) bribeAmt/100)*100;
        return reply;
    }    
    public Empire target() {
        return galaxy().empire(targetEmpId);
    }
    public List<String> techs()  { return techs; }
    public int bribeAmt()        { return bribe; }
}
