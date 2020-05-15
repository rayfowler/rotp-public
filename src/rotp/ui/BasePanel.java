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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.galaxy.StarType;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.util.Base;
import rotp.util.ThickBevelBorder;

public class BasePanel extends JPanel implements Base {
    private static final long serialVersionUID = 1L;
    public static final String TEXTURE_GRAY = "TEXTURE_GRAY";
    public static final String TEXTURE_BROWN = "TEXTURE_BROWN";

    protected static GraphicsConfiguration gc;
    public static final Color hoverC = Color.yellow;
    public static final Color depressedC = new Color(208,160,0);

    public static Color buttonLighter = new Color(192,192,192);
    public static Color buttonLight = new Color(156,156,156);
    public static Color buttonColor = new Color(123,123,123);
    public static Color buttonDark = new Color(83,83,83);
    public static Color buttonDarker = new Color(63,63,63);
    private static Border buttonBevelBorder;
    static final Color greenText = Color.green;
    static final Color greenBackground = new Color(0, 128, 0, 128);

    static Image textureGray, textureBrown;

    static Color borderLight0 = new Color(169,127,99);
    static Color borderLight1 = new Color(151,112,90);
    static Color borderShade0 = new Color(85,64,47);
    static Color borderShade1 = new Color(61,48,28);
    static Color backShade = new Color(0,0,0,128);
    static Border shadedBorder;

    protected BufferedImage starBackground;
    protected int starScrollX = 0;
    protected Image screenBuffer;

    public static GraphicsConfiguration gc() {
        if (gc == null)
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        return gc;
    }
    //public void setDefaultCursor()         { setCursor(cursors().defaultCursor()); }
    //public void setCrosshairsCursor()      { setCursor(cursors().crosshairs()); }
    //public void setScrapCursor()           { setCursor(cursors().scrap()); }
    //public void setToCursor()              { setCursor(cursors().to()); }
    //public void setPromptCursor()          { setCursor(cursors().prompt()); }
    public void setDefaultCursor()         {  }
    public void setCrosshairsCursor()      {  }
    public void setScrapCursor()           {  }
    public void setToCursor()              { }
    public void setPromptCursor()          {  }

    public boolean hasStarBackground()     { return false; }
    public final boolean hasTexture()      { return textureName() != null; }
    public boolean isAlpha()               { return false; }
    public String textureName()            { return null; }
    public Shape textureClip()             { return null; }
    public Area textureArea()              { return null; }
    public boolean drawMemory()            { return false; }
    public int minStarDist()               { return 50; }
    public int varStarDist()               { return 100; }

    public Color starBackgroundC()         { return Color.black; }
    public Border buttonBevelBorder() {
        if (buttonBevelBorder == null)
            buttonBevelBorder =  new ThickBevelBorder(4, 1, buttonLighter, buttonLight, buttonDarker, buttonDark, buttonDark, buttonDarker, buttonLight, buttonLighter);
        return buttonBevelBorder;
    }
    public Border shadedBorder() {
        if (shadedBorder == null)
            shadedBorder = new ThickBevelBorder(5, borderShade0, borderShade1, borderShade0, borderShade1, borderLight0, borderLight1, borderLight0, borderLight1);
        return shadedBorder;
    }
    protected Image screenBuffer() {
        if (screenBuffer == null)
            screenBuffer = newOpaqueImage(getWidth(), getHeight());
        return screenBuffer;
    }
    protected void clearBuffer() {
        screenBuffer = null;
    }

    public boolean useNullClick(int cnt, boolean right) { return false; }
    public boolean useClickedSprite(Sprite o, int count, boolean rightClick)   { return false; }
    public boolean useHoveringSprite(Sprite o)             { return false; }

    public JFrame frame()                  { return (JFrame) SwingUtilities.getRoot(RotPUI.instance()); }
    public void enableGlassPane(BasePanel p) {
        p.setVisible(false);
        frame().setGlassPane(p);
        p.setVisible(true);
    }
    public void disableGlassPane()  { frame().getGlassPane().setVisible(false); }
    
    public void showError(String s) {
        disableGlassPane();
        ErrorDialogPanel err = new ErrorDialogPanel(s);
        enableGlassPane(err);
    }
    public void cancel()   { }
    public void handleNextTurn()    { }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (hasTexture())
            drawTexture(g);
        if (isAlpha()) 
            drawAlpha(g);
        if (drawMemory())
            drawMemory(g);
    }
    protected void jPanelPaintComponent(Graphics g) {
        setFontHints(g);
        super.paintComponent(g);
    }
    protected void jPanelPaint(Graphics g) {
        super.paint(g);
    }
    public BasePanel glassPane() {
        if (frame() != null) {
            Component pane = frame().getGlassPane();
            if ((pane instanceof BasePanel) && pane.isVisible())
                return (BasePanel)frame().getGlassPane();
        }
        return null;
    }
    @Override
    public void paintComponent(Graphics g) {
        setFontHints(g);
        if (hasStarBackground())
            setBackground(starBackgroundC());

        super.paintComponent(g);

        if (hasStarBackground())
            drawStars(g);
    }
    protected void setFPS(int fps) {
        RotPUI.fps(fps);
    }
    protected void resetFPS() {
        setFPS(10);
    }
    protected void drawStars(Graphics g) {
        drawStars(g, getWidth(), getHeight());
    }
    protected void  drawStars(Graphics g, int w, int h) {
        BufferedImage stars = starBackground(w, h);
        int scroll = starScrollX % w;
        if (scroll == 0)
            g.drawImage(stars, 0, 0, w, h, 0, 0, w, h, null);
        else {
            //g.drawImage(stars, 0, 0, w, h, 0, 0, w, h, null);
            g.drawImage(stars, 0, 0, w-scroll, h, scroll, 0, w, h, null);
            g.drawImage(stars, w-scroll, 0, w, h, 0, 0, scroll, h, null);
        }
    }
    public void drawNextTurnNotice(Graphics g) {
        // sets a global flag to true so that the parent UI will draw
        // the notice on its next paint
        if (session().performingTurn())
            RotPUI.drawNextTurnNotice = true;
    }

    protected BufferedImage newStarBackground() {
        initializeStarBackgroundImage(this,getWidth(),getHeight());
        return starBackground;
    }
    protected BufferedImage starBackground() {
        return starBackground == null ? newStarBackground() : starBackground;
    }
    protected BufferedImage starBackground(int w, int h) {
        if (starBackground == null)
            initializeStarBackgroundImage(this,w,h);
        return starBackground;
    }
    public void actionPerformed(ActionEvent e) { }

    public void animate() { }
    public void drawStar(Graphics2D g2, StarType sType, int r, int x0, int y0) {
        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver);
        BufferedImage img = sType.image(r,0);
        int w = img.getWidth();
        //g2.drawImage(img,x0-(w/2),y0-(w/2),null);
        g2.drawImage(img,x0-r,y0-r,x0+r,y0+r,0,0,w,w,null);
        g2.setComposite(prev);
    }
    public void initializeStarBackgroundImage(JPanel obs, int w, int h) {
        starBackground =  new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        drawBackgroundStars(starBackground, obs, minStarDist(), varStarDist());
    }
    public BufferedImage newStarBackground(JPanel obs, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        drawBackgroundStars(img, obs, minStarDist(), varStarDist());
        return img;
    }
    public void drawSkipText(Graphics g, boolean clickContinue) {
        int y0 = getHeight()-s8;
        drawSkipText(g, y0, clickContinue);
    }
    public void drawSkipText(Graphics g, int y0, boolean clickContinue) {
        String skipText = clickContinue ? text("CLICK_CONTINUE") : text("CLICK_SKIP");
        g.setFont(narrowFont(20));
        int sw = g.getFontMetrics().stringWidth(skipText);
        int x0 = (getWidth()-sw)/2;
        drawBorderedString(g, skipText, x0, y0, Color.black, Color.lightGray);
    }
    public void drawNotice(Graphics g, int fontSize) {
        drawNotice(g, fontSize, 0);
    }
    public void drawNotice(Graphics g, int fontSize, int yAdj) {
        int w = getWidth();
        int h = getHeight();
        int bdrW = s7;

        g.setColor(backShade);
        g.fillRect(0,0,getWidth(), getHeight());

        String title = NoticeMessage.title();
        String step = NoticeMessage.step();
        int fontSize2 = fontSize*4/5;

        g.setFont(narrowFont(fontSize));
        int sw1 = g.getFontMetrics().stringWidth(title);
        g.setFont(narrowFont(fontSize2));
        int sw2 = g.getFontMetrics().stringWidth(step);
        int sw = max(sw1,sw2);
        int noticeW = sw+s60;
        int noticeH = step.isEmpty() ? scaled(6+fontSize)+bdrW+bdrW : scaled(6+(fontSize*7/4))+bdrW+bdrW;

        int x = (w-sw)/2;
        int y = (h+yAdj)/2;
        g.setColor(MainUI.paneShadeC);
        g.fillRect(x-bdrW, y-bdrW, noticeW+bdrW+bdrW, noticeH+bdrW+bdrW);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x, y, noticeW, noticeH);

        g.setFont(narrowFont(fontSize));
        int y1 = y+scaled(fontSize)+bdrW-s5;
        drawShadowedString(g, title, 4, x+((noticeW-sw1)/2), y1, SystemPanel.textShadowC, SystemPanel.whiteText);
        if (!step.isEmpty()) {
            int y2 = y+noticeH-bdrW-s6;
            g.setFont(narrowFont(fontSize2));
            drawShadowedString(g, step, 4, x+((noticeW-sw2)/2), y2, SystemPanel.textShadowC, SystemPanel.whiteText);
        }
    }
    public void redrawMemory() {
        repaint(getWidth()-s100,getHeight()-s50,s100,s50);
    }
    public boolean displayMemory() {
        return UserPreferences.showMemory();
    }
    public void drawMemory(Graphics g) {
        if (!displayMemory())
            return;
        long max = Runtime.getRuntime().maxMemory() / 1048576;
        long total = Runtime.getRuntime().totalMemory() / 1048576;
        long free = Runtime.getRuntime().freeMemory() / 1048576;
        long used = total - free;
        if (used > Rotp.maxUsedMemory)
            Rotp.maxUsedMemory = used;

        int threads = Thread.activeCount();

        g.setColor(Color.white);
        g.setFont(narrowFont(14));
        String s = concat(str(total-free), "M / ", str(total), "M / ", str(max), "M  (", str(Rotp.maxUsedMemory), ")");
        if (threads >= 15)
            s = concat(s, " T:", str(threads));
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, getWidth()-sw-s5, getHeight()-s5);
    }
    public void drawAlpha(Graphics g) {
        g.setFont(narrowFont(99));
        g.setColor(new Color(255,255,255,48));
        g.drawString("ALPHA", getWidth()/3, getHeight()/2-s30);         
    }
    public void drawTexture(Graphics g0) {
        drawTexture(g0, 0, 0, getWidth(), getHeight());
    }
    
    public void drawTextureWithExistingClip(Graphics g0, int x, int y, int w, int h) {
       if (!UserPreferences.textures())
            return;
        float pct = UserPreferences.uiTexturePct();
        if (pct <= 0)
            return;
        if (pct > 1)
            pct = 1;
        Graphics2D g = (Graphics2D) g0;
        Image texture = null;
        switch(textureName()) {
            case TEXTURE_BROWN:
                if (textureBrown == null)
                    textureBrown = image(TEXTURE_BROWN);
                texture = textureBrown;
                break;
            case TEXTURE_GRAY:
                if (textureGray == null)
                    textureGray = image(TEXTURE_GRAY);
                texture = textureGray;
        }
        if (texture == null)
            return;

        int imgW = texture.getWidth(null);
        int imgH = texture.getHeight(null);

        Composite prevComposite = g.getComposite();
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pct);
        g.setComposite(ac);
        g.drawImage(texture, x, y, x+w, y+h, 0, 0, min(x+w,imgW), min(imgH,y+h), this);
        g.setComposite(prevComposite);        
    }
    public void drawTexture(Graphics g0, int x, int y, int w, int h) {
        Shape clip = (textureArea() != null) ? textureArea() : textureClip(); 
        g0.setClip(clip);
        drawTextureWithExistingClip(g0, x,y,w,h);
        g0.setClip(null);
    }
    public void drawTexture(Graphics g0, Shape clip, int x, int y, int w, int h) {
        g0.setClip(clip);
        drawTextureWithExistingClip(g0, x,y,w,h);
        g0.setClip(null);
    }

    public static int  s1,  s2,  s3,  s4,  s5,  s6,  s7,  s8,  s9, s10;
    public static int s11, s12, s13, s14, s15, s16, s17, s18, s19, s20;
    public static int s21, s22, s23, s24, s25, s26, s27, s28, s29, s30;
    public static int s31, s32, s33, s34, s35, s36, s37, s38, s39, s40;
    public static int s41, s42, s43, s44, s45, s46, s47, s48, s49, s50;
    public static int s51, s52, s53, s54, s55, s56, s57, s58, s59, s60;
    public static int s61, s62, s63, s64, s65, s66, s67, s68, s69, s70;
    public static int s71, s72, s73, s74, s75, s76, s77, s78, s79, s80;
    public static int s81, s82, s83, s84, s85, s86, s87, s88, s89, s90;
    public static int s91, s92, s93, s94, s95, s96, s97, s98, s99, s100;

    public static Stroke stroke1, stroke2, stroke3, stroke4, stroke5, stroke6, stroke7;

    public void loadScaledIntegers() {
        s1 = scaled(1); s2 = scaled(2); s3 = scaled(3); s4 = scaled(4); s5 = scaled(5); s6 = scaled(6); s7 = scaled(7); s8 = scaled(8); s9 = scaled(9); s10 = scaled(10);
        s11 = scaled(11); s12 = scaled(12); s13 = scaled(13); s14 = scaled(14); s15 = scaled(15); s16 = scaled(16); s17 = scaled(17); s18 = scaled(18); s19 = scaled(19); s20 = scaled(20);
        s21 = scaled(21); s22 = scaled(22); s23 = scaled(23); s24 = scaled(24); s25 = scaled(25); s26 = scaled(26); s27 = scaled(27); s28 = scaled(28); s29 = scaled(29); s30 = scaled(30);
        s31 = scaled(31); s32 = scaled(32); s33 = scaled(33); s34 = scaled(34); s35 = scaled(35); s36 = scaled(36); s37 = scaled(37); s38 = scaled(38); s39 = scaled(39); s40 = scaled(40);
        s41 = scaled(41); s42 = scaled(42); s43 = scaled(43); s44 = scaled(44); s45 = scaled(45); s46 = scaled(46); s47 = scaled(47); s48 = scaled(48); s49 = scaled(49); s50 = scaled(50);
        s51 = scaled(51); s52 = scaled(52); s53 = scaled(53); s54 = scaled(54); s55 = scaled(55); s56 = scaled(56); s57 = scaled(57); s58 = scaled(58); s59 = scaled(59); s60 = scaled(60);
        s61 = scaled(61); s62 = scaled(62); s63 = scaled(63); s64 = scaled(64); s65 = scaled(65); s66 = scaled(66); s67 = scaled(67); s68 = scaled(68); s69 = scaled(69); s70 = scaled(70);
        s71 = scaled(71); s72 = scaled(72); s73 = scaled(73); s74 = scaled(74); s75 = scaled(75); s76 = scaled(76); s77 = scaled(77); s78 = scaled(78); s79 = scaled(79); s80 = scaled(80);
        s81 = scaled(81); s82 = scaled(82); s83 = scaled(83); s84 = scaled(84); s85 = scaled(85); s86 = scaled(86); s87 = scaled(87); s88 = scaled(88); s89 = scaled(89); s90 = scaled(90);
        s91 = scaled(91); s92 = scaled(92); s93 = scaled(93); s94 = scaled(94); s95 = scaled(95); s96 = scaled(96); s97 = scaled(97); s98 = scaled(98); s99 = scaled(99); s100 = scaled(100);

        stroke1 = new BasicStroke(s1);
        stroke2 = new BasicStroke(s2);
        stroke3 = new BasicStroke(s3);
        stroke4 = new BasicStroke(s4);
        stroke5 = new BasicStroke(s5);
        stroke6 = new BasicStroke(s6);
        stroke7 = new BasicStroke(s7);
    }
    public static Stroke baseStroke(int n) {
        switch(n) {
            case 1: return stroke1;
            case 2: return stroke2;
            case 3: return stroke3;
            case 4: return stroke4;
            case 5: return stroke5;
            case 6: return stroke6;
            case 7: return stroke7;
            default: return stroke1;
        }
    }

    // used for keyEvents sent from RotPUI
    public void keyPressed(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }
    public void keyTyped(KeyEvent e) { }
    public void playAmbience() {
            playAmbience(ambienceSoundKey());
    }
    public String defaultAmbience(){
        if (galaxy() == null)
            return "IntroAmbience";
        if (galaxy().council().finalWar())
            return "FinalWarAmbience"; 
        if (player().atWar())
            return "WarAmbience"; 
        else if (player().hasAnyContact())
            return "ContactAmbience";
        else
            return "ExploreAmbience";        
    }
    public String ambienceSoundKey() { 
        return defaultAmbience();
    }
}