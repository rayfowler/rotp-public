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
import java.awt.Desktop;
import java.awt.Font;
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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.sprites.RoundGradientPaint;
import rotp.util.FontManager;
import rotp.util.ImageManager;
import rotp.util.LanguageManager;
import rotp.util.ThickBevelBorder;

public class GameUI  extends BasePanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    public static String AMBIENCE_KEY = "IntroAmbience";
    protected static RoundGradientPaint rgp;

    public static final int BG_DURATION = 80;
    public static final float SLIDESHOW_MAX = 15f;
    
    public static String gameName = "";
    
    private static final Color langShade[] = { new Color(0,0,0,128), new Color(128,0,0,96) };
    private static final Color menuHover[] = {  new Color(255,220,181), new Color(255,255,210) };
    private static final Color menuDepressed[] = { new Color(156,96,77), new Color(110,110,110) };
    private static final Color menuEnabled[] = { new Color(255,203,133), new Color(197,197,197) };
    private static final Color menuDisabled[] = { new Color(156,96,77), new Color(110,110,110) };
    private static final Color menuShade[] = { new Color(16,10,8,3), new Color(0,0,0,6) };

    private static final Color logoFore[] = { new Color(255,220,181), new Color(240,240,240) };
    private static final Color logoShade[] = { new Color(65,30,24,2), new Color(0,0,0,3) };
    private static final Color setupShade[] = { new Color(65,30,24,128), new Color(40,40,40,128) };
    private static final Color setupFrame[] = { new Color(254,204,153), new Color(195,205,205) };

    private static final Color[] titleColor = { new Color(255,220,181), new Color(240,240,240)  };
    private static final Color[] titleShade = { new Color(25,25,25), new Color(25,25,25) };
    private static final Color[] labelColor = { new Color(79,52,33), new Color(39,44,44) };
    private static final Color[] raceEdgeColor = { new Color(114,75,49), new Color(44,48,47) };
    private static final Color[] raceCenterColor = { new Color(179,117,77), new Color(86,96,95) };
    private static final Color[] borderBrightColor = { new Color(254,204,153), new Color(172,181,181) };
    private static final Color[] borderMidColor = { new Color(179,116,73), new Color(106,121,121) };
    private static final Color[] borderDarkColor = { new Color(79,52,33),  new Color(39,44,44) };
    private static final Color[] paneBackgroundColor = { new Color(240,182,132), new Color(172,181,181) };
    private static final Color[] saveGameBackgroundColor = { new Color(26,17,17), new Color(26,17,17) };
    private static final Color[] buttonBackgroundColor = { new Color(93,61,40), new Color(53,60,60) };
    private static final Color[] buttonTextColor = { new Color(240,240,240), new Color(196,196,196) };
    private static final Color[] disabledTextColor = { new Color(160,160,150), new Color(128,128,128) };
    private static final Color[] textColor = { new Color(246,197,130), new Color(196,196,196) };
    private static final Color[] textHoverColor = { new Color(250,247,140), new Color(253,219,180) };
    private static final Color[] textSelectedColor = { new Color(153,196,153), new Color(151,136,205) };
    private static final Color[] textShade = { new Color(25,25,25), new Color(25,25,25) };
    private static final Color[] loadHiBackgroundColor = { new Color(188,123,81), new Color(91,101,100) };
    private static final Color[] loadHoverBackgroundColor = { new Color(219,167,122), new Color(160,172,170) };
    private static final Color[] loadListMask = { new Color(0,0,0,120), new Color(0,0,0,120) };
    private static final Color[] sortLabelBackColor = { new Color(100,70,50), new Color(59,66,65) };
    private static LinearGradientPaint[] loadBackground;
    private static LinearGradientPaint[] raceLeftBackground;
    private static LinearGradientPaint[] raceRightBackground;
    private static LinearGradientPaint[] buttonLeftBackground;
    private static LinearGradientPaint[] buttonRightBackground;
    private static LinearGradientPaint[] opponentsSetupBackground;
    private static LinearGradientPaint[] galaxySetupBackground;
    private static LinearGradientPaint[] settingsSetupBackground;

    private static Border setupBorder;
    private static Border buttonBorder;
    private static Border saveGameBorder;
    private static Border saveListBorder;
    int fuzz = 8;
    int fuzzSc = 2;
    int diff = s60;
    int languageX;
    BaseText discussText, continueText, newGameText, loadGameText, saveGameText, settingsText, exitText, restartText;
    BaseText versionText, manualText;
    BaseText developerText, artistText, graphicDsnrText, writerText, soundText, translatorText, slideshowText;
    BaseText shrinkText, enlargeText;
    BaseText hoverBox;
    Rectangle languageBox = new Rectangle();
    boolean mouseDepressed = false;
    boolean hideText = false;
    int startingScale = 100;
    String startingDisplayMode;
    public static Image defaultBackground;
    Image backImg1, backImg2;
    BufferedImage titleImg;
    BufferedImage backImg;
    String imageKey1, imageKey2;
    int animationTimer = BG_DURATION;
    private final GameLanguagePane languagePanel;
    float slideshowFade = SLIDESHOW_MAX;
    
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
    public static Color loadHiBackground()        { return loadHiBackgroundColor[opt()]; }
    public static Color loadHoverBackground()     { return loadHoverBackgroundColor[opt()]; }
    public static Color loadListMask()            { return loadListMask[opt()]; }
    public static Color sortLabelBackColor()      { return sortLabelBackColor[opt()]; }
    public static LinearGradientPaint loadBackground() {
        if (loadBackground == null) {
            loadBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(RotPUI.scaledSize(350), 0);
            Point2D end = new Point2D.Float(RotPUI.scaledSize(879), 0);
            float[] dist = {0.0f, 0.1f, 0.9f, 1.0f};
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
    public static LinearGradientPaint settingsSetupBackground(int w) {
        if (settingsSetupBackground == null) {
            settingsSetupBackground = new LinearGradientPaint[2];
            Point2D start = new Point2D.Float(BasePanel.s100, 0);
            Point2D end = new Point2D.Float(w-BasePanel.s100, 0);
            float[] dist = {0.0f, 0.05f, 0.95f, 1.0f};
            Color edge0 = new Color(113,74,49);
            Color mid0 = new Color(188,123,81);
            Color[] colors0 = {edge0, mid0,  mid0, edge0 };
            settingsSetupBackground[0] = new LinearGradientPaint(start, end, dist, colors0);
            Color edge1 = new Color(51,56,55);
            Color mid1 = new Color(100,111,110);
            Color[] colors1 = {edge1, mid1, mid1, edge1 };
            settingsSetupBackground[1] = new LinearGradientPaint(start, end, dist, colors1);
        }
        return settingsSetupBackground[opt()];
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
    private static final String[] backImgKeys = { 
        "LANDSCAPE_RUINS_ORION", "LANDSCAPE_RUINS_ANTARAN", 
        "AlkCouncil", "AlkWin", "AlkLoss", "AlkSab01", "AlkSab02",
        "BulCouncil", "BulWin", "BulLoss", "BulSab01", "BulSab02", 
        "DarCouncil01", "DarWin", "DarLoss", "DarSab01", "DarSab02",
        "HumCouncil", "HumWin", "HumLoss",  "HumSab01", "HumSab02", 
        "KlaCouncil", "KlaWin", "KlaLoss", "KlaSab01", "KlaSab02",
        "MekCouncil", "MekWin", "MekLoss", "MekSab01", "MekSab02", 
        "MrrCouncil", "MrrWin", "MrrLoss", "MrrSab01", "MrrSab02",
        "PsiCouncil", "PsiWin", "PsiLoss", "PsiSab01", "PsiSab02",
        "SakCouncil", "SakWin", "SakLoss", "SakSab01", "SakSab02",
        "SilCouncil", "SilWin", "SilLoss", "SilSab01", "SilSab02",
         };
    public Image background() { return backImg; }
    public static int opt()     {
        return 0;
    }

    public GameUI() {
        startingScale = UserPreferences.screenSizePct();
        startingDisplayMode = UserPreferences.displayMode();
        languagePanel = new GameLanguagePane(this);
        imageKey1 = backImgKeys[0];
        imageKey2 = random(backImgKeys);
        while (imageKey1.equals(imageKey2))
            imageKey2 = random(backImgKeys);
        Color enabledC = menuEnabled[0];
        Color disabledC = menuDisabled[0];
        Color hoverC = logoFore[1];
        Color depressedC = menuDepressed[1];
        Color shadedC = menuShade[1];
        
        int w = getWidth();
        shrinkText      = new BaseText(this, false,20,   10,24,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        enlargeText     = new BaseText(this, false,20,    0,24,  enabledC, disabledC, hoverC, depressedC, shadedC, 0, 0, 0);
        enlargeText.preceder(shrinkText);
        continueText    = new BaseText(this, true, 45,   0, 340,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        newGameText     = new BaseText(this, true, 45,   0, 385,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        loadGameText    = new BaseText(this, true, 45,   0, 430,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        saveGameText    = new BaseText(this, true, 45,   0, 475,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        settingsText    = new BaseText(this, true, 45,   0, 520,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        manualText      = new BaseText(this, true, 45,   0, 565,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        exitText        = new BaseText(this, true, 45,   0, 610,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        restartText     = new BaseText(this, true, 45,   0, 430,  enabledC, disabledC, hoverC, depressedC, shadedC, 1, 1, 8);
        versionText     = new BaseText(this, false,16,   5, -35,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 0, 1);
        discussText     = new BaseText(this, false,22,   5, -10,  enabledC, disabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        developerText   = new BaseText(this, false,16, -210,-95,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        artistText      = new BaseText(this, false,16, -210,-78,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        graphicDsnrText = new BaseText(this, false,16, -210,-61,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        writerText      = new BaseText(this, false,16, -210,-44,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        soundText       = new BaseText(this, false,16, -210,-27,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        translatorText  = new BaseText(this, false,16, -210,-10,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);
        slideshowText   = new BaseText(this, false,16, -210,-10,  enabledC,  enabledC, hoverC, depressedC, Color.black, 1, 1, 1);

        developerText.disabled(true);
        artistText.disabled(true);
        graphicDsnrText.disabled(true);
        writerText.disabled(true);
        soundText.disabled(true);
        translatorText.disabled(true);
        slideshowText.disabled(true);
        versionText.disabled(true);
        developerText.bordered(true);
        artistText.bordered(true);
        graphicDsnrText.bordered(true);
        writerText.bordered(true);
        soundText.bordered(true);
        translatorText.bordered(true);
        slideshowText.bordered(true);
        versionText.bordered(true);
        discussText.bordered(true);
        shrinkText.bordered(true);
        enlargeText.bordered(true);
        setTextValues();
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
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
        settingsText.displayText(text("GAME_MENU_SETTINGS"));
        manualText.displayText(text("GAME_MENU_OPEN_MANUAL"));
        exitText.displayText(text("GAME_MENU_EXIT"));
        restartText.displayText(text("GAME_MENU_RESTART"));

        shrinkText.displayText(text("GAME_SHRINK"));
        enlargeText.displayText(text("GAME_ENLARGE"));
        developerText.displayText(text("CREDITS_DEVELOPER"));
        artistText.displayText(text("CREDITS_ILLUSTRATOR"));
        graphicDsnrText.displayText(text("CREDITS_GRAPHIC_DESIGN"));
        writerText.displayText(text("CREDITS_WRITER"));
        soundText.displayText(text("CREDITS_SOUND"));
        translatorText.displayText(text("CREDITS_TRANSLATOR"));
        slideshowText.displayText(text("CREDITS_ILLUSTRATOR"));
        versionText.displayText(text("GAME_VERSION", str(Rotp.releaseId)));
    }
    public void init() {
        slideshowFade = SLIDESHOW_MAX;
        resetSlideshowTimer();
    }
    @Override
    public void animate() {
        if (glassPane() != null)
            return;
        
        animationTimer--;
        slideshowFade -=.1f;
        if (animationTimer <= 0) {
            imageKey1 = imageKey2;
            imageKey2 = random(backImgKeys);
            backImg1 = backImg2;
            log("getting image: "+imageKey2);
            backImg2 = ImageManager.current().image(imageKey2);
            backImg = newOpaqueImage(backImg1);
            animationTimer = BG_DURATION;
            repaint();
        }
        else if (animationTimer < 20) {
            float pct = bounds(0.0f, (20-animationTimer)/20f, 1.0f);
            BufferedImage img = newOpaqueImage(backImg1);
            Graphics2D imgG = (Graphics2D) img.getGraphics();
            AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pct);
            imgG.setComposite(composite);
            imgG.drawImage(backImg2, 0,0,null);
            imgG.dispose();
            backImg = img;
            repaint();
        }
        else if ((slideshowFade <= 1) && (slideshowFade >= -0.3f))
            repaint();
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
        
        languagePanel.initFonts();

        if (backImg == null) {
            backImg1 =  ImageManager.current().image(imageKey1);
            backImg2 =  ImageManager.current().image(imageKey2);
            backImg = newOpaqueImage(backImg1);
            defaultBackground = backImg;
        }
        Image back = background();
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, getWidth(), getHeight(), 0, 0, imgW, imgH, this);

  
        
        Composite prevComp = g.getComposite();
        float textAlpha = min(1,max(0,slideshowFade));
        if ((textAlpha < 1) || hideText) {
            if (!hideText) {
                AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1-textAlpha);
                g.setComposite(ac);
            }
            slideshowText.draw(g);
        }
 
        if (textAlpha == 0) {
            languagePanel.setVisible(false);
            g.setComposite(prevComp);
            return;
        }
        
        if (textAlpha < 1) {
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,textAlpha);
            g.setComposite(ac);
        }
        
        String titleStr1 = text("GAME_TITLE_LINE_1");
        String titleStr2 = text("GAME_TITLE_LINE_2");
        String titleStr3 = text("GAME_TITLE_LINE_3");

        if (titleImg == null) {
            titleImg = newBufferedImage(getWidth(), scaled(200));
            Graphics2D imgG = (Graphics2D) titleImg.getGraphics();
            setFontHints(imgG);
            int bigFont = scaledLogoFont(imgG, titleStr1+titleStr3, w*3/4, 80, 65);
            int smallFont = scaledLogoFont(imgG, titleStr2, w*3/20, 60, 40);
            imgG.setFont(logoFont(bigFont));
            int sw1a = imgG.getFontMetrics().stringWidth(titleStr1);
            imgG.setFont(logoFont(smallFont));
            int sw1b = imgG.getFontMetrics().stringWidth(titleStr2);
            imgG.setFont(logoFont(bigFont));
            int sw1c = imgG.getFontMetrics().stringWidth(titleStr3);
            int sw1Title = sw1a+sw1b+sw1c+s40;
            int x1Left = (w-sw1Title)/2;
            imgG.setFont(logoFont(bigFont));
            int baseY = scaled(150);
            drawShadowedString(imgG, titleStr1, 1, 0, 10, x1Left, baseY, logoShade[1], logoFore[0]);
            imgG.setFont(logoFont(smallFont));
            drawShadowedString(imgG, titleStr2, 1, 0, 10, x1Left+sw1a+s20, baseY, logoShade[1], logoFore[0]);
            imgG.setFont(logoFont(bigFont));
            drawShadowedString(imgG, titleStr3, 1, 0, 10, x1Left+sw1a+sw1b+s40, baseY, logoShade[1], logoFore[0]);
            imgG.setFont(logoFont(bigFont));
            drawShadowedString(imgG, titleStr1, 1, 0, 10, x1Left, baseY, logoShade[1], logoFore[0]);
            imgG.setFont(logoFont(smallFont));
            drawShadowedString(imgG, titleStr2, 1, 0, 10, x1Left+sw1a+s20, baseY, logoShade[1], logoFore[0]);
            imgG.setFont(logoFont(bigFont));
            imgG.dispose();
        }
        
        if (!hideText)
           g.drawImage(titleImg, 0, s100, null);
        if (hideText) {
            g.setComposite(prevComp);
            return;
        }

        if (languagePanel.fontsReady) {
            int lw = languagePanel.w;
            int lh = languagePanel.h;
            languagePanel.setBounds(w-lw-s15,s5,lw,lh);

            if (languagePanel.isVisible()) {
                g.setColor(langShade());
                g.fillRoundRect(w-s55, s5, s40, s40,s10,s10);
            }
            Image img = image("LANGUAGE_ICON");
            g.drawImage(img, w-s55, s5, s40, s40, this);
            languageBox.setBounds(w-s55, s5, s40, s40);

            String langText = LanguageManager.current().selectedLanguageName();
            g.setFont(narrowFont(24));
            int langSW = g.getFontMetrics().stringWidth(langText);
            int langX = w-s55-langSW-s10;
            g.setColor(logoFore[0]);
            drawShadowedString(g, langText, 2, langX, s30, Color.black, logoFore[0]);
        }

        discussText.disabled(false);
        if (!discussText.isEmpty())
            discussText.draw(g);
        
        if (canOpenManual()) {
            exitText.setY(610);
        }
        else
            exitText.setY(565);

        if (canRestart()) {
            continueText.reset();
            newGameText.reset();
            loadGameText.reset();
            saveGameText.reset();
            settingsText.disabled(false);
            settingsText.drawCentered(g);
            manualText.reset();
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
            settingsText.disabled(false);
            settingsText.drawCentered(g);
            manualText.visible(canOpenManual());
            manualText.drawCentered(g);
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
        versionText.draw(g);
        
        shrinkText.visible(UserPreferences.windowed());
        enlargeText.visible(UserPreferences.windowed());
        shrinkText.draw(g);
        enlargeText.draw(g);
        
        g.setComposite(prevComp);
    }
    private String manualFilePath() {
        return LanguageManager.current().selectedLanguageFullPath()+"/manual.pdf";
    }
    private boolean manualExists() { 
        String filename = manualFilePath();
        return readerExists(filename);
    }
    private boolean canContinue()    { return session().status().inProgress() || session().hasRecentSession(); }
    private boolean canNewGame()     { return true; }
    private boolean canLoadGame()    { return true; }
    private boolean canSaveGame()    { return session().status().inProgress(); }
    private boolean canOpenManual()  { return manualExists(); }
    private boolean canExit()        { return true; }
    private boolean canRestart()     { return !UserPreferences.displayMode().equals(startingDisplayMode) 
            || (UserPreferences.screenSizePct() != startingScale); }

    private void rescaleMenuOptions() {
        restartText.rescale();
        continueText.rescale();
        newGameText.rescale();
        loadGameText.rescale();
        saveGameText.rescale();
        settingsText.rescale();
        manualText.rescale();
        exitText.rescale();
    }
    private void resetSlideshowTimer() {
        if (slideshowFade < 1) {
            slideshowFade = SLIDESHOW_MAX;
            repaint();
        }
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
        resetSlideshowTimer();
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
            case KeyEvent.VK_O:  openManual();     return;
            case KeyEvent.VK_S:  saveGame();     return;
            case KeyEvent.VK_T:  goToSettings();     return;
            case KeyEvent.VK_E:
            case KeyEvent.VK_X:
                exitGame();     return;
            case KeyEvent.VK_ESCAPE:
                if (canContinue())
                    continueGame();
        }
    }
    private void shrinkFrame() {
        if (!UserPreferences.windowed())
            return;
        if (UserPreferences.shrinkFrame()) {
            Rotp.setFrameSize();
            rescaleMenuOptions();
            titleImg = null;
            languagePanel.initBounds();
            UserPreferences.save();
            repaint();
       }
    }
    private void expandFrame() {
        if (!UserPreferences.windowed())
            return;
       if (UserPreferences.expandFrame()) {
            Rotp.setFrameSize();
            rescaleMenuOptions();
            titleImg = null;
            languagePanel.initBounds();
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
    private void openManual() {
        try {
            buttonClick();
            String filename = manualFilePath();
            InputStream manualAsStream = fileInputStream(filename);
            Path tempOutput = Files.createTempFile("ROTP_Manual", ".pdf");
            tempOutput.toFile().deleteOnExit();
            Files.copy(manualAsStream, tempOutput, StandardCopyOption.REPLACE_EXISTING);
            File userManual = new File (tempOutput.toFile().getPath());
            if (userManual.exists()) 
                Desktop.getDesktop().open(userManual);
        } catch (IOException e) {}
    }
    public void continueGame() {
        if (canContinue()) {
            buttonClick();
            if (!session().status().inProgress())
                session().loadRecentSession(true);
            RotPUI.instance().selectMainPanel();
            RotPUI.instance().mainUI().showDisplayPanel();
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
    private void selectLanguage(int i) {
        softClick();
        LanguageManager.current().selectLanguage(i);
        UserPreferences.save();
        setTextValues();
        titleImg = null;
        repaint();
    }
    public void goToSettings() {
        buttonClick();
        GameSettingsUI settingsUI = RotPUI.gameSettingsUI();
        settingsUI.open(this);
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
        resetSlideshowTimer();
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

        if (manualText.contains(x,y))
            openManual();
        else if (discussText.contains(x,y))
            openRedditPage();
        else if (continueText.contains(x,y))
            continueGame();
        else if (newGameText.contains(x,y))
            newGame();
        else if (loadGameText.contains(x,y))
            loadGame();
        else if (saveGameText.contains(x,y))
            saveGame();
        else if (settingsText.contains(x,y))
            goToSettings();
        else if (exitText.contains(x,y))
            exitGame();
        else if (restartText.contains(x,y))
            restartGame();
        else if (shrinkText.contains(x,y))
            shrinkFrame();
        else if (enlargeText.contains(x,y))
            expandFrame();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        resetSlideshowTimer();

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
        else if (manualText.contains(x,y))
            newHover = manualText;
        else if (settingsText.contains(x,y))
            newHover = settingsText;
        else if (canExit() && exitText.contains(x,y))
            newHover = exitText;
        else if (canRestart() && restartText.contains(x,y))
            newHover = restartText;
        else if (shrinkText.contains(x,y))
            newHover = shrinkText;
        else if (enlargeText.contains(x,y))
            newHover = enlargeText;

        if (hoverBox != newHover) {
            if (hoverBox != null) {
                hoverBox.mouseExit();
                repaint(hoverBox.bounds());
            }
            hoverBox = newHover;
            if (hoverBox != null) {
                if (mouseDepressed)
                    hoverBox.mousePressed();
                else
                    hoverBox.mouseEnter();
                repaint(hoverBox.bounds());
            }
        }
    }
    public class GameLanguagePane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        List<String> names;
        List<String> codes;
        public int w;
        public int h;
        boolean fontsInitialized = false;
        boolean fontsReady = false;
        private Rectangle[] lang;
        private Rectangle hoverBox;
        GameUI parent;
        public GameLanguagePane(GameUI ui) {
            parent = ui;
            init();
        }
        private void init() {
            codes = LanguageManager.current().languageCodes();
            names = LanguageManager.current().languageNames();
            initBounds();
            lang = new Rectangle[names.size()];
            for (int i=0;i<lang.length;i++)
                lang[i] = new Rectangle();
            addMouseListener(this);
            addMouseMotionListener(this);
            setOpaque(false);
        }
        void initBounds() {
            w = scaled(100);
            h = s45+(s17*names.size());
        }
        public void initFonts() {
            if (fontsInitialized)
                return;
            
            fontsInitialized = true;
            Thread r1 = new Thread(){
                @Override
                public void run(){
                    renderFonts();
                }
            };
            r1.start();
        }
        private void renderFonts() {
            Graphics g = getGraphics();
            int y0 = 0;
            for (int i=0; i<names.size(); i++) {
                String code = codes.get(i);
                String name = names.get(i);
                Font f = FontManager.current().languageFont(code);
                g.setFont(f);
                g.setColor(Color.white);
                int sw = g.getFontMetrics().stringWidth(name);
                drawString(g,name, w-sw-s5, y0);
            }    
            fontsReady = true;
            g.dispose();
            parent.repaint();
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(langShade());
            int topM = s35;
            int lineH = s17;
            g.fillRoundRect(0,topM,w,h-topM,s10,s10);
            int y0 = topM;
            for (int i=0; i<names.size(); i++) {
                String code = codes.get(i);
                String name = names.get(i);
                Font f = FontManager.current().languageFont(code);
                g.setFont(f);
                Color c0 = hoverBox == lang[i] ? Color.yellow : Color.white;
                g.setColor(c0);
                y0 += lineH;
                int sw = g.getFontMetrics().stringWidth(name);
                drawString(g,name, w-sw-s5, y0);
                lang[i].setBounds(w-sw-s5, y0-lineH, sw+s5, lineH);
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { 
            resetSlideshowTimer();
        }
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
            resetSlideshowTimer();
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

