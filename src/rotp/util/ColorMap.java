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
package rotp.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColorMap implements Base {
    List<Color> colors = new ArrayList<>();
    List<ColorMark> marks = new ArrayList<>();
    public void addColorMark(float pct, int r0, int g0, int b0) {
        marks.add(new ColorMark(pct, r0, g0, b0));
    }
    public Color colorAt(int alt, int sea) {
        if (colors.isEmpty())
            genColors();

        float pct = (alt > sea) ? (float)(alt-sea)/(255-sea) : (float)(alt-sea)/sea;
        int index = 100+(int) (100*pct);
        return colors.get(bounds(0,index,colors.size()-1));
    }
    private void genColors() {
        for (int i=-100; i<=100; i++) 
            colors.add(genColorAt((float)i/100));
    }
    private Color genColorAt(float pct) {
        if (marks.isEmpty())
            return null;

        ColorMark cm1 = marks.get(0);
        ColorMark cm2 = null;
        for (ColorMark cm: marks) {
            if (cm1 == null)
                cm1 = cm;
            if (cm2 == null) {
                if (cm.p <= pct)
                    cm1 = cm;
                if (cm.p >= pct) {
                    cm2 = cm;
                }
            }
        }
        if (cm2 == null)
            cm2 = cm1;

        if (cm1.p == cm2.p) 
            return newColor(cm1.r, cm1.g, cm1.b);

        float amt = (pct - cm1.p) / (cm2.p - cm1.p);
        int r = bounds(0,cm1.r+(int)(amt*(cm2.r-cm1.r)),255);
        int g = bounds(0,cm1.g+(int)(amt*(cm2.r-cm1.r)),255);
        int b = bounds(0,cm1.b+(int)(amt*(cm2.r-cm1.r)),255);
        return newColor(r,g,b);
    }
    class ColorMark {
        float p;
        int r;
        int g;
        int b;
        public ColorMark(float pct, int r0, int g0, int b0) {
            p = pct;
            r = r0;
            g = g0;
            b = b0;
        }
    }
}
