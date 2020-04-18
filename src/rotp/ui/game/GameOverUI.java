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
package rotp.ui.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.ui.FadeInPanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;

public final class GameOverUI extends FadeInPanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    private static Composite[] trans;
    private int transIndex;
    BufferedImage backImg;
    int fadeDelay = 0;
    public GameOverUI() {
        init0();
    }
    private void init0() {
        setBackground(Color.black);
        addMouseListener(this);
        addMouseMotionListener(this);
        
    }
    public boolean textFinished()  { return transIndex >= (trans.length -1); }
    @Override
    public int fadeInMs()        { return 2000; }
    public void init() {
        startFadeTimer();
        Race r = player().race();
        Image gameOverImage = session().status().lost() ? image(r.lossSplashKey) : image(r.winSplashKey);
        
        if (gameOverImage == null) {
            backImg = GalaxyMapPanel.sharedStarBackground;
            fadeDelay = 1000;
        }
        else {
            backImg = newBufferedImage(gameOverImage);
            fadeDelay = 3000;
        }
            
        if (trans == null) {
            trans = new Composite[20];
            for (int i=0;i<trans.length;i++) 
                trans[i]  = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)i/trans.length);
        }
        transIndex = -1;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawEndingScene(screenBuffer()); 
        g.drawImage(screenBuffer(),0,0,null);
    }
    private void drawEndingScene(Image img) {
        long t0 = System.currentTimeMillis();
        int w = getWidth();
        int h = getHeight();
        Graphics2D g = (Graphics2D) img.getGraphics();
        setFontHints(g);

        g.drawImage(backImg, 0, 0, w, h, null);

        g.setFont(narrowFont(30));
        g.setColor(Color.lightGray);
        String title = gameOverTitle();
        g.drawString(title, s10, s35);
        String score = text("GAME_OVER_FINAL_SCORE", session().status().finalScore());
        int sw0 = g.getFontMetrics().stringWidth(score);
        g.drawString(score, w-sw0-s30, s35);
        
        if (transIndex >= 0) {
            int lineH = s30;
            int lineW = w*2/3;
            g.setFont(font(26));
            List<String> paragraphs = substrings(gameOverText(), '#');
            List<String> lines = new ArrayList<>();
            for (String para : paragraphs) {
                lines.addAll(wrappedLines(g, para, lineW));
                lines.add("");
            }
            int textBoxH = lineH*(lines.size()+1);
            int y0 = (h-textBoxH)/ 2;
            int x0 = (w-lineW)/2;
            Composite preComp = g.getComposite();
            g.setComposite(trans[transIndex]);
            g.setColor(new Color(0,0,0,128));
            g.fillRect(x0-s20, y0, lineW+s30, textBoxH);

            g.setColor(Color.lightGray);
            y0 += lineH;
            for (String line : lines) {
                y0 += lineH;
                //int sw = g.getFontMetrics().stringWidth(line);
                // only draw text border when fade-in is complete
                if (transIndex == trans.length-1) 
                    drawBorderedString(g, line, x0, y0, Color.black, Color.lightGray);
                else
                    g.drawString(line, x0, y0);
            }
            g.setComposite(preComp);
        }
        if (textFinished())
            drawSkipText(g, true);

        drawOverlay(g);
        
        // fade in the descriptive text
        if (!stillFading() && (transIndex < trans.length -1)) {
            // on the first time through (transIndex = -1)
            // wait 3 seconds to let the background image linger
            // before fading in text over it
            if (transIndex < 0) {
                sleep(fadeDelay);
                transIndex = 0;
            }
            transIndex = Math.min(transIndex+1, trans.length-1);
            int dur = (int) min(200, System.currentTimeMillis() - t0);
            sleep(200-dur);
            repaint();
            return;
        }
    }
    public String gameOverTitle() {
        if (session().status().lostOverthrown())
            return text("GAME_OVER_OVERTHROWN_LOSS");
        else if (session().status().lostMilitary())
            return text("GAME_OVER_MILITARY_LOSS");
        else if (session().status().lostDiplomatic())
            return text("GAME_OVER_DIPLOMATIC_LOSS");
        else if (session().status().lostNewRepublic())
            return text("GAME_OVER_NEW_REPUBLIC_LOSS");
        else if (session().status().lostRebellion())
            return text("GAME_OVER_REBELLION_LOSS");
        else if (session().status().wonDiplomatic())
            return text("GAME_OVER_DIPLOMATIC_WIN");
        else if (session().status().wonMilitary())
            return text("GAME_OVER_MILITARY_WIN");
        else if (session().status().wonMilitaryAlliance())
            return text("GAME_OVER_MILITARY_ALLIANCE_WIN");
        else if (session().status().wonNewRepublic())
            return text("GAME_OVER_NEW_REPUBLIC_WIN");
        else if (session().status().wonRebellion())
            return text("GAME_OVER_REBELLION_WIN");
        else if (session().status().wonCouncilAlliance())
            return text("GAME_OVER_ALLIANCE_WIN");
        else if (session().status().wonRebellionAlliance())
            return text("GAME_OVER_REBEL_ALLIANCE_WIN");

        return "";
    }
    private String gameOverText() {
        Empire pl = player();
        String year = str(galaxy().currentYear());
        String pName = pl.leader().name();
        String pRace = pl.raceName();
        String pEmpire = pl.name();
        Empire ruler = galaxy().council().leader();
        String rName = ruler == null ? "" : ruler.leader().name();
        String rRace = ruler == null ? "" : ruler.raceName();
        String rEmpire = ruler == null ? "" :ruler.label("_race_plural");

        if (session().status().lostOverthrown())
            return text("GAME_OVER_OVERTHROWN_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostMilitary())
            return text("GAME_OVER_MILITARY_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostDiplomatic())
            return text("GAME_OVER_DIPLOMATIC_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostNewRepublic())
            return text("GAME_OVER_NEW_REPUBLIC_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostRebellion()) {
            String special = ruler.text("GAME_OVER_REBELLION_LOSS3");
            return text("GAME_OVER_REBELLION_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire, special);
        }
        else if (session().status().wonDiplomatic())
            return text("GAME_OVER_DIPLOMATIC_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonMilitary())
            return text("GAME_OVER_MILITARY_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonMilitaryAlliance()) 
            return text("GAME_OVER_MILITARY_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonNewRepublic())
            return text("GAME_OVER_NEW_REPUBLIC_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonRebellion())
            return text("GAME_OVER_REBELLION_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonCouncilAlliance()) {
            String special = ruler.text("GAME_OVER_ALLIANCE_WIN3");
            return text("GAME_OVER_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire, special);
        }
        else if (session().status().wonRebellionAlliance())
            return text("GAME_OVER_REBEL_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);

        return "";
    }
    private void advanceMode() {
        stopAmbience();
        RotPUI.instance().selectGamePanel();
    }
    @Override
    public void animate() {
        advanceFade();

        repaint();
    }
    @Override
    public String ambienceSoundKey() { 
        if (session().status().won())
            return "VictoryAmbience";
        else if (session().status().lost())
            return "DefeatAmbience";
        else
            return "";
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) { }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent arg0) { }
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent e) { 
        if (e.getButton() > 3)
            return;
        if (!textFinished())
            return;
        advanceMode();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (!textFinished())
            return;

        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                advanceMode();
                return;
        }
    }
}
