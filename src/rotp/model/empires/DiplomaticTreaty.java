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
import rotp.model.incidents.DiplomaticIncident;
import rotp.util.Base;

public class DiplomaticTreaty implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    int empire1;
    int empire2;
    String statusKey;
    float date;
    public DiplomaticTreaty() { }
    public DiplomaticTreaty(Empire e1, Empire e2, String key) {
        empire1 = e1.id;
        empire2 = e2.id;
        statusKey = key;
        date = galaxy().currentTime();
    }
    public String status(Empire e)        { return text(statusKey); }
    public float date()                   { return date; }
    public void nextTurn(Empire emp)      { }
    public void noticeIncident(DiplomaticIncident inc) { }
    public boolean wantToBreak(Empire e)  { return false; }
    public boolean isFinalWar()           { return false; }
    public boolean isWar()                { return false; }
    public boolean isNoTreaty()           { return false; }
    public boolean isPact()               { return false; }
    public boolean isAlliance()           { return false; }
    public boolean isTrade()              { return false; }
    public boolean isUnity()              { return false; }
    public boolean isPeace()              { return false; }
    public int listOrder()                { return 0; }
    public void losePopulation(Empire e, float amt) {  }
    public void loseFactories(Empire e, float amt)  {  }
    public void loseFleet(Empire e, float amt)    {  }
}
