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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

public class ShadowBorder  extends AbstractBorder implements Base {
    private static final long serialVersionUID = 1L;
    private final Color borderC;
    private final Color shadeC;

    public ShadowBorder(Color border, Color shade) {
        borderC = border;
        shadeC = shade;
    }
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        super.paintBorder(c, g, x, y, w, h);

        int s1 = scaled(1);
        int s2 = scaled(2);
        int s3 = scaled(3);
        int s4 = scaled(4);

        g.setColor(shadeC);
        g.drawRect(x+s2, y+s2, w-s4, h-s4);
        g.drawRect(x+s3, y+s3, w-s4, h-s4);

        g.setColor(borderC);
        g.drawRect(x+0,  y+0,  w-s4, h-s4);
        g.drawRect(x+s1, y+s1, w-s4, h-s4);
    }
    @Override
    public Insets getBorderInsets(Component c)  {
        return (getBorderInsets(c, new Insets(4, 4, 4, 4)));
    }
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = 4;
        return insets;
    }
    @Override
    public boolean isBorderOpaque() { return false; }
}
