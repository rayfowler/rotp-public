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
package rotp.model.galaxy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.sprites.MapSprite;
import rotp.ui.util.planets.PlanetImager;
import rotp.util.Base;
import rotp.util.FastImage;

public class Nebula extends MapSprite implements Base, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static Color labelColor = new Color(255,255,255,64);
    private Rectangle.Float shape;
    private Rectangle.Float innerShape;
    private int sysId = -1;
    private int numStars = 0;
    private float size, width, height;
    private float x, y;
    private transient BufferedImage image;

    public float width()                  { return width; }
    public float height()                 { return height; }
    public Rectangle.Float shape()        { return shape; }
    public String name() {
        if (sysId < 1)
            return "";
        
        String sysName = player().sv.name(sysId);
        if (sysName.isEmpty())
            return text("NEBULA_ID", sysId);
        else
            return text("NEBULA_NAME", sysName);
    }

    public Nebula copy() {
        Nebula neb = new Nebula();
        neb.size = size;
        neb.width = width;
        neb.height = height;
        neb.image = image;
        return neb;
    }
    public boolean intersects(Nebula n)    { return shape.intersects(n.shape); }
    @Override
    public float x()                      { return x; }
    @Override
    public float y()                      { return y; }
    
    public float adjWidth()                { return size == 0 ? width : size*width; }
    public float adjHeight()                { return size == 0 ? height : size*height; }
    
    public float centerX()                { return x+(adjWidth()/2); }
    public float centerY()                { return y+(adjHeight()/2); }
    
    public boolean noStars()              { return numStars == 0; }
    
    public void setXY(float x1, float y1) {
        x = x1;
        y = y1;
        shape = new Rectangle.Float(x, y, adjWidth(), adjHeight());
        innerShape = new Rectangle.Float(x+1, y+1, adjWidth()-2, adjHeight()-2);
    }
    public Nebula() {
        
    }
    public Nebula(boolean buildImage, float sizeMult) {
        size = max(1, sizeMult);
        width = random(8,14);
        height = random(8,14);
        if (buildImage)
            image = buildImage();
    }
    public void noteStarSystem(StarSystem sys) {
        numStars++;
        if (sysId < 0) {
            sysId = sys.id;
            return;
        }
        float centerX = centerX();
        float centerY = centerY();
        StarSystem currSys = galaxy().system(sysId);
        float currDist = distance(currSys.x(), currSys.y(), centerX, centerY);
        float newDist = distance(sys.x(), sys.y(), centerX, centerY);
        
        if (newDist < currDist)
            sysId = sys.id;
    }
    public void enrichCentralSystem() {
        if (numStars < 3)
            return;
        if (galaxy().numStarSystems() <= 100)
            return;
        
        StarSystem sys = galaxy().system(sysId);
        if (sys.planet().isEnvironmentNone())
            return;
        
        float ultraPct = numStars * .07f;
        if (random() < ultraPct)
            sys.planet().setResourceUltraRich();
        else if (!sys.planet().isResourceUltraRich())
            sys.planet().setResourceRich();
    }
    public boolean intersects(Line2D path) {
        return (innerShape != null) && path.intersects(innerShape);
    }
    public boolean contains(float x, float y) {
        return (innerShape != null) && innerShape.contains(x, y);
    }
    public BufferedImage image() {
        if (image == null)
            image = buildImage();
        return image;
    }
    private BufferedImage buildImage() {
        int w = (int) width()*19;
        int h = (int) height()*12;

        int nebR = roll(160,225);
        int nebG = 0;
        int nebB = roll(160,255);

        //int centerX = w/2;
        //int centerY = h/2;
        FastImage fImg = PlanetImager.current().getTerrainSubImage(w,h);

        int floor = 255;
        int ceiling = 0;
        for (int y=0;y<h;y++)    for (int x=0;x<w;x++) {
            int pixel = fImg.getRGB(x, y);
            floor = min(floor, pixel & 0xff);
            ceiling = max(ceiling, pixel & 0xff);
        }
        for (int x=0;x<w;x++)   for (int y=0;y<h;y++) {
            int pixel = fImg.getRGB(x, y);
            int landLevel = pixel & 0xff;
            landLevel = (int) (256*((float)(landLevel-floor)/(ceiling-floor)));
            int distFromEdgeX = min(x, w-x);
            int distFromEdgeY = min(y, h-y);
            int distFromEdge = min(distFromEdgeX, distFromEdgeY);
            float pctFromEdge = min((float)distFromEdgeX/w, (float)distFromEdgeY/h);
            //int distFromCenter = (int) Math.min(128,Math.sqrt(((x-centerX)*(x-centerX))+((y-centerY)*(y-centerY))));
            int alpha = min(distFromEdge/2, landLevel*3/5);
            alpha = (int) (pctFromEdge * landLevel);
            alpha = min(alpha*3/2, (alpha+255)/2);
           //alpha = Math.min(145-distFromCenter, landLevel/2);
            int newPixel = (alpha << 24) | (nebR << 16) | (nebG << 8) | nebB;
            fImg.setRGB(x, y, newPixel);
        }
        return fImg.image();
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) { return false; }

    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        Rectangle mShape = mapShape(map);
        g2.drawImage(image(), mShape.x, mShape.y, mShape.x+mShape.width, mShape.y+mShape.height, 0, 0, image().getWidth(), image().getHeight(), map);
        float scale = map.scaleX();
        
        if (map.hideSystemNames())
            return;
        
        // use smaller font when we have the full name
        int fontSize = sysId <= 0 ? (int) (size*1800/scale) : (int) (size*1200/scale);
        if (fontSize >= 14) {
            String name = name();
            if (!name.isEmpty()) {
                g2.setFont(narrowFont(fontSize));
                g2.setColor(labelColor);
                int sw = g2.getFontMetrics().stringWidth(name);
                int x0 = mShape.x+((mShape.width-sw)/2);
                int y0 = mShape.y+((mShape.height-fontSize)/2);
                drawString(g2, name, x0, y0);
            }
        }
    }
    private Rectangle mapShape(GalaxyMapPanel map) {
        int x0 = map.mapX(x);
        int y0 = map.mapY(y);
        int x1 = map.mapX(x+(int)adjWidth());
        int y1 = map.mapY(y+(int)adjHeight());
        return new Rectangle(x0,y0, x1-x0, y1-y0);
    }
}
