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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;
import rotp.Rotp;

import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.NoticeMessage;
import rotp.ui.RotPUI;
import rotp.ui.main.SystemPanel;

public final class LoadGameUI  extends BasePanel implements MouseListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static final int  MAX_FILES = 10;
    private static final SimpleDateFormat fileDateFmt = new SimpleDateFormat("MMM dd, HH:mm");
    static LoadGameUI current;

    LoadListingPanel listingPanel;
    List<String> saveFiles = new ArrayList<>();
    List<String> saveDates = new ArrayList<>();
    String selectedFile = "";
    Rectangle hoverBox;
    Rectangle selectBox;
    int selectIndex;
    int start = 0;
    int end = 0;

    int buttonW, button1X, button2X;

    boolean hasAutosave = false;
    boolean loading = false;
    private final Rectangle cancelBox = new Rectangle();
    private final Rectangle loadBox = new Rectangle();
    private LinearGradientPaint[] loadBackC;
    private LinearGradientPaint[] cancelBackC;

    public LoadGameUI() {
        current = this;
        initModel();
    }
    public void init() {
        saveFiles.clear();
        saveDates.clear();
        hoverBox = null;
        selectBox = null;
        selectIndex = -1;
        start = 0;
        end = 0;
        selectedFile = "";
        hasAutosave = false;
        loading = false;

        String ext = GameSession.SAVEFILE_EXTENSION;

        // check for autosave
        File curDir = new File(Rotp.jarPath());
        File autoSave = new File(curDir, GameSession.RECENT_SAVEFILE);
        if (autoSave.isFile()) {
            hasAutosave = true;
            saveFiles.add(text("LOAD_GAME_AUTOSAVE"));
            saveDates.add(fileDateFmt.format(autoSave.lastModified()));
        }

        File[] filesList = curDir.listFiles();
        Arrays.sort(filesList, FILE_DATE);
        for (File f : filesList){
            if (f.isFile()) {
                String name = f.getName();
                if (name.endsWith(ext)
                && !name.equalsIgnoreCase(GameSession.RECENT_SAVEFILE)) {
                    List<String> parts = substrings(name, '.');
                    if (!parts.get(0).trim().isEmpty()) {
                        saveFiles.add(name.substring(0, name.length()-ext.length()));
                        saveDates.add(fileDateFmt.format(f.lastModified()));
                    }
                }
            }
        }
        if (!saveDates.isEmpty()) {
            selectIndex = 0;
            if (hasAutosave)
                selectedFile = GameSession.RECENT_SAVEFILE;
            else
                selectedFile = saveFiles.get(start+selectIndex)+GameSession.SAVEFILE_EXTENSION;
        }
    }
    private String fileBaseName(String fn) {
        String ext = GameSession.SAVEFILE_EXTENSION;
        if (fn.endsWith(ext)
        && !fn.equalsIgnoreCase(GameSession.RECENT_SAVEFILE)) {
            List<String> parts = substrings(fn, '.');
            if (!parts.get(0).trim().isEmpty()) 
                return fn.substring(0, fn.length()-ext.length());
        }
        return "";
    }
    private void initGradients() {
        int w = getWidth();
        buttonW = s100+s100;
        button1X = (w/2)-s10-buttonW;
        button2X = (w/2)+s10;
        Point2D start1 = new Point2D.Float(button1X, 0);
        Point2D end1 = new Point2D.Float(button1X+buttonW, 0);
        Point2D start2 = new Point2D.Float(button2X, 0);
        Point2D end2 = new Point2D.Float(button2X+buttonW, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color brownEdgeC = new Color(100,70,50);
        Color brownMidC = new Color(161,110,76);
        Color[] brownColors = {brownEdgeC, brownMidC, brownEdgeC };

        Color grayEdgeC = new Color(59,66,65);
        Color grayMidC = new Color(107,118,117);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        loadBackC = new LinearGradientPaint[2];
        cancelBackC = new LinearGradientPaint[2];

        loadBackC[0] = new LinearGradientPaint(start1, end1, dist, brownColors);
        cancelBackC[0] = new LinearGradientPaint(start2, end2, dist, brownColors);
        loadBackC[1] = new LinearGradientPaint(start1, end1, dist, grayColors);
        cancelBackC[1] = new LinearGradientPaint(start2, end2, dist, grayColors);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (loadBackC == null)
            initGradients();
        Image back = GameUI.background();
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, getWidth(), getHeight(), 0, 0, imgW, imgH, this);
    }
    private void initModel() {
        addMouseWheelListener(this);
        listingPanel = new LoadListingPanel();
        setLayout(new BorderLayout());
        add(listingPanel, BorderLayout.CENTER);
    }
    private void scrollDown() {
        int prevStart = start;
        int prevSelect = selectIndex;
        start = max(0, min(start+1, saveFiles.size()-MAX_FILES));
        if ((start == prevStart) && (selectIndex >= 0))
            selectIndex = min(selectIndex+1, saveFiles.size()-1, MAX_FILES-1);
        selectedFile = saveFiles.get(start+selectIndex)+GameSession.SAVEFILE_EXTENSION;
        if ((prevStart != start) || (prevSelect != selectIndex))
            repaint();
    }
    private void scrollUp() {
        int prevStart = start;
        int prevSelect = selectIndex;
        start = max(start-1, 0);
        if ((start == prevStart) && (selectIndex >= 0))
            selectIndex = max(selectIndex-1, 0);
        selectedFile = saveFiles.get(start+selectIndex)+GameSession.SAVEFILE_EXTENSION;
        if ((prevStart != start) || (prevSelect != selectIndex))
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        if (count < 0)
            scrollUp();
        else
            scrollDown();
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        RotPUI.instance().selectMainPanel(false);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_DOWN:  scrollDown(); return;
            case KeyEvent.VK_UP:    scrollUp();   return;
            case KeyEvent.VK_L:
            case KeyEvent.VK_ENTER:
                if (canSelect())
                    loadGame(selectedFile);
                return;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_C:    cancelLoad();      return;
        }
    }
    private boolean canSelect()    { return selectIndex >= 0; }
    private boolean canLoad()      { return !selectedFile.isEmpty(); }
    public void loadRecentGame() {
        loading = true;
        repaint();
        buttonClick();
        final Runnable load = () -> {
            RotPUI.instance().unregisterOnSession(session());
            GameSession.instance().loadRecentSession(false);
            RotPUI.instance().registerOnSession(session());
        };
        SwingUtilities.invokeLater(load);
    }
    public void loadGame(String s) {
        if (!canLoad())
            return;
        loading = true;
        GameUI.gameName = fileBaseName(s);
        repaint();
        buttonClick();
        final Runnable load = () -> {
            RotPUI.instance().unregisterOnSession(session());
            GameSession.instance().loadSession(s, false);
            RotPUI.instance().registerOnSession(session());
        };
        SwingUtilities.invokeLater(load);
    }
    public void cancelLoad() {
        buttonClick();
        RotPUI.instance().selectGamePanel();
    }
    class LoadListingPanel extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final Rectangle[] gameBox = new Rectangle[MAX_FILES];
        private final Rectangle listBox = new Rectangle();
        private boolean dragging = false;
        private int lastMouseY;
        private int yOffset = 0;
        private int lineH = s50;
        public LoadListingPanel() {
            init();
        }
        private void init() {
            setOpaque(false);
            for (int i=0;i<gameBox.length;i++)
                gameBox[i] = new Rectangle();
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            for (int i=0;i<gameBox.length;i++)
                gameBox[i].setBounds(0,0,0,0);

            int w = getWidth();
            lineH = s30;

            String title = text("LOAD_GAME_TITLE");
            g.setFont(font(60));
            int sw = g.getFontMetrics().stringWidth(title);
            drawShadowedString(g, title, 1, 3, (w-sw)/2, scaled(140), GameUI.titleShade(), GameUI.titleColor());

            end = min(saveFiles.size(), start+MAX_FILES);
            g.setFont(narrowFont(24));

            int w0 = getWidth()-scaled(700);
            int x0 = scaled(350);
            int h0 = s5+(MAX_FILES*lineH);
            int y0 = scaled(180);

            // draw back mask
            int wTop = s10;
            int wSide = s50;
            int wBottom = s80;
            g.setColor(GameUI.loadListMask());
            g.fillRect(x0-wSide, y0-wTop, w0+wSide+wSide, h0+wTop+wBottom);

            g.setPaint(GameUI.loadBackground());
            g.fillRect(x0, y0, w0, h0);
            // draw list of games to load
            int lineY = y0+s5;
            listBox.setBounds(x0, y0, w0, h0);
            for (int i=start;i<start+MAX_FILES;i++) {
                int boxIndex = i-start;
                if (boxIndex == selectIndex) {
                    g.setPaint(GameUI.loadHoverBackground());
                    g.fillRect(x0+s20, lineY+s2, w0-s40, lineH-s4);
                }
                if (i<end) {
                    drawSaveGame(g, boxIndex, saveFiles.get(i), saveDates.get(i), x0, lineY, w0, lineH);
                    gameBox[boxIndex].setBounds(x0,lineY,w0,lineH);
                }
                lineY += lineH;
            }
            // draw load button
            int buttonY = lineY+s20;
            int buttonH = s40;
            loadBox.setBounds(button1X,buttonY,buttonW,buttonH);
            g.setColor(SystemPanel.buttonShadowC);
            g.fillRoundRect(button1X+s1,buttonY+s3,buttonW,buttonH,s8,s8);
            g.fillRoundRect(button1X+s2,buttonY+s4,buttonW,buttonH,s8,s8);
            g.setPaint(loadBackC[GameUI.opt()]);
            g.fillRoundRect(button1X,buttonY,buttonW,buttonH,s5,s5);

            String text1 = text("LOAD_GAME_OK");
            g.setFont(narrowFont(30));
            int sw1 = g.getFontMetrics().stringWidth(text1);
            int x1 = button1X + ((buttonW-sw1)/2);

            boolean hoveringLoad = (loadBox == hoverBox) && canLoad();
            Color textC = hoveringLoad ? GameUI.textHoverColor() : GameUI.textColor();
            drawShadowedString(g, text1, 0, 2, x1, buttonY+buttonH-s10, GameUI.textShade(), textC);

            if (hoveringLoad) {
                Stroke prev2 = g.getStroke();
                g.setStroke(stroke1);
                g.drawRoundRect(button1X,buttonY,buttonW,buttonH,s5,s5);
                g.setStroke(prev2);
            }

            // draw cancel button
            cancelBox.setBounds(button2X,buttonY,buttonW,buttonH);
            g.setColor(SystemPanel.buttonShadowC);
            g.fillRoundRect(button2X+s1,buttonY+s3,buttonW,buttonH,s8,s8);
            g.fillRoundRect(button2X+s2,buttonY+s4,buttonW,buttonH,s8,s8);
            g.setPaint(cancelBackC[GameUI.opt()]);
            g.fillRoundRect(button2X,buttonY,buttonW,buttonH,s5,s5);

            String text2 = text("LOAD_GAME_CANCEL");
            g.setFont(narrowFont(30));
            int sw2 = g.getFontMetrics().stringWidth(text2);
            int x2 = button2X + ((buttonW-sw2)/2);

            textC = (cancelBox == hoverBox) ? GameUI.textHoverColor() : GameUI.textColor();
            drawShadowedString(g, text2, 0, 2, x2, buttonY+buttonH-s10, GameUI.textShade(), textC);

            if (cancelBox == hoverBox) {
                Stroke prev2 = g.getStroke();
                g.setStroke(stroke1);
                g.drawRoundRect(button2X,buttonY,buttonW,buttonH,s5,s5);
                g.setStroke(prev2);
            }

            // if loading, draw notice
            if (loading) {
                NoticeMessage.setStatus(text("LOAD_GAME_LOADING"));
                drawNotice(g, 30);
            }
        }
        private void scrollY(int deltaY) {
            yOffset += deltaY;
            if (yOffset > lineH) {
                scrollUp();
                yOffset -= lineH;
            }
            else if (yOffset < -lineH) {
                scrollDown();
                yOffset += lineH;
            }
        }
        private void drawSaveGame(Graphics2D g, int index, String s, String dt, int x, int y, int w, int h) {
            Color c0 = (index != selectIndex) && (hoverBox == gameBox[index]) ? GameUI.loadHoverBackground() : Color.black;
            g.setColor(c0);
            g.setFont(narrowFont(24));
            g.drawString(s, x+s30, y+h-s8);

            g.setFont(font(22));
            int sw = g.getFontMetrics().stringWidth(dt);
            g.drawString(dt, x+w-s30-sw, y+h-s8);
        }
        @Override
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int deltaY = y - lastMouseY;
            lastMouseY = y;

            if (dragging && listBox.contains(x,y))
                scrollY(deltaY);

            Rectangle oldHover = hoverBox;
            hoverBox = null;

            if (loadBox.contains(x,y))
                hoverBox = loadBox;
            else if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else {
                for (int i=0;i<gameBox.length;i++) {
                    if (gameBox[i].contains(x,y))
                        hoverBox = gameBox[i];
                }
            }

            if (hoverBox != oldHover)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {  }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) {
            dragging = true;
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            if (e.getButton() > 3)
                return;
            int count = e.getClickCount();
            if (hoverBox == null)
                return;

            if (hoverBox == loadBox) {
                loadGame(selectedFile);
                return;
            }
            if (hoverBox == cancelBox) {
                cancelLoad();
                return;
            }
            if (count == 2)
                loadGame(selectedFile);
            if (hoverBox != selectBox) {
                softClick();
                selectBox = hoverBox;
                for (int i=0;i<gameBox.length;i++) {
                    if (gameBox[i] == hoverBox)
                        selectIndex = i;
                }
                int fileIndex = start+selectIndex;
                if ((fileIndex == 0) && hasAutosave)
                    selectedFile = GameSession.RECENT_SAVEFILE;
                else
                    selectedFile = saveFiles.get(start+selectIndex)+GameSession.SAVEFILE_EXTENSION;
                current.repaint();
            }
        }
    }
    public static Comparator<File> FILE_NAME = (File f1, File f2) -> f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
    public static Comparator<File> FILE_DATE = (File f1, File f2) -> Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
}
