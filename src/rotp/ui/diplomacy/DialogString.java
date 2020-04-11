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
import rotp.model.empires.EmpireView;
import rotp.util.Base;

public class DialogString implements Base {
    String messageType;
    int relationsCode;
    int powerCode;
    String key;
    public String key()               { return key; }
    public DialogString (String line) {
        decodeFileInput(line);
    }
    public boolean matchesType(String t)      { return messageType.equals(t); }
    public boolean fitsContext(EmpireView view) {
        if (view == null)
            return true;
        return matchesRelations(view.embassy().relations())
            && matchesRelativePower(view.empirePower());
    }
    private void decodeFileInput(String line) {
        List<String> parms = substrings(line,'|');
        messageType = parms.get(0);
        int filterCode = parseInt(parms.get(1)); // filter code is 2-digit number: [1-7][1-7]
        relationsCode = filterCode / 10;  // first dight is for relations
        powerCode = filterCode % 10;      // second dight is for relative power
        key = parms.get(2);
    }
    private boolean matchesRelations(float d) {
        int cThreshold = -30;
        int aThreshold = 30;
        int[] cCodes = { 1, 3, 5, 7 };  // 1,3,5,7 = 1-bit is set
        int[] bCodes = { 2, 3, 6, 7 };  // 2,3,6,7 = 2-bit is set             
        int[] aCodes = { 4, 5, 6, 7 };  // 4,5,6,7 = 4-bit is set

        int[] testCodes = d < cThreshold ? cCodes: (d > aThreshold ? aCodes: bCodes);

        for (int i=0;i<4;i++) {
            if (testCodes[i] == relationsCode)
                return true;
        }
        return false;
    }
    private boolean matchesRelativePower(float d) {
        float cThreshold = 0.5f;
        float aThreshold = 2.0f;
        int[] cCodes = { 1, 3, 5, 7 };  // 1,3,5,7 = 1-bit is set
        int[] bCodes = { 2, 3, 6, 7 };  // 2,3,6,7 = 2-bit is set             
        int[] aCodes = { 4, 5, 6, 7 };  // 4,5,6,7 = 4-bit is set

        int[] testCodes = d < cThreshold ? cCodes: (d > aThreshold ? aCodes: bCodes);

        for (int i=0;i<4;i++) {
            if (testCodes[i] == powerCode)
                return true;
        }
        return false;
    }
}