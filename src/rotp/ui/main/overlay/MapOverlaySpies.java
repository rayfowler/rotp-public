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
package rotp.ui.main.overlay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SabotageMission;
import rotp.model.empires.SpyReport;
import rotp.model.tech.Tech;
import rotp.ui.BasePanel;
import static rotp.ui.BasePanel.stroke2;
import rotp.ui.RotPUI;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticMessage;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.MapSprite;

public class MapOverlaySpies extends MapOverlay {
    Color maskC  = new Color(40,40,40,160);
    MainUI parent;
    BufferedImage labImg;

    private final List<Empire> empires = new ArrayList<>();
    private final List<EmpireTabSprite> tabs = new ArrayList<>();
    private Empire selectedEmpire;
    boolean drawSprites = false;
    Color darkShadingC = newColor(50,50,50);
    CloseButton closeButton = new CloseButton();
    IntelligenceButton intelButton = new IntelligenceButton();
    ThreatenButton threatenButton = new ThreatenButton();
    
    public MapOverlaySpies(MainUI p) {
        parent = p;
    }
    public boolean shouldDisplay() {
        return !empires.isEmpty();
    }
    public void init() {
        labImg = null;
        empires.clear();
        tabs.clear();
        closeButton.parent(this);
        intelButton.parent(this);
        threatenButton.parent(this);
        drawSprites = true;
        List<Empire> allEmpires = player().contactedEmpires();
        for (Empire emp: allEmpires) {
            EmpireView v = player().viewForEmpire(emp.id);
            SpyReport rpt = v.spies().report();
            if (rpt.hasActivity()) 
                empires.add(emp);               
        }
        Collections.sort(empires, Empire.RACE_NAME);
        selectedEmpire = empires.isEmpty() ? null : empires.get(0);
        for (Empire e: empires) 
            tabs.add(new EmpireTabSprite(this, e));
    }
    public void selectEmpire(Empire e) {
        selectedEmpire = e;
        parent.repaint();
    }
    public void manageSpies() {
        RotPUI.instance().selectRacesPanel();
        RotPUI.instance().racesUI().selectIntelligenceTab();
        RotPUI.instance().racesUI().selectedEmpire(selectedEmpire);
        parent.hoveringOverSprite(null);
    }
    public void threaten() {
        EmpireView view = selectedEmpire.viewForEmpire(player());
        DiplomaticMessage.show(view, DialogueManager.DIPLOMACY_THREATEN_MENU, true);
        parent.hoveringOverSprite(null);
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_C:
                advanceMap();
                break;
            case KeyEvent.VK_M:
                if (selectedEmpire != null)
                    manageSpies();
                break;
            case KeyEvent.VK_T:
                if (selectedEmpire != null)
                    threaten();
                break;
            case KeyEvent.VK_TAB:
                if (empires.size()> 1) {
                    int nextI = empires.indexOf(selectedEmpire);
                    if (e.isShiftDown()) {
                        nextI--;
                        if (nextI < 0)
                            nextI = empires.size()-1;
                    }
                    else {
                        nextI++;
                        if (nextI >= empires.size())
                            nextI = 0;
                    }
                    selectEmpire(empires.get(nextI));
                    break;
                }
            default:
                misClick(); break;
        }
        return true;
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (!drawSprites)
            return;
        
        int w = ui.getWidth()-scaled(150);
        int h = ui.getHeight()-BasePanel.s30;
        Empire pl = player();
        
        int bdr = BasePanel.s7;
        int s10 = BasePanel.s10;

        parent.hideDisplayPanel();
        // draw map mask
        g.setColor(maskC);
        g.fillRect(0,0,ui.getWidth(),ui.getHeight());
        
        int extraEmps = min(0, max(6,24-empires.size()));
        int tabW = empires.isEmpty() ? 0 : scaled(100);
        int h0 = scaled(450) +(extraEmps*BasePanel.s24);
        int x0 = scaled(250)+(tabW/2);
        int y0 = (h-h0)/2;
        int w0 = scaled(580)+tabW;
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int leftW = scaled(250);

        int x1 = x0 + bdr;
        int y1 = y0 + bdr;
        int w1 = w0 - tabW - bdr - bdr;
        int h1 = h0 - bdr - bdr;
        int infoW = w1-leftW;

        int x2 = x1+leftW+s10;
        int x3 = x1+leftW+infoW;

        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        int buttonH = BasePanel.s25;
        int buttonY = y1+h1-buttonH-BasePanel.s5;
        int closeW = closeButton.getWidth(g);
        int closeX = x1+w1-closeW-BasePanel.s5;
        closeButton.setBounds(closeX,buttonY,closeW,buttonH);
        
        int intelW = intelButton.getWidth(g);
        int intelX = closeX-intelW-BasePanel.s5;
        intelButton.setBounds(intelX,buttonY,intelW,buttonH);
        
        int threatW = threatenButton.getWidth(g);
        int threatX = intelX-threatW-BasePanel.s5;
        threatenButton.setBounds(threatX,buttonY,threatW,buttonH);
        
        // draw year/turn info
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x1a = x1+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x1a, y1+BasePanel.s45, SystemPanel.textShadowC, SystemPanel.orangeText);

        // draw title
        String subtitle = text("NOTICE_SPIES_TITLE");
        g.setFont(narrowFont(26));
        sw = g.getFontMetrics().stringWidth(subtitle);
        x1a = x1+((leftW-sw)/2);
        drawShadowedString(g, subtitle, 3, x1a, y1+BasePanel.s85, SystemPanel.textShadowC, Color.white);

        if (labImg == null) {
            labImg = asBufferedImage(pl.race().laboratory());
            Graphics imgG = labImg.getGraphics();
            BufferedImage spyImg = pl.race().spyQuiet();
            int w0a = labImg.getWidth();
            int h0a = labImg.getHeight();
            imgG.drawImage(spyImg, w0a/2, 0, w0a, h0a, 0, 0, (spyImg.getWidth()/2), spyImg.getHeight(), null);
            imgG.dispose();
        }

        int y1b = y1+BasePanel.s100;
        int h1b = h1-BasePanel.s100;
        int imgH = h1b;
        int imgW = imgH*Rotp.IMG_W/Rotp.IMG_H;
        g.setClip(x1+BasePanel.s5,y1b,leftW-BasePanel.s5,h1b-BasePanel.s5);
        g.drawImage(labImg, x1+leftW-imgW, y1b, x1+leftW, y1b+h1b, 0, 0, labImg.getWidth(), labImg.getHeight(), null);
        g.setClip(null);

        closeButton.draw(ui, g);
        parent.addNextTurnControl(closeButton);

        int descW = infoW-BasePanel.s20;
        int lineH = BasePanel.s18;

        if (selectedEmpire == null) {
            int y1a = y1+BasePanel.s100;
            String none = text("NOTICE_SPIES_NO_ACTIVITY");
            g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            List<String> lines = wrappedLines(g, none, descW); 
            for (String line: lines) {
                y1a += lineH;
                drawString(g,line, x2, y1a);
            }
            return;
        }
        
        intelButton.draw(ui, g);
        parent.addNextTurnControl(intelButton);
        
        if (pl.diplomatAI().canThreaten(selectedEmpire)) {
            threatenButton.draw(ui, g);
            parent.addNextTurnControl(threatenButton);
        }

        EmpireView v = pl.viewForEmpire(selectedEmpire.id);
        
        // draw selected empire name
        int y2 = y1+BasePanel.s25;
        g.setFont(narrowFont(26));
        drawShadowedString(g, selectedEmpire.name(), 3, x2, y2, SystemPanel.textShadowC, Color.white);
        
        // draw treaty status
        y2 = y2 + BasePanel.s24;
        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        drawString(g,v.embassy().treatyStatus(), x2, y2);

        g.setColor(SystemPanel.blackText);
        // draw spies caught
        SpyReport rpt = v.spies().report();
        int ourSpiesLost = rpt.spiesLost();
        if (ourSpiesLost > 0) {
            y2 += lineH;
            String desc = text("NOTICE_SPIES_LOST_DESC", str(ourSpiesLost));
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (rpt.confessedMission() != null) {
                switch(rpt.confessedMission()) {
                    case SABOTAGE: desc = concat(desc," ", text("NOTICE_SPIES_LOST_CONFESSED")); break;
                    case ESPIONAGE: desc = concat(desc," ", text("NOTICE_SPIES_LOST_CONFESSED2")); break;
                    case HIDE: 
                        if (selectedEmpire.leader().isXenophobic())
                            desc = concat(desc, " ", text("NOTICE_SPIES_LOST_CONFESSED3")); break;
                }
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        // show enemy spies that we caught
        int theirSpiesLost = rpt.spiesCaptured();
        if (theirSpiesLost > 0) {
            SpyReport theirRpt = v.otherView().spies().report();
            y2 += lineH;
            String desc = text("NOTICE_SPIES_CAUGHT_DESC", str(theirSpiesLost));
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (theirRpt.confessedMission() != null) {
                switch(theirRpt.confessedMission()) {
                    case SABOTAGE: desc = concat(desc," ", text("NOTICE_SPIES_CAUGHT_CONFESSED")); break;
                    case ESPIONAGE: desc = concat(desc," ", text("NOTICE_SPIES_CAUGHT_CONFESSED2")); break;
                }
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        // show any sabotage
        if (rpt.sabotageCount() > 0) {
            y2 += lineH;
            String desc = "";
            switch (rpt.sabotageMission()) {
                case SabotageMission.BASES: 
                    desc = text("NOTICE_SPIES_SABOTAGE_BASES", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem)); 
                    break;
                case SabotageMission.FACTORIES: 
                    desc = text("NOTICE_SPIES_SABOTAGE_FACTORIES", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem)); 
                    break;
                case SabotageMission.REBELLION: 
                    desc = text("NOTICE_SPIES_SABOTAGE_REBELS", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem)); 
                    break;
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
                
        // show any techs we stole
        if (rpt.stolenTech() != null) {
            y2 += lineH;
            Tech t = tech(rpt.stolenTech());
            Empire framed = rpt.framedEmpire();
            String desc = text("NOTICE_SPIES_ESPIONAGE", t.name());
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (framed != null) {
                desc = concat(desc, " ", text("NOTICE_SPIES_ESPIONAGE_FRAME"));
                desc = framed.replaceTokens(desc, "framed");
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
                
        // informs if we were framed
        if (rpt.wasFramed()) {
            y2 += lineH;
            String desc = text("NOTICE_SPIES_FRAMED");
            desc = selectedEmpire.replaceTokens(desc, "alien");
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }

        // show list of techs learned
        if (!rpt.techsLearned().isEmpty()) {
            y2 += lineH;         
            List<String> techNames = new ArrayList<>();
            for (String tId : rpt.techsLearned()) 
                techNames.add(tech(tId).name());
            Collections.sort(techNames);
            String techList = techNames.get(0);
            if (techNames.size() > 1) {
                for (int i=1;i<techNames.size();i++) 
                    techList = text("NOTICE_SPIES_MULTIPLE_TECHS", techList, techNames.get(i));
            }
            String desc = text("NOTICE_SPIES_LEARNED_TECH", techList);
            desc = selectedEmpire.replaceTokens(desc, "alien");
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW); 
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        
        
        // draw tabs
        int y3 = y0+bdr;
        int tabSp = BasePanel.s2;
        for (EmpireTabSprite tab: tabs) {
            tab.setPosition(x3,y3);
            y3 = y3+tab.tabH+tabSp;
            tab.draw(ui, g);
        }
            
        
        for (EmpireTabSprite tab: tabs) 
            parent.addNextTurnControl(tab);
    }
    @Override
    public void advanceMap() {
        drawSprites = false;
        parent.resumeOutsideTurn();
    }
    class EmpireTabSprite extends MapSprite {
        private int tabX, tabY, tabW, tabH;
        private int fontSize = 16;

        private final MapOverlaySpies parent;
        private final Empire empire;

        protected int mapX()      { return tabX; }
        protected int mapY()      { return tabY; }
        public void mapX(int i)   { tabX = i; }
        public void mapY(int i)   { tabY = i; }

        public int width()        { return tabW; }
        public int height()       { return tabH; }
        public void reset()       {  }

        public EmpireTabSprite(MapOverlaySpies p, Empire e)  {
            parent = p;
            empire = e;
            tabW = BasePanel.s100;
            fontSize = 11;
            int n = p.empires.size();
            if (n > 40)
                fontSize = 20;
            else if (n > 36)
                fontSize = 12;
            else if (n > 34)
                fontSize = 13;
            else if (n > 32)
                fontSize = 14;
            else if (n > 30)
                fontSize = 15;
            else if (n > 29)
                fontSize = 16;
            else if (n > 28)
                fontSize = 17;
            else if (n > 26)
                fontSize = 18;
            else if (n > 25)
                fontSize = 19;
            else 
                fontSize = 20;
            tabH = scaled(fontSize+1);
        }
        public void setPosition(int x, int y) {
            tabX = x;
            tabY = y;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= tabX
                        && x <= tabX+tabW
                        && y >= tabY
                        && y <= tabY+tabH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;

            int s5 = BasePanel.s5;
            int cnr = scaled(fontSize/2);
            
            g.setFont(narrowFont(fontSize));
            if (empire == parent.selectedEmpire)
                g.setColor(MainUI.paneBackground);
            else
                g.setColor(MainUI.paneBackgroundDk);
            g.setClip(tabX,tabY,tabW,tabH);
            g.fillRoundRect(tabX-cnr,tabY,tabW+cnr,tabH,cnr,cnr);
            g.setClip(null);
            if (hovering)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.blackText);
            drawString(g,empire.raceName(), tabX+scaled(5), tabY+tabH-scaled(3));
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            //if (click)
            //    softClick();
            parent.selectEmpire(empire);
        };
    } 
    public class CloseButton extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int x, y, w, h;
        private MapOverlaySpies parent;
        private String label;
        Color buttonC = newColor(110,110,110);
        Color gray190C = newColor(190,190,190);

        public void setBounds(int x0, int y0, int w0, int h0) {
            x = x0;
            y = y0;
            w = w0;
            h = h0;
        }
        public int getWidth(Graphics2D g) {
            g.setFont(narrowFont(20));
            return g.getFontMetrics().stringWidth(label)+BasePanel.s20;
        }
        protected int mapX()      { return x; }
        protected int mapY()      { return y; }
        public void mapX(int i)   { x = i; }
        public void mapY(int i)   { y = i; }
        public void reset()       { background = null; }

        public void parent(MapOverlaySpies p)  { 
            parent = p;
            label = text("NOTICE_SPIES_CLOSE");
             background = null;
       }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x0, int y0) {
            hovering = x0 >= x
                        && x0 <= x+w
                        && y0 >= y
                        && y0 <= y+h;

            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D yesStart = new Point2D.Float(x, 0);
                Point2D yesEnd = new Point2D.Float(x+w, 0);
                Color[] yesColors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
            }
            g.setColor(buttonC);
            g.fillRect(x,y, w, h);
            g.setColor(gray190C);
            g.drawRect(x,y, w, h);

            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            g.setFont(narrowFont(20));
            int sw = g.getFontMetrics().stringWidth(label);
            drawString(g,label, x+((w-sw)/2),y+BasePanel.s19);
            if (hovering) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x,y, w, h);
                g.setStroke(prevStroke);
            }

        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            if (click)
                softClick();
            parent.advanceMap();
        }
    }
    public class IntelligenceButton extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int x, y, w, h;
        private MapOverlaySpies parent;
        private String label;
        Color buttonC = newColor(110,110,110);
        Color gray190C = newColor(190,190,190);

        public void setBounds(int x0, int y0, int w0, int h0) {
            x = x0;
            y = y0;
            w = w0;
            h = h0;
        }
        public int getWidth(Graphics2D g) {
            g.setFont(narrowFont(20));
            return g.getFontMetrics().stringWidth(label)+BasePanel.s20;
        }
        protected int mapX()      { return x; }
        protected int mapY()      { return y; }
        public void mapX(int i)   { x = i; }
        public void mapY(int i)   { y = i; }
        public void reset()       { background = null; }

        public void parent(MapOverlaySpies p)  { 
            parent = p;
            label = text("NOTICE_SPIES_MANAGE");
            background = null;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x0, int y0) {
            hovering = x0 >= x
                        && x0 <= x+w
                        && y0 >= y
                        && y0 <= y+h;

            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D yesStart = new Point2D.Float(x, 0);
                Point2D yesEnd = new Point2D.Float(x+w, 0);
                Color[] yesColors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
            }
            g.setColor(buttonC);
            g.fillRect(x,y, w, h);
            g.setColor(gray190C);
            g.drawRect(x,y, w, h);

            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            g.setFont(narrowFont(20));
            int sw = g.getFontMetrics().stringWidth(label);
            drawString(g,label, x+((w-sw)/2),y+BasePanel.s19);
            if (hovering) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x,y, w, h);
                g.setStroke(prevStroke);
            }

        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            if (click)
                softClick();
            manageSpies();
        }
    }
    public class ThreatenButton extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int x, y, w, h;
        private MapOverlaySpies parent;
        private String label;
        Color buttonC = newColor(110,110,110);
        Color gray190C = newColor(190,190,190);

        public void setBounds(int x0, int y0, int w0, int h0) {
            x = x0;
            y = y0;
            w = w0;
            h = h0;
        }
        public int getWidth(Graphics2D g) {
            g.setFont(narrowFont(20));
            return g.getFontMetrics().stringWidth(label)+BasePanel.s20;
        }
        protected int mapX()      { return x; }
        protected int mapY()      { return y; }
        public void mapX(int i)   { x = i; }
        public void mapY(int i)   { y = i; }
        public void reset()       { background = null; }

        public void parent(MapOverlaySpies p)  { 
            parent = p;
            label = text("NOTICE_SPIES_THREATEN");
            background = null;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x0, int y0) {
            hovering = x0 >= x
                        && x0 <= x+w
                        && y0 >= y
                        && y0 <= y+h;

            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D yesStart = new Point2D.Float(x, 0);
                Point2D yesEnd = new Point2D.Float(x+w, 0);
                Color[] yesColors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(yesStart, yesEnd, dist, yesColors);
            }
            g.setColor(buttonC);
            g.fillRect(x,y, w, h);
            g.setColor(gray190C);
            g.drawRect(x,y, w, h);

            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            g.setFont(narrowFont(20));
            int sw = g.getFontMetrics().stringWidth(label);
            drawString(g,label, x+((w-sw)/2),y+BasePanel.s19);
            if (hovering) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x,y, w, h);
                g.setStroke(prevStroke);
            }
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            if (click)
                softClick();
            threaten();
        }
    }
}
