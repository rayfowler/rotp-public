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
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import javax.swing.border.AbstractBorder;

public class ThickBevelBorder extends AbstractBorder  implements Base {
    private static final long serialVersionUID = 1L;
    private static final int TOP = 1;
    private static final int LEFT = 2;
    private static final int BOTTOM = 3;
    private static final int RIGHT = 4;

    private int numColors = 2;
    private Color tIn, bIn, lIn, rIn;
    private Color tOut, bOut, lOut, rOut;
    private Color tMid, bMid, lMid, rMid;
    private int thick;
    private int step = 1;
    private final HashMap<Float,Color> colors = new HashMap<>();

    public int thickness()  { return thick; }

    public ThickBevelBorder(int thickness, Color innerColor, Color outerColor) {
    	this(thickness, 1, innerColor, outerColor);
    }
    public ThickBevelBorder(int thickness, int s,  Color innerColor, Color outerColor) {
        lIn = tIn = rIn = bIn = innerColor;
        lOut = tOut = rOut = bOut = outerColor;
        thick = scaled(thickness);
        step = s;
        numColors = 2;
    }
    public ThickBevelBorder(int thickness, int s,  Color innerColor, Color midColor, Color outerColor) {
        lIn  = tIn  = rIn  = bIn = innerColor;
        lMid = tMid = rMid = bMid = midColor;
        lOut = tOut = rOut = bOut = outerColor;
        thick = scaled(thickness);
        step = s;
        numColors = 3;
    }
    public ThickBevelBorder(int thickness, Color topOuter, Color topInner, Color leftOuter, Color leftInner, Color bottomOuter, Color bottomInner, Color rightOuter, Color rightInner) {
    	this(thickness, 1, topOuter, topInner, leftOuter, leftInner, bottomOuter, bottomInner, rightOuter, rightInner);
    }
    public ThickBevelBorder(int thickness, int s, Color topColor1, Color topColor2, Color leftColor1, Color leftColor2, Color bottomColor1, Color bottomColor2, Color rightColor1, Color rightColor2){
        tOut = topColor1;
        bOut = bottomColor1;
        lOut = leftColor1;
        rOut = rightColor1;
        tIn = topColor2;
        bIn = bottomColor2;
        lIn = leftColor2;
        rIn = rightColor2;
        thick = scaled(thickness);
        step = s;
        numColors = 2;
    }
    public ThickBevelBorder(int thickness, int s, int r, Color tOutC, Color tMidC, Color tInC, Color lOutC, Color lMidC, Color lInC, Color bOutC, Color bMidC, Color bInC, Color rOutC, Color rMidC, Color rInC){
        tOut = tOutC;
        tMid = tMidC;
        tIn = tInC;
        bOut = bOutC;
        bMid = bMidC;
        bIn = bInC;
        lOut = lOutC;
        lMid = lMidC;
        lIn = lInC;
        rOut = rOutC;
        rMid = rMidC;
        rIn = rInC;
        thick = scaled(thickness);
        step = s;
        numColors = 3;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        super.paintBorder(c, g, x, y, width, height);     
        paintBorder(g,x,y,width,height);
    }
    public void paintBorder(Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        
        for (int i=0;i<thick;i+=step) {
        	float pct = (float) i/(thick-step);
        	if (numColors == 2) {
	            g2d.setColor(gradientColor(lIn, lOut, LEFT, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+i, step, height-i-i));
	            g2d.setColor(gradientColor(tIn, tOut, TOP, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+i, width-i-i, step));
	            g2d.setColor(gradientColor(rIn, rOut, RIGHT, pct));
	            g2d.fill(new Rectangle2D.Float(x+width-i-step, y+i, step, height-i-i));
	            g2d.setColor(gradientColor(bIn, bOut, BOTTOM, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+height-i-step, width-i-i, step));
        	}
        	else {
	            g2d.setColor(gradientColor(lIn, lMid, lOut, LEFT, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+i, step, height-i-i));
	            g2d.setColor(gradientColor(tIn, tMid, tOut, TOP, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+i, width-i-i, step));
	            g2d.setColor(gradientColor(rIn, rMid, rOut, RIGHT, pct));
	            g2d.fill(new Rectangle2D.Float(x+width-i-step, y+i, step, height-i-i));
	            g2d.setColor(gradientColor(bIn, bMid, bOut, BOTTOM, pct));
	            g2d.fill(new Rectangle2D.Float(x+i, y+height-i-step, width-i-i, step));
        	}
        }       
    }
    private Color gradientColor(Color c2, Color c1, int side, float pct) {
    	if (colors.containsKey(side+pct)) 
    		return colors.get(side+pct);
    	float adjPct = pct;
    	int r0 = c1.getRed()   + (int) (adjPct * (c2.getRed() - c1.getRed()));
    	int g0 = c1.getGreen() + (int) (adjPct * (c2.getGreen() - c1.getGreen()));
    	int b0 = c1.getBlue()  + (int) (adjPct * (c2.getBlue() - c1.getBlue()));
    	int a0 = c1.getAlpha() + (int) (adjPct * (c2.getAlpha() - c1.getAlpha()));
    	Color c = newColor(bounds(0,r0,255),bounds(0,g0,255),bounds(0,b0,255),bounds(0,a0,255));
    	colors.put(side+pct, c);
    	return c;
    }
    private Color gradientColor(Color c3, Color c2, Color c1, int side, float pct) {
    	if (pct <= 0.5)
    		return gradientColor(c2,c1,side,2*pct);
    	else
    		return gradientColor(c3,c2,side,2*pct-1);
    }
    @Override
    public Insets getBorderInsets(Component c) {
        return (getBorderInsets(c, new Insets(thick, thick, thick, thick)));
    }
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = thick;
        return insets;
    }
    @Override
    public boolean isBorderOpaque() {
        return true;
    }
}
