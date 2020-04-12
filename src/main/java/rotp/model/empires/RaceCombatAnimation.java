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

import java.awt.*;
import java.util.List;
import rotp.util.AnimationManager;
import rotp.util.Base;

public class RaceCombatAnimation implements Base {
    public String iconKey;
    public int frames, firingFrame;
    public int gunX, gunY, size;
    public float scale = 1.0f;
    public int xSpacing, ySpacing;

    public Image notFiring()            { return currentFrame(iconKey, Race.notFiring);  }
    public List<Image> firingFrames()   { return AnimationManager.current().allImages(iconKey); }

    public void iconSpec(String s) {
        List<String> vals = substrings(s, ',');
        iconKey = vals.get(0).trim();

        List<String> frameSpec = substrings(vals.get(1).trim(), '/');
        if (frameSpec.size() == 1) {
            firingFrame = -1;
            frames = parseInt(frameSpec.get(0));
        }
        else if (frameSpec.size() == 2) {
            // The string argument is "X/Y" where Y is the
            // total number of firing frames for this race's
            // trooper, and X is the specific frame on which
            // the weapon is fired
            firingFrame = parseInt(frameSpec.get(0));
            frames = parseInt(frameSpec.get(1));
        }
        else
            err("Invalid troopFireNum string: ", s);
    }
    public void fireXY(String s) {
        // The string argument is "X@Y" where x & y are
        // coordinates in the firing image frame where the
        // gun blast should originate from
        List<String> vals = substrings(s, '@');
        if (vals.size() != 2)
            err("Invalid troopFireXY string: ", s);

        gunX = parseInt(vals.get(0));
        gunY = parseInt(vals.get(1));
    }
    public void scaling(String s) {
        List<String> vals = substrings(s, ',');
        if (vals.size() != 3)
            err("Invalid troopScaling string: ", s);

        scale = parseFloat(vals.get(0));
        xSpacing = parseInt(vals.get(1));
        ySpacing = parseInt(vals.get(2));
    }
}
