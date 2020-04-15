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
package rotp.ui;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class FadeInPanel extends BasePanel {
    private static final long serialVersionUID = 1L;
    long msRemaining = 0;
    long targetTime = 0;

    @Override
    public boolean drawMemory()            { return true; }
    public int fadeInMs()        { return 1000; }
    public boolean stillFading()  { return playAnimations() && (msRemaining > 0); }
    public void startFadeTimer() {
        if (playAnimations()) {
            msRemaining = fadeInMs();
            targetTime = System.currentTimeMillis()  + fadeInMs();
        }
        else {
            msRemaining = 0;
            targetTime = System.currentTimeMillis();
        }
    }
    public void endFade()   { msRemaining = 0; }
    public void advanceFade() {
        if (stillFading())
            msRemaining = targetTime - System.currentTimeMillis();
    }
    public void drawOverlay(Graphics g) {
        BufferedImage img = fadeInOverlay();
        if (img != null)
            g.drawImage(img, 0, 0, null);
    }
    private BufferedImage fadeInOverlay() {
        if (!stillFading())
            return null;

        float alpha = (float) Math.sqrt((float)msRemaining / fadeInMs());
        alpha = max(0,min(1,alpha));
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(newColor(0,0,0,(int)(alpha*255)));
        g2.fillRect(0,0,getWidth(), getHeight());
        return image;
    }
}
