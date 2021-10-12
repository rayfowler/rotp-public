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
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.ui.BasePanel;
import rotp.ui.FadeInPanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;

public final class GameOverUI extends FadeInPanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    private static Composite[] trans;
    private final Color greenEdgeC = new Color(44,59,30);
    private final Color greenMidC = new Color(70,93,48);
    private int transIndex;
    BufferedImage backImg;
    private LinearGradientPaint back1, back2;
    Rectangle exitBox = new Rectangle();
    Rectangle replayBox = new Rectangle();
    Shape hoverBox;
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
        drawString(g,title, s10, s35);
        
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
            g.fillRoundRect(x0-s20, y0, lineW+s30, textBoxH,s30,s30);

            g.setColor(Color.lightGray);
            y0 += lineH;
            for (String line : lines) {
                y0 += lineH;
                //int sw = g.getFontMetrics().stringWidth(line);
                // only draw text border when fade-in is complete
                if (transIndex == trans.length-1) 
                    drawBorderedString(g, line, x0, y0, Color.black, Color.lightGray);
                else
                    drawString(g,line, x0, y0);
            }
            g.setComposite(preComp);
        }
        if (textFinished())
            drawButtons(g);

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
    public void drawButtons(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        g.setFont(narrowFont(24));
        String exitText = text("GAME_OVER_EXIT");
        String replayText = text("GAME_OVER_REPLAY");
        int sw0 = g.getFontMetrics().stringWidth(exitText);
        int sw1 = g.getFontMetrics().stringWidth(replayText);
        
        exitBox.setBounds(w-sw0-s70, h-s45, sw0+s60, s40);
        replayBox.setBounds(exitBox.x-s80-sw1, h-s45, sw1+s60, s40);
        
        if (back1 == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC};
            Point2D pt1 = new Point2D.Float(replayBox.x, 0);
            Point2D pt2 = new Point2D.Float(replayBox.x + replayBox.width, 0);
            back1 = new LinearGradientPaint(pt1, pt2, dist, greenColors);                
            pt1 = new Point2D.Float(exitBox.x, 0);
            pt2 = new Point2D.Float(exitBox.x + exitBox.width, 0);
            back2 = new LinearGradientPaint(pt1, pt2, dist, greenColors);                
        }
        
        // draw replay button
        g.setColor(SystemPanel.blackText);
        g.fillRoundRect(replayBox.x+s3, replayBox.y+s3, replayBox.width, replayBox.height, s8, s8);           
        boolean hovering = hoverBox == replayBox;
        g.setPaint(back1);
        g.fillRoundRect(replayBox.x, replayBox.y, replayBox.width, replayBox.height, s8, s8);
        Stroke prevStr = g.getStroke();
        Color c0;
        if (hovering) {
            c0 = SystemPanel.yellowText;
            g.setStroke(stroke2);
        }
        else {
            c0 = SystemPanel.whiteText;
            g.setStroke(BasePanel.stroke1);              
        }
        g.setColor(c0);
        g.drawRoundRect(replayBox.x, replayBox.y, replayBox.width, replayBox.height, s8, s8);
        g.setStroke(prevStr);
        int x2a = replayBox.x + ((replayBox.width - sw1) / 2);
        drawShadowedString(g, replayText, x2a, replayBox.y + replayBox.height - s12, Color.black, c0);
           
        
        // draw exit button
        g.setColor(SystemPanel.blackText);
        g.fillRoundRect(exitBox.x+s3, exitBox.y+s3, exitBox.width, exitBox.height, s8, s8);           
        hovering = hoverBox == exitBox;
        g.setPaint(back2);
        g.fillRoundRect(exitBox.x, exitBox.y, exitBox.width, exitBox.height, s8, s8);
        prevStr = g.getStroke();
        if (hovering) {
            c0 = SystemPanel.yellowText;
            g.setStroke(stroke2);
        }
        else {
            c0 = SystemPanel.whiteText;
            g.setStroke(BasePanel.stroke1);              
        }
        g.setColor(c0);
        g.drawRoundRect(exitBox.x, exitBox.y, exitBox.width, exitBox.height, s8, s8);
        g.setStroke(prevStr);
        x2a = exitBox.x + ((exitBox.width - sw0) / 2);
        drawShadowedString(g, exitText, x2a, exitBox.y + exitBox.height - s12, Color.black, c0);
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
        else if (session().status().lostNoColonies())
            return text("GAME_OVER_NO_COLONIES_LOSS");
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

        String resultText = "";
        if (session().status().lostOverthrown())
            resultText = text("GAME_OVER_OVERTHROWN_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostMilitary())
            resultText = text("GAME_OVER_MILITARY_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostDiplomatic())
            resultText = text("GAME_OVER_COUNCIL_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostNewRepublic())
            resultText = text("GAME_OVER_COUNCIL_MILITARY_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().lostRebellion()) {
            String special = ruler.race().text("GAME_OVER_REBELLION_LOSS3");
            resultText = text("GAME_OVER_REBELLION_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire, special);
        }
        else if (session().status().lostNoColonies()) {
            resultText = text("GAME_OVER_NO_COLONIES_LOSS2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
            resultText = player().replaceTokens(resultText, "player");
        }
        else if (session().status().wonDiplomatic())
            resultText = text("GAME_OVER_COUNCIL_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonMilitary())
            resultText = text("GAME_OVER_MILITARY_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonMilitaryAlliance()) 
            resultText = text("GAME_OVER_MILITARY_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonNewRepublic())
            resultText = text("GAME_OVER_COUNCIL_MILITARY_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonRebellion())
            resultText = text("GAME_OVER_REBELLION_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);
        else if (session().status().wonCouncilAlliance()) {
            String special = ruler.race().text("GAME_OVER_ALLIANCE_WIN3");
            resultText = text("GAME_OVER_COUNCIL_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire, special);
        }
        else if (session().status().wonRebellionAlliance())
            return text("GAME_OVER_REBEL_ALLIANCE_WIN2", year, pName, pRace, pEmpire, rName, rRace, rEmpire);

        resultText = pl.replaceTokens(resultText, "player");
        if (ruler != null)
            resultText = ruler.replaceTokens(resultText, "leader");
        
        return resultText;
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
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        Shape prevHover = hoverBox;
        hoverBox = null;

        if (exitBox.contains(x,y))
            hoverBox = exitBox;
        else if (replayBox.contains(x,y))
            hoverBox = replayBox;

        if (prevHover != hoverBox) 
           repaint();
    }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent e) { 
        if (e.getButton() > 3)
            return;
        if (!textFinished())
            return;
        if (hoverBox == exitBox) {
            softClick(); 
            advanceMode();
            return;
        }
        else if (hoverBox == replayBox) {
            softClick(); 
            RotPUI.instance().selectHistoryPanel(player().id, true);
            return;
        }
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
