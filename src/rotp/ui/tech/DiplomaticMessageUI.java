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
package rotp.ui.tech;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.ui.FadeInPanel;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomacyRequestReply;
import rotp.ui.diplomacy.DiplomaticMessage;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.util.ThickBevelBorder;

public class DiplomaticMessageUI extends FadeInPanel implements MouseListener, MouseMotionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    static Color innerTextBackC = new Color(73,163,163);
    static Color outerTextAreaC = new Color(92,208,208);
    static Color textBorderLo1 = new Color(73,163,163);
    static Color textBorderLo2 = new Color(52,126,126);
    static Color textBorderHi1  = new Color(110,240,240);
    static Color textBorderHi2  = new Color(115,252,252);
    static Color textC = Color.white;
    static Color textBgC = Color.darkGray;
    static Color optionC = Color.white;
    static Color hoverOptionC = Color.yellow;
    static Color disabledOptionC = Color.gray;

    static Border outerTextAreaBorder, innerTextAreaBorder;

    private Image flagPole;

    private final Rectangle[] selectBoxes = new Rectangle[DiplomaticMessage.MAX_SELECTIONS];
    private int selectHover = -1;

    Empire diplomatEmpire;
    Image flag, dialogBox;
    DiplomaticMessage message;
    String remarkTitle, messageRemark;

    int talkTimeMs = 5000;
    long startTimeMs;
    float holoPct = 0f;
    boolean hasSpoken = false;
    boolean mouseSet = false;
    boolean exited = false;

    public DiplomaticMessageUI() {
        outerTextAreaBorder = new ThickBevelBorder(8, textBorderHi2, textBorderHi1, textBorderHi2, textBorderHi1, textBorderLo2, textBorderLo1, textBorderLo2, textBorderLo1);
        innerTextAreaBorder = new ThickBevelBorder(8, textBorderLo1, textBorderLo2, textBorderLo1, textBorderLo2, textBorderHi1, textBorderHi2, textBorderHi1, textBorderHi2);

        for (int i=0;i<selectBoxes.length;i++)
            selectBoxes[i] = new Rectangle();
        initModel();
    }
    private void initModel() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    @Override
    public String ambienceSoundKey() { return diplomatEmpire.isPlayer() ? defaultAmbience() : diplomatEmpire.race().diplomacyTheme; }

    public void init(DiplomaticNotification notif) {
        clearBuffer();
        diplomatEmpire = notif.talker();
        if (diplomatEmpire.isPlayer()) {
            flag = player().race().flagNorm();
            dialogBox = player().race().dialogNorm();
        }
        else {
            flag = player().viewForEmpire(diplomatEmpire).flag();
            dialogBox = player().viewForEmpire(diplomatEmpire).dialogueBox();
        }

        diplomatEmpire.race().resetDiplomat();
        message = DialogueManager.current().message(notif.type(), notif.incident(), diplomatEmpire, notif.otherEmpire());
        messageRemark = "";
        if (message == null)
            messageRemark = concat("Message type not defined: ", notif.type());
        else if (notif.otherEmpire() == null)
            messageRemark = diplomatEmpire.decode(message.remark(notif.otherEmpire()), player());
        else 
            messageRemark = diplomatEmpire.decode(message.remark(notif.otherEmpire()), player());
            
        commonInit();
    }
    public void initReply(DiplomacyRequestReply reply) {
        diplomatEmpire = reply.view().owner();
        if (diplomatEmpire.isPlayer()) {
            flag = player().race().flagNorm();
            dialogBox = player().race().dialogNorm();
        }
        else {
            flag = player().viewForEmpire(diplomatEmpire).flag();
            dialogBox = player().viewForEmpire(diplomatEmpire).dialogueBox();
        }
        diplomatEmpire.race().resetDiplomat();
        message = reply;
        messageRemark = reply.remark();
        commonInit();
    }
    private void commonInit() {
        exited = false;
        if (message == null) {
            err(messageRemark);
            return;
        }

        remarkTitle = diplomatEmpire.race().text("RACES_DIPLOMACY_DIALOGUE_TITLE", diplomatEmpire.raceName(), diplomatEmpire.leader().name());
        if (message.showTalking() && playAnimations())
            startFadeTimer();
        else
            endFade();
        selectHover = -1;
        mouseSet = false;
        hasSpoken = false;
        talkTimeMs = playAnimations() ? min(500, messageRemark.length() * 30) : 0;
        startTimeMs = playAnimations() ? System.currentTimeMillis() : 0;
        if (flagPole == null)
            flagPole = currentFrame("FlagPole");
    }
    private void clearSelections() {
        for (Rectangle r: selectBoxes)
            r.setBounds(0,0,0,0);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Image img0 = paintToImage();
        g.drawImage(img0,0,0,null);
        drawOverlay(g);
    }
    public Image paintToImage() {
        if (message == null) 
                err(messageRemark);
        
        clearSelections();
        boolean talking = message.showTalking() && ((System.currentTimeMillis() - startTimeMs) < talkTimeMs);
        boolean receiving = message.showTalking() && ((System.currentTimeMillis() - startTimeMs) < 200);

        Race diploRace = diplomatEmpire.race();
        BufferedImage labImg = diploRace.embassy();
        BufferedImage holoImg = diploRace.holograph();
        Image raceImg = talking ? diploRace.diplomatTalking() : diploRace.diplomatQuiet();

        int w = getWidth();
        int h = getHeight();

        int w0 = labImg.getWidth();
        int h0 = labImg.getHeight();
        //  location of flag
        int fW = scaled(diploRace.flagW);
        int fH = scaled(diploRace.flagH);
        int fX = (int)(w-(diplomatEmpire.race().labFlagX()*w))-(fW/2);
        int fY = (h*4/10)-(fH/2);

        Image dataImg = screenBuffer();
        Graphics2D g = (Graphics2D) dataImg.getGraphics();
        super.paintComponent(g);
        g.drawImage(labImg, w, 0, 0, h, 0, 0, labImg.getWidth(), labImg.getHeight(), null);

        // draw oscillating holograph
        if (holoImg != null) {
            Composite prevComposite = g.getComposite();
            float fluxPct = (holoPct % 1) / 2;
            fluxPct = (fluxPct <= 0.25) ?  1 - fluxPct : 0.5f + fluxPct;
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fluxPct);
            g.setComposite(ac);
            g.drawImage(holoImg, w, 0, 0, h, 0, 0, w0, h0, null);
            g.setComposite(prevComposite);
        }

        // draw flag
        g.drawImage(flag, fX, fY, fW, fH, null);

        // draw diplomat
        g.drawImage(raceImg, w, 0, 0, h, 0, 0, raceImg.getWidth(null), raceImg.getHeight(null), null);

        // draw game image to screen, flipped horizontally
        //g.drawImage(dataImg, w, 0, 0, h, 0, 0, w, h, null);

        // draw dialog image to screen
        int boxW = dialogBox.getWidth(null);
        int boxH = dialogBox.getHeight(null);
        int boxH2 = scaled(boxH);
        int boxW2 = scaled(boxW);
        int boxY = h-boxH2;
        g.drawImage(dialogBox, 0, boxY, boxW2, boxH2, null);

        int rMargin = scaled(player().race().dialogRightMargin());

        int textBoxX = scaled(player().race().dialogLeftMargin());
        int textBoxY = scaled(player().race().dialogTopY());
        int textBoxW = w-textBoxX-rMargin;
        int textBoxH = h-textBoxY;

        drawText(g, receiving, textBoxX, textBoxY, textBoxW, textBoxH);

        g.dispose();
        return screenBuffer();
    }
    private int dialogFontSize(String remark, int options) {
        // largest font under any conditions
        int maxSize0 = 30;
        // maxFont based on number of replies
        int maxSize1 = 44-(options*3);
        // maxFont base on remark length (150 chars = 3 max lines at font 35 >> font 30)
        int maxSize2 = (400-remark.length())/10;
        return min(maxSize0, maxSize1, maxSize2);
    }
    private void drawText(Graphics g, boolean receiving, int x, int y, int w, int h) {
        if (message == null) {
            err(messageRemark);
            return;
        }

        g.setColor(textC);

        int x1 = x+s20;
        int y1 = y-s5;
        int w1 = w-s20-s20;

        boolean showRemarkOnly = false;
        // draw remark
        String displayText = messageRemark;
        if (!diplomatEmpire.isPlayer() && message.showTalking()) {
            showRemarkOnly = true;
            if (receiving)
                displayText = text("DIPLOMACY_RECEIVING");
            else
                showRemarkOnly = false;
        }

        // draw the remark...  may be multi-line
        // data lines will draw two items per line
        int nonRemarkLines = message.numReplies()+(message.numDataLines()+1)/2;
        int maxLinesForText = nonRemarkLines > 3 ? 2 : 6-nonRemarkLines;
        g.setFont(narrowFont(18));
        drawBorderedString(g, remarkTitle, 1, x1, y1, textBgC,  textC);
        List<String> remarkLines = scaledDialogueWrappedLines(g, displayText, w1, maxLinesForText, 26, 16);
        int lineH = g.getFontMetrics().getHeight();
        //List<String> remarkLines = wrappedLines(g, displayText, w1);
        for (String line: remarkLines) {
            y1 += lineH;
            drawBorderedString(g, line, 1, x1, y1, textBgC,  textC);
        }

        y1 += s10;
        if (showRemarkOnly)
            return;

        // don't show options until ready to click
        if (waitingOnMessage())
            return;

        // if this is a simple message  with no replies, display as wrapped text
        // and enable selection on any portion of the screen
        if (message.numReplies() == 0) {
            selectBoxes[0].setBounds(0,0,getWidth(),getHeight());
            return;
        }

        // space remaining (in actual px)
        int ySpace = h - (y1-y)-s35;

        // calculate line height size for options (max 36)
        int optionLineH = min(s24, ySpace / nonRemarkLines);
        lineH = optionLineH;
        int yGap = (ySpace-(lineH*nonRemarkLines))/2;

        // calculate option font size based on line height
        int fontSize = unscaled(optionLineH) - 2;

        if (fontSize < 10) 
            err("Font too small. size:", str(fontSize), "  for messageText:  ", displayText);

        // draw data lines, smaller text & indented
        int dataLines = message.numDataLines();
        if (dataLines > 0) {
            int margin1 = s40;
            int margin2 = w1/2+s20;
            int x2 = x1;
            for (int i=0;i<dataLines;i++) {
                if (i % 2 == 0) {
                    y1 += (lineH+s2);
                    x2 = x1+margin1;
                }
                else 
                    x2 = x1+margin2;
                String reply = message.dataLine(i);
                g.fillOval(x1, y1-s9, s3, s3);
                drawBorderedString(g, reply, 1, x2+s10, y1, textBgC, textC);
            }
        }
        // draw options, smaller text & indented
        y1 += yGap;
        x1 += s20;
        Color c0, c0b;
        Color c1 = new Color(255,255,0,64);
        for (int i=0;i<message.numReplies();i++) {
            c0b = disabledOptionC;
            g.setFont(narrowFont(fontSize));
            y1 += (lineH+s2);
            if (!message.enabled(i))
                c0 = disabledOptionC;
            else if (i == selectHover) {
                c0 = hoverOptionC;
                c0b = hoverOptionC;
                g.setColor(c1);
                g.fillRoundRect(x,y1-lineH+s5, w, lineH, lineH, lineH);
            }
            else 
                c0 = optionC;
            g.setColor(c0);
            g.drawString(""+(i+1), x1-s15, y1);
            g.fillOval(x1, y1-s9, s3, s3);
            String reply = message.reply(i);
            String replyDetail = message.replyDetail(i);
            int sw1 = g.getFontMetrics().stringWidth(reply);
            drawBorderedString(g, reply, 1, x1+s10, y1, textBgC, c0);
            if (!replyDetail.isEmpty()) {
                g.setFont(narrowFont(fontSize-4));
                drawBorderedString(g, replyDetail, 1, x1+s30+sw1, y1, textBgC, c0b);
            }
            selectBoxes[i].setBounds(x1-s15, y1-lineH+s5, w, lineH);
        }
        findAndSetMouse();
    }
    private void selectOption(int opt) {
        if (selectHover < 0)
            return;
        if (stillFading() || waitingOnMessage())
            return;
        exited = true;
        softClick();
        message.select(opt);
    }
    private boolean waitingOnMessage() {
        if (diplomatEmpire.isPlayer() || !message.showTalking())
            return false;
        return (System.currentTimeMillis() - startTimeMs) < 500;
    }
    private void setMouseLocation(int x, int y) {
        int prevHover = selectHover;
        selectHover = -1;
        for (int i=0;i<selectBoxes.length;i++) {
            if (selectBoxes[i].contains(x,y))
                selectHover = i;
        }
        if (prevHover != selectHover)
            repaint();
    }
    private void findAndSetMouse() {
        if (!mouseSet) {
            mouseSet = true;
            Point p = MouseInfo.getPointerInfo().getLocation();
            try {
                Point p0 = getLocationOnScreen();
                setMouseLocation(p.x-p0.x, p.y-p0.y);
            }
            catch(Exception e) {
                // sometimes getLocationOnScreen() breaks if this panel is no longer active
            }
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (waitingOnMessage())
                return;

        int k = e.getKeyCode();

        switch(k) {
            case KeyEvent.VK_1: selectHover = 0; selectOption(0); return;
            case KeyEvent.VK_2: selectHover = 1; selectOption(1); return;
            case KeyEvent.VK_3: selectHover = 2; selectOption(2); return;
            case KeyEvent.VK_4: selectHover = 3; selectOption(3); return;
            case KeyEvent.VK_5: selectHover = 4; selectOption(4); return;
            case KeyEvent.VK_6: selectHover = 5; selectOption(5); return;

            case KeyEvent.VK_ESCAPE:
                exited = true;
                message.escape();
        }
    }

    @Override
    public void mouseDragged(MouseEvent arg0) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        setMouseLocation(e.getX(), e.getY());
    }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent arg0) { }
    @Override
    public void mousePressed(MouseEvent arg0) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (selectHover < 0)
            return;
        selectOption(selectHover);
    }
    @Override
    public void animate() {
        if (!playAnimations())
            return;

        advanceFade();
        holoPct += .1;
        if (!stillFading()) {
            if (!hasSpoken) {
                hasSpoken = true;
                talkTimeMs = 1000 + fadeInMs();
            }
        }
        repaint();
    }
}
