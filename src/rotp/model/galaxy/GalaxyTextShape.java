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

import java.awt.Point;
import java.awt.Shape;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import rotp.util.Base;
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Text
public class GalaxyTextShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	
	float adjust_densityX = 4.0f;
	float adjust_densityY = 1.5f;
	
    public GalaxyTextShape(IGameOptions options) {
        opts = options;
    }
	
	Shape textShape;
	
	public void init(int n) {
        super.init(n);
		
		BufferedImage img = new BufferedImage(galaxyWidthLY(), galaxyHeightLY(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		
		// Monospaced font used for constant spacing
		// but maybe other fonts have better kerning for connectivity?
		Font font1 = new Font("Monospaced", Font.PLAIN, 96);
		Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
		// use TextAttribute.TRACKING to cram letters together for better connectivity
		attributes.put(TextAttribute.TRACKING, -0.17);
		Font font2 = font1.deriveFont(attributes);
		
		// modnar: choose text string with setMapOption
		// TODO: work out multi-line text
		// some text strings will have issues with connectivity regardless of TextAttribute.TRACKING
		if (opts.setMapOption() == 1) {
			// "ROTP"
			GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), "ROTP");
			textShape = v.getOutline();
        }
		else if (opts.setMapOption() == 2) {
			// "MoO1", using unicode homoglyphs
			GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), "ℳo○𝟏");
			textShape = v.getOutline();
        }
		else if (opts.setMapOption() == 3) {
			// User-input Homeworld name, user can change colony name afterwards in-game
			String custStr = text(options().selectedHomeWorldName());
			GlyphVector v = font2.createGlyphVector(g2.getFontRenderContext(), custStr);
			textShape = v.getOutline();
        }
		
		// set galaxy aspect ratio to the textShape aspect ratio
		// this accommodates very long or short text strings
		// and multi-line texts in the future
		adjust_densityX = (float) (adjust_densityY * textShape.getBounds().getWidth() / textShape.getBounds().getHeight());
		
		// rescale textShape to fit galaxy map, then move into map center
		AffineTransform scaleText = new AffineTransform();
		AffineTransform moveText = new AffineTransform();
		
		// rescale
		double zoomX = (galaxyWidthLY() - 4*galaxyEdgeBuffer()) / textShape.getBounds().getWidth();
		double zoomY = (galaxyHeightLY() - 4*galaxyEdgeBuffer()) / textShape.getBounds().getHeight();
		double zoom = Math.min(zoomX, zoomY);
		scaleText.scale(zoom, zoom);
		textShape = scaleText.createTransformedShape(textShape);
		
		// recenter
		double oldx = textShape.getBounds().getX();
		double oldy = textShape.getBounds().getY();
		moveText.translate((galaxyWidthLY()-textShape.getBounds().getWidth())/2 - oldx, (galaxyHeightLY()-textShape.getBounds().getHeight())/2 - oldy);
		textShape = moveText.createTransformedShape(textShape);

	}
	
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(adjust_densityX*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(adjust_densityY*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
        return textShape.contains(pt.x, pt.y);
    }
    float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 10; 
            case IGameOptions.SIZE_SMALL:     return 15; 
            case IGameOptions.SIZE_SMALL2:    return 17;
            case IGameOptions.SIZE_MEDIUM:    return 19; 
            case IGameOptions.SIZE_MEDIUM2:   return 20; 
            case IGameOptions.SIZE_LARGE:     return 21; 
            case IGameOptions.SIZE_LARGE2:    return 22; 
            case IGameOptions.SIZE_HUGE:      return 23; 
            case IGameOptions.SIZE_HUGE2:     return 24; 
            case IGameOptions.SIZE_MASSIVE:   return 25; 
            case IGameOptions.SIZE_MASSIVE2:  return 26; 
            case IGameOptions.SIZE_MASSIVE3:  return 27; 
            case IGameOptions.SIZE_MASSIVE4:  return 28; 
            case IGameOptions.SIZE_MASSIVE5:  return 29; 
            case IGameOptions.SIZE_INSANE:    return 32; 
            case IGameOptions.SIZE_LUDICROUS: return 36; 
            default:             return 19; 
        }
    }

}
