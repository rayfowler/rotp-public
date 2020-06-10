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

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.sprites.RoundGradientPaint;
import rotp.util.AnimationManager;
import rotp.util.ImageManager;
import rotp.util.LanguageManager;
import rotp.util.sound.SoundManager;
import rotp.util.ThickBevelBorder;

public class GameUI  extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {
    private static final long serialVersionUID = 1L;
    public static String AMBIENCE_KEY = "IntroAmbience";
    protected static RoundGradientPaint rgp;

    public static String gameName = "";
    
    private static final Color langShade[] = { new Color(0,0,0,128), new Color(128,0,0,96) };
    private static final Color menuHover[] = {  new Color(255,220,181), new Color(255,255,210) };
    private static final Color menuDepressed[] = { new Color(156,96,77), new Color(110,110,110) };
    private static final Color menuEnabled[] = { new Color(255,203,133), new Color(197,197,197) };
    private static final Color menuDisabled[] = { new Color(156,96,77), new Color(110,110,110) };
    private static final Color menuShade[] = { new Color(16,10,8,6), new Color(12,12,12,6) };

    private static final Color logoFore[] = { new Color(255,220,181), new Color(255,255,240) };
    private static final Color logoShade[] = { new Color(65,30,24,24), new Color(40,40,40,24) };
    private static final Color setupShade[] = { new Color(65,30,24,128), new Color(40,40,40,128) };
    private static final Color setupFrame[] = { new Color(254,204,153), new Color(195,205,205) };

    private static final Color[] titleColor = { new Color(255,220,181), new Color(255,255,240)  };
    private static final Color[] titleShade = { new Color(25,25,25), new Color(25,25,25) };
    private static final Color[] labelColor = { new Color(79,52,33), new Color(39,44,44) };
    private static final Color[] raceEdgeColor = { new Color(114,75,49), new Color(44,48,47) };
    private static final Color[] raceCenterColor = { new Color(179,117,77), new Color(78,86,85) };
    private static final Color[] borderBrightColor = { new Color(254,204,153), new Color(172,181,181) };
    private static final Color[] borderMidColor = { new Color(179,116,73), new Color(106,121,121) };
    private static final Color[] borderDarkColor = { new Color(79,52,33),  new Color(39,44,44) };
    private static final Color[] paneBackgroundColor = { new Color(240,182,132), new Color(172,181,181) };
    private static final Color[] saveGameBackgroundColor = { new Color(26,17,17), new Color(26,17,17) };
    private static final Color[] buttonBackgroundColor = { new Color(93,61,40), new Color(53,60,60) };
    private static final Color[] buttonTextColor = { new Color(255,255,240), new Color(196,196,196) };
    private static final Color[] disabledTextColor = { new Color(160,160,150), new Color(128,128,128) };
    private static final Color[] textColor = { new Color(246,197,130), new Color(196,196,196) };
    private static final Color[] textHoverColor = { new Color(250,247,140), new Color(253,219,180) };
    private static final Color[] textSelectedColor = { new Color(153,196,153), new Color(151,136,205) };
    private static final Color[] textShade = { new Color(25,25,25), new Color(25,25,25) };
    private static final Color[] loadHoverBackgroundColor = { new Color(219,167,122), new Color(160,172,170) };
    private static final Color[] loadListMask = { new Color(0,0,0,80), new Color(0,0,0,80) };
    private static LinearGradientPaint[] loadBackground;
    private static LinearGradientPaint[] raceLeftBackground;
    private static LinearGradientPaint[] raceRightBackground;
    private static LinearGradientPaint[] buttonLeftBackground;
    private static LinearGradientPaint[] buttonRightBackground;
    private static LinearGradientPaint[] opponentsSetupBackground;
    private static LinearGradientPaint[] galaxySetupBackground;

    private static Border setupBorder;
    private static Border buttonBorder;
    private static Border saveGameBorder;
    private static Border saveListBorder;
    int fuzz = 8;
    int fuzzSc = 2;
    int diff = s60;
    int languageX;
    BaseText discussText, continueText, newGameText, loadGameText, saveGameText, exitText, restartText;
    BaseText soundsText, musicText, graphicsText, texturesText, versionText, memoryText;
    BaseText developerText, artistText, graphicDsnrText, writerText, soundText, translatorText;
    BaseText shrinkText, enlargeText;
    BaseText hoverBox;
    Rectangle languageBox = new Rectangle();
    boolean mouseDepressed = false;
    boolean hideText = false;
    int startingScale = 100;
    private final GameLanguagePane languagePanel = new GameLanguagePane();

    public static Color langShade()               { return langShade[opt()]; }
    public static Color titleColor()              { return titleColor[opt()]; }
    public static Color titleShade()              { return titleShade[opt()]; }
    public static Color setupShade()              { return setupShade[opt()]; }
    public static Color setupFrame()              { return setupFrame[opt()]; }
    public static Color labelColor()              { return labelColor[opt()]; }
    public static Color paneBackgroundColor()     { return paneBackgroundColor[opt()]; }
    public static Color buttonTextColor()         { return buttonTextColor[opt()]; }
    public static Color disabledTextColor()       { return disabledTextColor[opt()]; }
    public static Color textColor()               { return textColor[opt()]; }
    public static Color textHoverColor()          { return textHoverColor[opt()]; }
    public static Color textSelectedColor()       { return textSelectedColor[opt()]; }
    public static Color textShade()               { return textShade[opt()]; }
    public static Color saveGameBackgroundColor() { return saveGameBackgroundColor[opt()]; }
    public static Color raceEdgeColor()           { return raceEdgeColor[opt()]; }
    public static Color raceCenterColor()         { return raceCenterColor[opt()]; }
    public static Color buttonBackgroundColor()   { return buttonBackgroundColor[opt()]; }
    public static Color borderBrightColor()       { return borderBrightColor[opt()]; }
    public static Color borderMidColor()          { return borderMidColor[opt()]; }
    public static Color borderDarkColor()         { return borderDarkColor[opt()]; }
    public static Color loadHoverBackground()     { return loadHoverBackgroundColor[opt()]; }
    public static Color loadListMask()            { return loadListMask[opt()]; }
    public static LinearGradientPaint loadBackground() {
        if (loadBackground == null) {
            loadBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(350), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(879), 0);
            float[] dist = {0.0f, 0.4f, 0.6f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            loadBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(41,44,43);
            Color mid1 = new Color(88,97,96);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            loadBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return loadBackground[opt()];
    }
    public static LinearGradientPaint buttonLeftBackground() {
        if (buttonLeftBackground == null) {
            buttonLeftBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(710), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(930), 0);
            float[] dist = {0.0f, 0.5f, 0.51f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            buttonLeftBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(41,44,43);
            Color mid1 = new Color(88,97,96);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            buttonLeftBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return buttonLeftBackground[opt()];
    }
    public static LinearGradientPaint buttonRightBackground() {
        if (buttonRightBackground == null) {
            buttonRightBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(950), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(1170), 0);
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            buttonRightBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(41,44,43);
            Color mid1 = new Color(88,97,96);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            buttonRightBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return buttonRightBackground[opt()];
    }
    public static LinearGradientPaint raceLeftBackground() {
        if (raceLeftBackground == null) {
            raceLeftBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(220), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(420), 0);
            float[] dist = {0.0f, 0.4f, 0.6f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            raceLeftBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(51,56,55);
            Color mid1 = new Color(100,111,110);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            raceLeftBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return raceLeftBackground[opt()];
    }
    public static LinearGradientPaint raceRightBackground() {
        if (raceRightBackground == null) {
            raceRightBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(815), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(1015), 0);
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            raceRightBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(51,56,55);
            Color mid1 = new Color(100,111,110);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            raceRightBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return raceRightBackground[opt()];
    }
    public static LinearGradientPaint galaxySetupBackground() {
        if (galaxySetupBackground == null) {
            galaxySetupBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(685), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(1150), 0);
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            galaxySetupBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(51,56,55);
            Color mid1 = new Color(100,111,110);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            galaxySetupBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return galaxySetupBackground[opt()];
    }
    public static LinearGradientPaint opponentsSetupBackground() {
        if (opponentsSetupBackground == null) {
            opponentsSetupBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(80), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(585), 0);
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            opponentsSetupBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(51,56,55);
            Color mid1 = new Color(100,111,110);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            opponentsSetupBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return opponentsSetupBackground[opt()];
    }
    public static Border setupBorder() {
        if (setupBorder == null)
            setupBorder = new ThickBevelBorder(12, 1, borderBrightColor(), borderMidColor(), borderDarkColor());
        return setupBorder;
    }
    public static Border saveGameBorder() {
        if (saveGameBorder == null)
            saveGameBorder = new ThickBevelBorder(6, 1, borderBrightColor(), borderMidColor(), borderDarkColor());
        return saveGameBorder;
    }
    public static Border saveListBorder() {
        if (saveListBorder == null)
            saveListBorder = new ThickBevelBorder(12, 1, borderBrightColor(), borderMidColor(), borderDarkColor());
        return saveListBorder;
    }
    public static Border buttonBorder() {
        if (buttonBorder == null)
            buttonBorder = new ThickBevelBorder(6, 1, borderBrightColor(), borderMidColor(), borderDarkColor());
        return buttonBorder;
    }

    private static int opt = -1;
    private static final String[] backImg = { "LANDSCAPE_RUINS_ORION", "LANDSCAPE_RUINS_ANTARAN" };
    public static Image background() { return ImageManager.current().image(backImg[opt()]); }
    public static int opt()     {
        if (opt < 0)
            opt = (int)(backImg.length*Math.random());
        return opt;
    }

    public GameUI() {
        startingScale = UserPreferences.screenSizePct();
        Color enabledC = menuEnabled[opt()];
        Color disabledC = menuDisabled[opt()];
        Color hoverC = menuHover[opt()];
        Color depressedC = menuDepressed[opt()];
        Color shadedC = menuShade[opt()];
        
        int w = getWidth();
        shrinkText      = new BaseText(this, false,20,   10,24,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        enlargeText     = new BaseText(this, false,20,    0,24,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        enlargeText.preceder(shrinkText);
        continueText    = new BaseText(this, true, 50,   0, 300,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        newGameText     = new BaseText(this, true, 50,   0, 360,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        loadGameText    = new BaseText(this, true, 50,   0, 420,  enabledC, disabledC, hoverC, depressedC, shadedC, 2, 1, 8);
        saveGameText    = new BaseText(this, true, 50,   0, 480,  enabledC, disabledC, hoverC, depressedC, shadedC, 2, 1, 8);
        exitText        = new BaseText(this, true, 50,   0, 540,  enabledC, disabledC, hoverC, depressedC, shadedC, 2, 1, 8);
        restartText     = new BaseText(this, true, 50,   0, 420,  enabledC, disabledC, hoverC, depressedC, shadedC, 2, 1, 8);
        versionText     = new BaseText(this, false,16, w/2, -35,  enabledC,  enabledC, hoverC, depressedC, shadedC, 2, 0, 0);
        discussText     = new BaseText(this, false,22, w/2, -10,  enabledC, disabledC, hoverC, depressedC, shadedC, 2, 1, 0);
        soundsText      = new BaseText(this, false,16,   20,-78,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        musicText       = new BaseText(this, false,16,   20,-61,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        graphicsText    = new BaseText(this, false,16,   20,-44,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        texturesText    = new BaseText(this, false,16,   20,-27,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        memoryText      = new BaseText(this, false,16,   20,-10,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        developerText   = new BaseText(this, false,16, -220,-95,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        artistText      = new BaseText(this, false,16, -220,-78,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        graphicDsnrText = new BaseText(this, false,16, -220,-61,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        writerText      = new BaseText(this, false,16, -220,-44,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        soundText       = new BaseText(this, false,16, -220,-27,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        translatorText  = new BaseText(this, false,16, -220,-10,  enabledC,  enabledC, hoverC, depressedC, shadedC, 0, 0, 0);

        developerText.disabled(true);
        artistText.disabled(true);
        graphicDsnrText.disabled(true);
        writerText.disabled(true);
        soundText.disabled(true);
        translatorText.disabled(true);
        versionText.disabled(true);
        setTextValues();
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        languagePanel.setVisible(false);
        add(languagePanel);
    }
    private void setTextValues() {
        discussText.displayText(text("GAME_DISCUSS_ONLINE"));
        continueText.displayText(text("GAME_MENU_CONTINUE"));
        newGameText.displayText(text("GAME_MENU_NEW_GAME"));
        loadGameText.displayText(text("GAME_MENU_LOAD_GAME"));
        saveGameText.displayText(text("GAME_MENU_SAVE_GAME"));
        exitText.displayText(text("GAME_MENU_EXIT"));
        restartText.displayText(text("GAME_MENU_RESTART"));

        soundsText.displayText(soundsStr());
        soundsText.hoverText(soundsHoverStr());
        musicText.displayText(musicStr());
        texturesText.displayText(texturesStr());
        graphicsText.displayText(graphicsLevelStr());
        graphicsText.hoverText(graphicsLevelHoverStr());
        shrinkText.displayText(text("GAME_SHRINK"));
        enlargeText.displayText(text("GAME_ENLARGE"));
        memoryText.displayText(memoryStr());
        developerText.displayText(text("CREDITS_DEVELOPER"));
        artistText.displayText(text("CREDITS_ILLUSTRATOR"));
        graphicDsnrText.displayText(text("CREDITS_GRAPHIC_DESIGN"));
        writerText.displayText(text("CREDITS_WRITER"));
        soundText.displayText(text("CREDITS_SOUND"));
        translatorText.displayText(text("CREDITS_TRANSLATOR"));
        versionText.displayText(text("GAME_VERSION", str(Rotp.releaseId)));
    }
    @Override
    public boolean drawMemory()       { return true; }
    @Override
    public String ambienceSoundKey() { return canContinue() ? super.ambienceSoundKey() : AMBIENCE_KEY; }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth();

        Image back = background();
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, getWidth(), getHeight(), 0, 0, imgW, imgH, this);

        String titleStr1 = text("GAME_TITLE_LINE_1");
        String titleStr2 = text("GAME_TITLE_LINE_2");
        String titleStr3 = text("GAME_TITLE_LINE_3");

        int bigFont = scaledLogoFont(g, titleStr1+titleStr3, w*3/4, 80, 65);
        int smallFont = scaledLogoFont(g, titleStr2, w*3/20, 60, 40);
        g.setFont(logoFont(bigFont));
        int sw1a = g.getFontMetrics().stringWidth(titleStr1);
        g.setFont(logoFont(smallFont));
        int sw1b = g.getFontMetrics().stringWidth(titleStr2);
        g.setFont(logoFont(bigFont));
        int sw1c = g.getFontMetrics().stringWidth(titleStr3);
        int sw1Title = sw1a+sw1b+sw1c+s40;
        int x1Left = (w-sw1Title)/2;
        if (hideText) {
            g.setFont(logoFont(bigFont));
            drawShadowedString(g, titleStr1, 2, 0, 10, (w-sw1a)/2, scaled(250), logoShade[opt()], logoFore[opt()]);
            g.setFont(logoFont(smallFont));
            drawShadowedString(g, titleStr2, 1, 0, 5, (w-sw1b)/2, scaled(330), logoShade[opt()], logoFore[opt()]);
            g.setFont(logoFont(bigFont));
            drawShadowedString(g, titleStr3, 2, 0, 10, (w-sw1c)/2, scaled(430), logoShade[opt()], logoFore[opt()]);
        }
        else {
            g.setFont(logoFont(bigFont));
            drawShadowedString(g, titleStr1, 2, 0, 10, x1Left, scaled(200), logoShade[opt()], logoFore[opt()]);
            g.setFont(logoFont(smallFont));
            drawShadowedString(g, titleStr2, 1, 0, 5, x1Left+sw1a+s20, scaled(200), logoShade[opt()], logoFore[opt()]);
            g.setFont(logoFont(bigFont));
            drawShadowedString(g, titleStr3, 2, 0, 10, x1Left+sw1a+sw1b+s40, scaled(200), logoShade[opt()], logoFore[opt()]);
        }

        if (hideText)
            return;

        int lw = languagePanel.w;
        int lh = languagePanel.h;
        languagePanel.setBounds(w-lw-s15,s15,lw,lh);

        Image img = image("LANGUAGE_ICON");
        g.drawImage(img, w-s55, s15, s40, s40, this);
        languageBox.setBounds(w-s55, s15, s40, s40);

        String langText = LanguageManager.current().selectedLanguageName();
        g.setFont(narrowFont(24));
        int langSW = g.getFontMetrics().stringWidth(langText);
        int langX = w-s55-langSW-s10;
        g.setColor(logoFore[opt()]);
        drawShadowedString(g, langText, 2, langX, s40, logoShade[opt()], logoFore[opt()]);

        discussText.disabled(false);
        if (LanguageManager.current().currentLangDir().equals("en"))
            discussText.drawCentered(g);

        if (canRestart()) {
            continueText.reset();
            newGameText.reset();
            loadGameText.reset();
            saveGameText.reset();
            exitText.reset();
            restartText.disabled(false);
            restartText.drawCentered(g);
        }
        else {
            restartText.reset();
            continueText.disabled(!canContinue());
            continueText.drawCentered(g);
            newGameText.disabled(!canNewGame());
            newGameText.drawCentered(g);
            loadGameText.disabled(!canLoadGame());
            loadGameText.drawCentered(g);
            saveGameText.disabled(!canSaveGame());
            saveGameText.drawCentered(g);
            exitText.disabled(!canExit());
            exitText.drawCentered(g);
        }
        
        // draw version at bottom right, then go up for other vals
        developerText.draw(g);
        artistText.draw(g);
        graphicDsnrText.draw(g);
        writerText.draw(g);
        soundText.draw(g);
        translatorText.draw(g);
        versionText.drawCentered(g);
        memoryText.draw(g);
        texturesText.draw(g);
        shrinkText.draw(g);
        enlargeText.draw(g);
        graphicsText.draw(g);
        musicText.draw(g);
        soundsText.draw(g);
    }
    private boolean canContinue()    { return session().status().inProgress() || session().hasRecentSession(); }
    private boolean canNewGame()     { return true; }
    private boolean canLoadGame()    { return true; }
    private boolean canSaveGame()    { return session().status().inProgress(); }
    private boolean canExit()        { return true; }
    private boolean canRestart()     { return UserPreferences.screenSizePct() != startingScale; }

    private void rescaleMenuOptions() {
        restartText.rescale();
        continueText.rescale();
        newGameText.rescale();
        loadGameText.rescale();
        saveGameText.rescale();
        exitText.rescale();
    }
    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_Z:  hideText = false; repaint(); return;
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_MINUS:
                if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
                    shrinkFrame(); 
                return;
            case KeyEvent.VK_EQUALS: 
                if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
                    expandFrame(); 
                return;
            case KeyEvent.VK_Z:  hideText = true; repaint(); return;
            case KeyEvent.VK_C:  continueGame(); return;
            case KeyEvent.VK_N:  newGame();      return;
            case KeyEvent.VK_L:  loadGame();     return;
            case KeyEvent.VK_S:  saveGame();     return;
            case KeyEvent.VK_E:  exitGame();     return;
            case KeyEvent.VK_ESCAPE:
                if (canContinue())
                    continueGame();
                else
                    exitGame();
        }
    }
    private void shrinkFrame() {
       if (UserPreferences.shrinkFrame()) {
            Rotp.setFrameSize();
            rescaleMenuOptions();
            UserPreferences.save();
            repaint();
       }
    }
    private void expandFrame() {
       if (UserPreferences.expandFrame()) {
            Rotp.setFrameSize();
            rescaleMenuOptions();
            UserPreferences.save();
            repaint();
       }    
    }
    private void openRedditPage() {
        try {
            buttonClick();
            Desktop.getDesktop().browse(new URL("http://www.reddit.com/r/rotp").toURI());
        } catch (IOException | URISyntaxException e) {}
    }
    public void continueGame() {
        if (canContinue()) {
            buttonClick();
            if (!session().status().inProgress()) {
                RotPUI.instance().unregisterOnSession(session());
                session().loadRecentSession(true);
                RotPUI.instance().registerOnSession(session());
            }
            RotPUI.instance().selectMainPanel();
        }
    }
    public void newGame() {
        if (canNewGame()) {
            buttonClick();
            RotPUI.instance().selectSetupRacePanel();
        }
    }
    public void loadGame() {
        if (canLoadGame()) {
            buttonClick();
            RotPUI.instance().selectLoadGamePanel();
        }
    }
    public void saveGame() {
        if (canSaveGame()) {
            buttonClick();
            RotPUI.instance().selectSaveGamePanel();
        }
    }
    public void exitGame() {
        if (canExit()) {
            buttonClick();
            GameSession.instance().exit();
        }
    }
    public void restartGame() {
        Rotp.restart();
    }
    private String texturesStr() {
        if (UserPreferences.textures())
            return text("GAME_TEXTURES_ON")+"     ";
        else
            return text("GAME_TEXTURES_OFF")+"     ";
    }
    private String graphicsLevelStr() {
        UserPreferences.GraphicsSetting graphics = UserPreferences.graphicsLevel();
        switch(graphics) {
            case NORMAL: return text("GAME_GRAPHICS_NORMAL")+"     ";
            case MEDIUM: return text("GAME_GRAPHICS_MEDIUM")+"     ";
            case LOW:    return text("GAME_GRAPHICS_LOW")+"     ";
        }
        return text("GAME_GRAPHICS_NORMAL")+"     ";
    }
    private String graphicsLevelHoverStr() {
        UserPreferences.GraphicsSetting graphics = UserPreferences.graphicsLevel();
        switch(graphics) {
            case NORMAL: return text("GAME_GRAPHICS_NORMAL")+"     ";
            case MEDIUM: return text("GAME_GRAPHICS_MEDIUM_HOVER")+"     ";
            case LOW:    return text("GAME_GRAPHICS_LOW_HOVER")+"     ";
        }
        return graphicsLevelStr();
    }  
    private String soundsStr() {
        if (SoundManager.current().disabled())
            return text("GAME_SOUNDS_DISABLED", "");
        else if (SoundManager.current().playSounds())
            return text("GAME_SOUNDS_ON", str(SoundManager.soundLevel()));
        else
            return text("GAME_SOUNDS_OFF");
    }
    private String soundsHoverStr() {
        if (SoundManager.current().disabled())
            return text("GAME_SOUNDS_DISABLED", SoundManager.errorString);
        else 
            return soundsStr();
    }
    private String musicStr() {
        return SoundManager.current().playMusic() ? text("GAME_MUSIC_ON",str(SoundManager.musicLevel())) : text("GAME_MUSIC_OFF");
    }
    private String memoryStr() {
        return UserPreferences.showMemory() ? text("GAME_MEMORY_SHOW") : text("GAME_MEMORY_HIDE");
    }
    private void selectLanguage(int i) {
        softClick();
        LanguageManager.current().selectLanguage(i);
        UserPreferences.save();
        setTextValues();
        repaint();
    }
    private void toggleMemory() {
        softClick();
        UserPreferences.toggleMemory();
        memoryText.repaint(memoryStr());
        repaint();
    }
    private void scrollSounds(boolean up) {
        if (up)
            SoundManager.current().increaseSoundLevel();
        else
            SoundManager.current().decreaseSoundLevel();
        soundsText.repaint(soundsStr(), soundsHoverStr());
    }
    private void toggleSounds() {
        softClick();
        SoundManager.current().toggleSounds();
        soundsText.repaint(soundsStr(), soundsHoverStr());
    }
    private void scrollMusic(boolean up) {
        if (up)
            SoundManager.current().increaseMusicLevel();
        else
            SoundManager.current().decreaseMusicLevel();
        musicText.repaint(musicStr());
    }
    private void toggleMusic() {
        softClick();
        SoundManager.current().toggleMusic();
        musicText.repaint(musicStr());
    }
    private void toggleTextures() {
        softClick();
        UserPreferences.toggleTextures();
        texturesText.repaint(texturesStr());
    }
    private void toggleGraphicsLevel() {
        if (AnimationManager.current().animationsDisabled())
            misClick();
        else {
            softClick();
            UserPreferences.toggleGraphicsLevel();
            graphicsText.repaint(graphicsLevelStr(), graphicsLevelHoverStr());
            repaint();
        }
    }
    @Override
    public void playAmbience() {
        // in case playing ambience causes a sound error
        super.playAmbience();
        setTextValues();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {
        mouseDepressed = true;
        if (hoverBox != null)
            hoverBox.mousePressed();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();
        if (hoverBox != null)
            hoverBox.mouseReleased();
        mouseDepressed = false;

        if (discussText.contains(x,y))
            openRedditPage();
        else if (continueText.contains(x,y))
            continueGame();
        else if (newGameText.contains(x,y))
            newGame();
        else if (loadGameText.contains(x,y))
            loadGame();
        else if (saveGameText.contains(x,y))
            saveGame();
        else if (exitText.contains(x,y))
            exitGame();
        else if (restartText.contains(x,y))
            restartGame();
        else if (soundsText.contains(x,y))
            toggleSounds();
        else if (musicText.contains(x,y))
            toggleMusic();
        else if (texturesText.contains(x,y))
            toggleTextures();
        else if (graphicsText.contains(x,y))
            toggleGraphicsLevel();
        else if (shrinkText.contains(x,y))
            shrinkFrame();
        else if (enlargeText.contains(x,y))
            expandFrame();
        else if (memoryText.contains(x,y))
            toggleMemory();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (hideText)
            return;

        BaseText newHover = null;
        if (languageBox.contains(x,y))
            languagePanel.setVisible(true);
        else if (discussText.contains(x,y))
            newHover = discussText;
        else if (canContinue() && continueText.contains(x,y))
            newHover = continueText;
        else if (canNewGame() && newGameText.contains(x,y))
            newHover = newGameText;
        else if (canLoadGame() && loadGameText.contains(x,y))
            newHover = loadGameText;
        else if (canSaveGame() && saveGameText.contains(x,y))
            newHover = saveGameText;
        else if (canExit() && exitText.contains(x,y))
            newHover = exitText;
        else if (canRestart() && restartText.contains(x,y))
            newHover = restartText;
        else if (soundsText.contains(x,y))
            newHover = soundsText;
        else if (musicText.contains(x,y))
            newHover = musicText;
        else if (texturesText.contains(x,y))
            newHover = texturesText;
        else if (graphicsText.contains(x,y))
            newHover = graphicsText;
        else if (shrinkText.contains(x,y))
            newHover = shrinkText;
        else if (enlargeText.contains(x,y))
            newHover = enlargeText;
        else if (memoryText.contains(x,y))
            newHover = memoryText;

        if (hoverBox != newHover) {
            if (hoverBox != null)
                hoverBox.mouseExit();
            hoverBox = newHover;
            if (hoverBox != null) {
                if (mouseDepressed)
                    hoverBox.mousePressed();
                else
                    hoverBox.mouseEnter();
            }
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean up = e.getWheelRotation() < 0;
        if (hoverBox == soundsText)
            scrollSounds(up);
        else if (hoverBox == musicText)
            scrollMusic(up);
    }
    public class GameLanguagePane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        List<String> names;
        public int w;
        public int h;
        private Rectangle[] lang;
        private Rectangle hoverBox;
        public GameLanguagePane() {
            init();
        }
        private void init() {
            names = LanguageManager.current().languageNames();
            w = scaled(100);
            h = s45+(s20*names.size());
            lang = new Rectangle[names.size()];
            for (int i=0;i<lang.length;i++)
                lang[i] = new Rectangle();
            addMouseListener(this);
            addMouseMotionListener(this);
            setOpaque(false);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(langShade());
            int topM = s35;
            int lineH = s20;
            g.fillRoundRect(0,topM,w,h-topM,s10,s10);
            int y0 = topM;
            g.setFont(narrowFont(18));
            for (int i=0; i<names.size(); i++) {
                String name = names.get(i);
                Color c0 = hoverBox == lang[i] ? Color.yellow : Color.white;
                g.setColor(c0);
                y0 += lineH;
                int sw = g.getFontMetrics().stringWidth(name);
                g.drawString(name, w-sw-s10, y0);
                lang[i].setBounds(w-sw-s10, y0-lineH, sw+s5, lineH);
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            for (int i=0;i<lang.length;i++) {
                if (hoverBox == lang[i]) {
                    selectLanguage(i);
                    break;
                }
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            hoverBox = null;
            setVisible(false);
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            hoverBox = null;
            for (Rectangle box: lang) {
                if (box.contains(x,y)) {
                    hoverBox = box;
                    break;
                }
            }

            if (hoverBox != prevHover)
                repaint();
        }
    }
}

