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
package rotp.model.tech;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import rotp.model.empires.Empire;
import rotp.ui.BasePanel;

public final class TechHandWeapon extends Tech {
    public static final int COLLAPSE = 1;
    public static final int DISRUPT = 2;
    public static final int IMMOLATE = 3;
    public static final int VAPORIZE = 4;
    private static Stroke laserBeam = null;
    private transient Color color;
    private transient int strokeSize;
    public int combatMod = -1;
    public boolean rifle = false;
    public int deathType = COLLAPSE;
    public TechHandWeapon (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public void init() {
        super.init();
        techType = Tech.HAND_WEAPON;

        switch(typeSeq) {
           case 0:
                rifle = false;
                combatMod = 0;
                deathType = COLLAPSE;
                strokeSize = 1;
                color = Color.gray;
                break;
            case 1: // HAND LASERS
                rifle = false;
                combatMod = 5;
                deathType = COLLAPSE;
                color = Color.yellow;
                strokeSize = 1;
                break;
            case 2: // ION RIFLE
                rifle = true;
                combatMod = 10;
                deathType = IMMOLATE;
                color = Color.orange;
                strokeSize = 2;
                break;
            case 3: // FUSION RIFLE
                rifle = true;
                combatMod = 20;
                deathType = IMMOLATE;
                color = Color.red;
                strokeSize = 2;
                break;
            case 4: // HAND PHASOR
                rifle = false;
                combatMod = 25;
                deathType = DISRUPT;
                color = Color.white;
                strokeSize = 2;
                break;
            case 5: // PLASMA RIFLE
                rifle = true;
                combatMod = 30;
                deathType = VAPORIZE;
                color = Color.blue;
                strokeSize = 3;
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 3; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topHandWeaponTech() != null) && (combatMod < c.tech().weaponGroundBonus());
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topHandWeaponTech(this);
    }
    public void drawEffect(Graphics2D g, int x0, int y0, int x1, int y1) {
        if (laserBeam == null)
            laserBeam = new BasicStroke(scaled(2));

        if (strokeSize > 0) {
            Stroke prevStroke = g.getStroke();
            g.setStroke(BasePanel.baseStroke(strokeSize));
            g.setColor(color);
            g.drawLine(x0, y0, x1, y1);
            g.setStroke(prevStroke);
        }
        else {
            g.setColor(color);
            g.fillOval(x0, y0, BasePanel.s1, BasePanel.s1);
        }
    }
}
