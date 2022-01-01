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
package rotp.ui.design;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import javax.swing.*;
import java.util.List;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.main.SystemPanel;

public final class ConfirmCreateUI extends BasePanel implements KeyListener, MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);

    private static final int DIALOG_W = 310;
    private static final int DIALOG_H = 355;
    private static final Color borderC = new Color(112,85,68,128);
    private static final Color backC = new Color(112,85,68);
    private final Color greenEdgeC = new Color(44,59,30);
    private final Color greenMidC = new Color(70,93,48);
    private static final Color brownEdgeC = new Color(59,44,30);
    private static final Color brownMidC = new Color(93,70,48);
    private static final Color textBackC = new Color(178,124,87);

    final static String CANCEL_ACTION = "cancel-input";
    final static String ACCEPT_ACTION = "accept-input";

    private LinearGradientPaint cancelBackground;
    private LinearGradientPaint createBackground;
    private Shape hoverTarget;
    private final Rectangle cancelButtonArea = new Rectangle();
    private final Rectangle createButtonArea = new Rectangle();

    private ShipDesign targetDesign;
    private BaseTextField nameField;

    public boolean renamingOnly = false;
    boolean setNameBounds = true;
    int keystrokeCount = 0;

    public ConfirmCreateUI() {
        setOpaque(true);
        initTextFields();
        add(nameField);
    }
    public void targetDesign(ShipDesign d) {
        keystrokeCount = 0;
        targetDesign = d;
        targetDesign.resetImage();
        if ((targetDesign.name() == null) || targetDesign.name().isEmpty())
            player().shipLab().nameDesign(targetDesign);
        nameField.setFont(narrowFont(20));
        nameField.setText(targetDesign.name().trim());
        nameField.setCaretPosition(nameField.getText().length());
        setNameBounds = true;
    }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);
        
        DesignUI.instance.paint(g0);
        nameField.requestFocus();
        int w = getWidth();
        int h = getHeight();
        int bdr = s10;
        int mgn = s10;

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);

        // widths needed for spacing
        int dlgW = scaled(DIALOG_W);
        int boxW = dlgW-bdr-bdr;
        int shipW = boxW-mgn-mgn;
        int w0 = shipW-mgn-mgn;
        
        // draw border
        int dlgH = scaled(DIALOG_H);
        List<String> amtLines = null; 
        // extra descriptive text for deploy, so window may need to be taller
        if (!renamingOnly) {
            g.setFont(narrowFont(16));
            String descString = text("SHIP_DESIGN_DEPLOY_DESC");
            amtLines = wrappedLines(g, descString,  w0);
            dlgH = scaled(DIALOG_H)+(amtLines.size()*s16);        
        }
        
        int dlgX = (w-dlgW)/2;
        int dlgY = (h-dlgH)/2;
        g.setColor(borderC);
        g.fillRect(dlgX,dlgY,dlgW,dlgH);

        // draw Box
        int boxX = dlgX+bdr;
        int boxY = dlgY+bdr;
        int boxH = dlgH-bdr-bdr;
        g.setColor(backC);
        g.fillRect(boxX, boxY, boxW, boxH);

        // draw ship
        int shipH = shipW*3/4;
        drawShipIcon(g, boxX+mgn, boxY+mgn, shipW, shipH);

        int y0 = boxY+mgn+shipH;
        int x0 = boxX+mgn+mgn;
        String titleString = renamingOnly ? text("SHIP_DESIGN_RENAME_CONFIRM") : text("SHIP_DESIGN_DEPLOY_CONFIRM");

        g.setFont(narrowFont(20));
        List<String> titleLines = wrappedLines(g, titleString, w0);

        int y1 = y0 + s25;
        g.setColor(SystemPanel.whiteText);
        for (String line: titleLines) {
            int sw = g.getFontMetrics().stringWidth(line);
            int x1 = (w - sw) / 2;
            drawString(g,line, x1, y1);
            y1 += s20;
        }

        int y2 = y1+s30;
        g.setFont(narrowFont(16));
        if (setNameBounds) {
            setNameBounds = false;
            nameField.setCaretPosition(nameField.getText().length());
            nameField.setBounds(x0,y2-s40,w0,s26); // y coord for namefield is that the top of the box, not the bottom
        }
        if (!renamingOnly) {
            y2 += s5;
            g.setFont(narrowFont(16));
            g.setColor(SystemPanel.blackText);
            for (String line: amtLines) {
                int sw = g.getFontMetrics().stringWidth(line);
                int x1 = (w-sw)/2;
                drawString(g,line, x1, y2);
                y2 += s16;
            }
        }

        int y3 = y2+s10;
        // button vars
        int buttonH = s25;
        int buttonW = (boxW-(2*bdr)-(3*mgn))/2;
        int buttonY = y3;
        int button1X = boxX+s10;
        int button2X = boxX+boxW-buttonW-s10;
        g.setFont(narrowFont(20));

        if (createBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(button2X, 0);
            Point2D ptEnd = new Point2D.Float(button2X + buttonW, 0);
            Color[] yesColors = {greenEdgeC, greenMidC, greenEdgeC};
            createBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        createButtonArea.setBounds(button1X, buttonY, buttonW, buttonH);
        boolean hovering = hoverTarget == createButtonArea;
        g.setPaint(createBackground);
        g.fillRoundRect(button1X, buttonY, buttonW, buttonH, s3, s3);
        Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
        g.setColor(c0);
        Stroke prevStr = g.getStroke();
        g.setStroke(BasePanel.stroke1);
        g.drawRoundRect(button1X, buttonY, buttonW, buttonH, s3, s3);
        g.setStroke(prevStr);
        String str = renamingOnly ? text("SHIP_DESIGN_RENAME_OK") : text("SHIP_DESIGN_DEPLOY_OK");
        int sw = g.getFontMetrics().stringWidth(str);
        int x2a = button1X + ((buttonW - sw) / 2);
        drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);

        if (cancelBackground == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D ptStart = new Point2D.Float(button2X, 0);
            Point2D ptEnd = new Point2D.Float(button2X + buttonW, 0);
            Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
            cancelBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
        }
        cancelButtonArea.setBounds(button2X, buttonY, buttonW, buttonH);
        hovering = hoverTarget == cancelButtonArea;
        g.setPaint(cancelBackground);
        g.fillRoundRect(button2X, buttonY, buttonW, buttonH, s3, s3);
        c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
        g.setColor(c0);
        prevStr = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(button2X, buttonY, buttonW, buttonH, s3, s3);
        g.setStroke(prevStr);
        str = text("SHIP_DESIGN_DEPLOY_CANCEL");
        sw = g.getFontMetrics().stringWidth(str);
        x2a = button2X + ((buttonW - sw) / 2);
        drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
    }
    private void initTextFields() {
        nameField = new BaseTextField(this);
        nameField.setLimit(18);
        nameField.setBackground(textBackC);
        nameField.setBorder(newEmptyBorder(5,5,0,0));
        nameField.setMargin(new Insets(0, 0, 0, 0));
        nameField.setFont(narrowFont(20));
        nameField.setForeground(SystemPanel.blackText);
        nameField.setCaretColor(SystemPanel.blackText);
        nameField.putClientProperty("caretWidth", s3);
        nameField.setFocusTraversalKeysEnabled(false);
        nameField.setVisible(true);

        InputMap im0 = nameField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am0 = nameField.getActionMap();
        im0.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_ACTION);
        im0.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACCEPT_ACTION);
        am0.put(CANCEL_ACTION, new CancelAction());
        am0.put(ACCEPT_ACTION, new CreateAction());
        nameField.addKeyListener(this);
        
        addMouseListener(this);
        addMouseMotionListener(this);
        nameField.addMouseListener(this);
    }
    private void drawShipIcon(Graphics g, int x, int y, int w, int h) {
        g.setColor(Color.black);
        g.fillRect(x, y, w, h);

        Image img = targetDesign.image();

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale = min((float)w/w0, (float)(h-s20)/h0);

        int w1 = (int)(scale*w0);
        int h1 = (int)(scale*h0);

        int x1 = x+((w-w1)/2);
        int y1 = y+((h-s20-h1)/2);
        g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);
    }
    private void cancelAction() {
        hoverTarget = null;
        disableGlassPane();
    }
    private void createAction() {
        if (renamingOnly && nameField.getText().trim().isEmpty()) {
            cancelAction();
            return;
        }
        targetDesign.active(true);
        targetDesign.setIconKey();
        String name = nameField.getText().trim();
        if (name.isEmpty() && (targetDesign.name() == null))
            targetDesign.name(text("SHIP_DESIGN_UNNAMED_DESIGN"));
        else
            targetDesign.name(name);
        
        targetDesign.clearEmptyWeapons();

        hoverTarget = null;
        disableGlassPane();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        keystrokeCount++;
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_BACK_SPACE) {
            if (keystrokeCount == 1) {
                nameField.setText(" ");
                return;
            }
        }
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
        if (hoverTarget == cancelButtonArea) {
            softClick();
            cancelAction();
            return;
        }
        else if (hoverTarget == createButtonArea) {
            softClick();
            createAction();
            return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Shape prevHover = hoverTarget;
        hoverTarget = null;
        if (cancelButtonArea.contains(x,y))
            hoverTarget = cancelButtonArea;
        else if (createButtonArea.contains(x,y))
            hoverTarget = createButtonArea;
            
        if (prevHover != hoverTarget) 
            repaint();  
    }
    class CreateAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            createAction();
        }
    }
    class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            cancelAction();
        }
    }
}
