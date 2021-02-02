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
package rotp.model.ships;

import java.awt.Image;
import java.io.Serializable;
import javax.swing.ImageIcon;
import rotp.model.empires.Empire;
import rotp.util.Base;

public class Design implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private ShipDesignLab lab;
    private String name;
    protected int seq = 0;
    private int id = -1;

    private int buildCount = 0;
    private boolean obsolete = false;
    private boolean scrapped = false;
    protected boolean active = true;

    // historical data
    private int totalBuilt = 0;
    private int totalDestroyed = 0;
    private int totalUsed = 0;
    private int totalScrapped = 0;

    public Design() {  }

    public ShipDesignLab lab()           { return lab; }
    public void lab(ShipDesignLab l)     { lab = l; }
    public String name()                 { return name == null ? "" : name; }
    public void name(String s)           { name = s; }
    public boolean active()              { return active; }
    public void active(boolean b)        { active = b; }
    public boolean isShip()              { return false; }
    public int id()                      { return id; }
    public void id(int i)                { id = i; }
    public int seq()                     { return seq; }
    public boolean obsolete()            { return obsolete; }
    public void obsolete(boolean b)      { obsolete = b; }
    public boolean scrapped()            { return scrapped; }
    public void scrapped(boolean b)      { scrapped = b; }
    public void resetBuildCount()        { buildCount = 0; }
    public void addBuildCount(int i)     { buildCount += i; }
    public int buildCount()              { return buildCount; }
    public int totalBuilt()              { return totalBuilt; }
    public void addTotalBuilt(int i)     { totalBuilt += i; }
    public int totalDestroyed()          { return totalDestroyed; }
    public void addTotalDestroyed(int i) { totalDestroyed += i; }
    public int totalUsed()               { return totalUsed; }
    public void addTotalUsed(int i)      { totalUsed += i; }
    public int totalScrapped()           { return totalScrapped; }
    public void addTotalScrapped(int i)  { totalScrapped += i; }

    public int cost()                 { return 0; }
    protected ImageIcon icon()        { return null;}
    public Empire empire()            { return lab.empire(); }

    public Image  image()          { return icon() == null ? null : icon().getImage(); }
}
