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
package rotp.ui.planets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.colony.Colony;
import rotp.model.colony.ColonyDefense;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.empires.RaceCombatAnimation;
import rotp.model.galaxy.Transport;
import rotp.model.tech.TechHandWeapon;
import rotp.ui.BasePanel;
import rotp.util.Base;
import rotp.util.sound.SoundClip;

public class GroundBattleUI extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SOLDIERS = 500;
    private static final int MAX_COUNTDOWN = 50;
    private static final int MAX_SHIPS = 3;
    private static final int MIN_COUNTDOWN = -10;
    private final static int[] attackerState = new int[MAX_SOLDIERS];
    private final static int[] defenderState = new int[MAX_SOLDIERS];
    private final static int[] attackerX = new int[MAX_SOLDIERS];
    private final static int[] defenderX = new int[MAX_SOLDIERS];
    private final static int[] attackerY = new int[MAX_SOLDIERS];
    private final static int[] defenderY = new int[MAX_SOLDIERS];

    private static int ATTACKER_END_FIRING, DEFENDER_END_FIRING, ATTACKER_END_DYING, DEFENDER_END_DYING;
    private final static int BEGIN_FIRING = 1;
    private final static int NOT_FIRING = 0;
    private final static int BEGIN_DYING = -1;

    private boolean attackerDead;  // true if attacker died this turn, false for defender
    private int deathThisTurn = 0; // index of attacker/defender that died this turn

    private Colony colony;
    private Transport transport;
    private TechHandWeapon attackerWeapon;
    private TechHandWeapon defenderWeapon;
    private int totalAttackers  = 0;
    private int totalDefenders = 0;
    private int landingCount;

    private Image landscapeImg;
    private LinearGradientPaint soldierBackG;
    String subtitle;

    private final List<Image> descendingFrames = new ArrayList<>();
    private final List<Integer> descendingFrameRefs = new ArrayList<>();
    private final List<Image> openingFrames = new ArrayList<>();
    private final List<Integer> openingFrameRefs = new ArrayList<>();

    List<BufferedImage> attackerFrames = new ArrayList<>();
    List<BufferedImage> defenderFrames = new ArrayList<>();
    List<BufferedImage> attackerDeathFrames = new ArrayList<>();
    List<BufferedImage> defenderDeathFrames = new ArrayList<>();
    List<Integer> remainingAttackers = new ArrayList<>();
    List<Integer> remainingDefenders = new ArrayList<>();
    // which animated frame the soldiers will fire
    int attackerFiringFrame, defenderFiringFrame;
    int attackerFinalFrame, defenderFinalFrame;
    int attackerIconW, attackerIconH, defenderIconW, defenderIconH;
    int attackerImgW, attackerImgH, defenderImgW, defenderImgH;
    float attackerScale, defenderScale;
    int attackerYSpacing, defenderYSpacing, attackerXSpacing, defenderXSpacing;
    boolean attackerFired, defenderFired;
    boolean exited = false;
    int baseIconH;
    // x,y coords (within icon img) of gun when firing
    int attackerGunX = 0;
    int attackerGunY = 0;
    int defenderGunX = 0;
    int defenderGunY = 0;
    LandingShip[] ships = new LandingShip[MAX_SHIPS];
    private SoundClip shipLanding;

    public GroundBattleUI() {
        init();
    }
    private void init() {
        setBackground(Color.black);
        addMouseListener(this);
    }
    public void init(Colony c, Transport tr) {
        colony = c;
        transport = tr;
        baseIconH = s70;  // standard width of troopers
        landingCount = 0;
        exited = false;
        initLandscapeImage(colony);
        
        if (tr.size() < tr.originalSize()) 
            subtitle = text("INVASION_SOME_TROOPS_LANDED", str(tr.size()), str(tr.originalSize()), tr.empire().raceName());
        else
            subtitle = text("INVASION_TROOPS_LANDED", str(tr.size()), tr.empire().raceName());

        descendingFrames.clear();
        descendingFrameRefs.clear();
        Race race = tr.empire().race();
        
        allFrames(race.transportDescKey, race.transportDescFrames, 0, descendingFrames, descendingFrameRefs);
        openingFrames.clear();
        openingFrameRefs.clear();
        allFrames(race.transportOpenKey, race.transportOpenFrames, 0, openingFrames, openingFrameRefs);

        for (int i=0;i<ships.length;i++)
            ships[i] = new LandingShip(i);

        Empire attackerEmp = transport.empire();
        Empire defenderEmp = colony.empire();

        // determine which attack and death animations to use
        attackerWeapon = attackerEmp.tech().topHandWeaponTech();
        defenderWeapon = defenderEmp.tech().topHandWeaponTech();
        RaceCombatAnimation attacker;
        RaceCombatAnimation defender;
        RaceCombatAnimation attackerDeath  = null;
        RaceCombatAnimation defenderDeath  = null;
        if (c.planet().isEnvironmentHostile()) {
            attacker  = attackerEmp.race().troopHostile;
            defender  = defenderEmp.race().troopHostile;
            switch(attackerWeapon.deathType) {
                case TechHandWeapon.COLLAPSE: defenderDeath = defenderEmp.race().troopDeath1H; break;
                case TechHandWeapon.DISRUPT: defenderDeath = defenderEmp.race().troopDeath2H; break;
                case TechHandWeapon.IMMOLATE: defenderDeath = defenderEmp.race().troopDeath3H; break;
                case TechHandWeapon.VAPORIZE: defenderDeath = defenderEmp.race().troopDeath4H; break;
            }
            switch(defenderWeapon.deathType) {
                case TechHandWeapon.COLLAPSE: attackerDeath = attackerEmp.race().troopDeath1H; break;
                case TechHandWeapon.DISRUPT: attackerDeath = attackerEmp.race().troopDeath2H; break;
                case TechHandWeapon.IMMOLATE: attackerDeath = attackerEmp.race().troopDeath3H; break;
                case TechHandWeapon.VAPORIZE: attackerDeath = attackerEmp.race().troopDeath4H; break;
            }
        }
        else {
            attacker  = attackerEmp.race().troopNormal;
            defender  = defenderEmp.race().troopNormal;
            switch(attackerWeapon.deathType) {
                case TechHandWeapon.COLLAPSE: defenderDeath = defenderEmp.race().troopDeath1; break;
                case TechHandWeapon.DISRUPT: defenderDeath = defenderEmp.race().troopDeath2; break;
                case TechHandWeapon.IMMOLATE: defenderDeath = defenderEmp.race().troopDeath3; break;
                case TechHandWeapon.VAPORIZE: defenderDeath = defenderEmp.race().troopDeath4; break;
            }
            switch(defenderWeapon.deathType) {
                case TechHandWeapon.COLLAPSE: attackerDeath = attackerEmp.race().troopDeath1; break;
                case TechHandWeapon.DISRUPT: attackerDeath = attackerEmp.race().troopDeath2; break;
                case TechHandWeapon.IMMOLATE: attackerDeath = attackerEmp.race().troopDeath3; break;
                case TechHandWeapon.VAPORIZE: attackerDeath = attackerEmp.race().troopDeath4; break;
            }
        }

        // init attacker vars
        totalAttackers = tr.size();
        attackerFinalFrame = 0;
        attackerFrames.clear();
        for (Image img: attacker.firingFrames())
            attackerFrames.add(asBufferedImage(img));
        attackerDeathFrames.clear();
        for (Image img: attackerDeath.firingFrames())
            attackerDeathFrames.add(asBufferedImage(img));
        ATTACKER_END_DYING = BEGIN_DYING - attackerDeathFrames.size() + 1;
        ATTACKER_END_FIRING = BEGIN_FIRING + attackerFrames.size() - 1;
        attackerFiringFrame = attacker.firingFrame;
        attackerIconW = attackerFrames.get(0).getWidth();
        attackerIconH = attackerFrames.get(0).getHeight();
        attackerScale = attacker.scale;
        attackerYSpacing = scaled(attacker.ySpacing);
        attackerXSpacing = scaled(attacker.xSpacing);
        attackerImgW = (int) (attackerScale*attackerIconW);
        attackerImgH = (int) (attackerScale*attackerIconH);
        attackerGunX = (int) (attackerScale*attacker.gunX);
        attackerGunY = (int) (attackerScale*attacker.gunY);
        remainingAttackers.clear();
        for (int i=0;i<totalAttackers;i++)
            remainingAttackers.add(i);
        for (int i=0;i<attackerState.length;i++)
            attackerState[i] = NOT_FIRING;  // -1 is "not firing"  0-N is the firing frame #

        // init defender vars
        totalDefenders = c.defense().troops();
        defenderFinalFrame = 0;
        defenderFrames.clear();
        for (Image img: defender.firingFrames())
            defenderFrames.add(flip(asBufferedImage(img)));
        defenderDeathFrames.clear();
        for (Image img: defenderDeath.firingFrames())
            defenderDeathFrames.add(flip(asBufferedImage(img)));
        DEFENDER_END_DYING = BEGIN_DYING - defenderDeathFrames.size() + 1;
        DEFENDER_END_FIRING = BEGIN_FIRING + defenderFrames.size() - 1;
        defenderFiringFrame = defender.firingFrame;
        defenderIconW = defenderFrames.get(0).getWidth();
        defenderIconH = defenderFrames.get(0).getHeight();
        defenderScale = defender.scale;
        defenderYSpacing = scaled(defender.ySpacing);
        defenderXSpacing = scaled(defender.xSpacing);
        defenderImgW = (int) (defenderScale*defenderIconW);
        defenderImgH = (int) (defenderScale*defenderIconH);
        defenderGunX = (int) (defenderScale*defender.gunX);
        defenderGunY = (int) (defenderScale*defender.gunY);
        remainingDefenders.clear();
        for (int i=0;i<totalDefenders;i++)
                remainingDefenders.add(i);
        for (int i=0;i<defenderState.length;i++)
                defenderState[i] = NOT_FIRING;  // -1 is "not firing"  0-N is the firing frame #
        shipLanding = playAudioClip(attackerEmp.race().shipAudioKey);

        //log("Starting Ground Battle. ", totalAttackers+" attackers vs. ", str(totalDefenders), " defenders");
    }
    private void initLandscapeImage(Colony c) {
        int w = getWidth();
        int h = getHeight();
        landscapeImg = newBufferedImage(w,h);
        Graphics2D g = (Graphics2D) landscapeImg.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(c.planet().type().atmosphereImage(), 0, 0, w, h, null);
        g.drawImage(c.planet().type().randomCloudImage(), 0, 0, w, h, null);
        g.drawImage(c.planet().landscapeImage(), 0, 0, w, h, null);

        // draw fortress
        //BufferedImage fortImg = colony.empire().race().fortress(colony.fortressNum());
        BufferedImage fortImg = colony.empire().race().fortress(0);
        int fortW = scaled(fortImg.getWidth());
        int fortH = scaled(fortImg.getHeight());
        int fortX = w-fortW;
        int fortY = h-scaled(180)-fortH;
        g.drawImage(fortImg, fortX, fortY, fortX+fortW, fortY+fortH, 0, 0, fortImg.getWidth(), fortImg.getHeight(), null);

        // for hostile planets, draw shield
        if (colony.empire().race().isHostile(colony.planet().type())) {
            BufferedImage shieldImg = colony.empire().race().shield();
            g.drawImage(shieldImg, fortX, fortY, fortX+fortW, fortY+fortH, 0, 0, shieldImg.getWidth(), shieldImg.getHeight(), null);
        }

        int barY = getHeight()-scaled(140);
        if (soldierBackG == null) {
            Color c0 = new Color(255,255,255,128);
            Color c1 = new Color(255,255,255,0);
            Point2D start = new Point2D.Float(0, barY);
            Point2D end = new Point2D.Float(getWidth(), barY);
            float[] dist = {0.0f, 0.4f, 0.6f, 1.0f};
            Color[] colors = {c0, c1, c1, c0 };
            soldierBackG = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(soldierBackG);
        g.fillRect(0, barY, getWidth(), s80);

        g.dispose();
    }
    private void allStopFiring() {
        for (int i=0;i<attackerState.length;i++) {
            if (attackerState[i] < NOT_FIRING)
                attackerState[i] = ATTACKER_END_DYING;
            else
                attackerState[i] = NOT_FIRING;
        }
        for (int i=0;i<defenderState.length;i++) {
            if (defenderState[i] < NOT_FIRING)
                defenderState[i] = DEFENDER_END_DYING;
            else
                defenderState[i] = NOT_FIRING;
        }
    }
    private float startPct(int count) {
        if (count < 5)
            return 1.0f;
        else if (count < 20)
            return 0.4f;
        else
            return 0.1f;
    }
    private void allStartFiring(int attackerCount, int defenderCount) {
        float attackerStartPct = startPct(attackerCount);
        float defenderStartPct = startPct(defenderCount);
        int attackFrames = attackerFrames.size();

        for (int i=0;i<attackerCount;i++) {
            int currState= attackerState[i];
            if (attackerState[i] == NOT_FIRING) {
                if (random() < attackerStartPct)
                    attackerState[i] = BEGIN_FIRING;
            }
            else if (attackerState[i] <= BEGIN_DYING) {
                if (attackerState[i] > ATTACKER_END_DYING)
                    attackerState[i]--;
            }
            else if (attackerState[i] >= BEGIN_FIRING) {
                attackerState[i]++;
                if (attackerState[i] >= ATTACKER_END_FIRING)
                    attackerState[i] = NOT_FIRING;
            }
            //System.out.println("Attacker "+i+": from "+currState+" to "+attackerState[i]);
        }
        for (int i=0;i<defenderCount;i++) {
            int currState= defenderState[i];
            if (defenderState[i] == NOT_FIRING) {
                if (random() < defenderStartPct)
                    defenderState[i] = BEGIN_FIRING;
            }
            else if (defenderState[i] <= BEGIN_DYING) {
                if (defenderState[i] > DEFENDER_END_DYING)
                    defenderState[i]--;
            }
            else if (defenderState[i] >= BEGIN_FIRING) {
                defenderState[i]++;
                if (defenderState[i] >= DEFENDER_END_FIRING)
                    defenderState[i] = NOT_FIRING;
            }
            //System.out.println("Defender "+i+": from "+currState+" to "+defenderState[i]);
        }
    }
    @Override
    public String ambienceSoundKey() { return "GroundCombatAmbience"; }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        switch(k) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_SPACE:
                advanceScreen();
        }
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintCombatScene(screenBuffer());
        g.drawImage(screenBuffer(),0,0,null);
    }
    private void paintCombatScene(Image img) {
        Graphics2D g = (Graphics2D) img.getGraphics();
        int w = getWidth();
        int h = getHeight();
        Color detailLineC = Color.white;

        attackerFired =false;
        defenderFired = false;

        g.drawImage(landscapeImg, 0, 0, w, h, null);

        int textY = h-s100;

        // draw ships (one ship per 20 transports)
        int numShips = bounds(1,(totalAttackers/20)+1, ships.length);
        for (int i=numShips-1;i>=0;i--)
            ships[i].draw(g);

        // draw defenders
        ColonyDefense defense = colony.defense();
        int x0 = (w/2)+s40;
        int y0 = textY;
        drawTroops(g, totalDefenders, false, x0, y0-s40);

        if (!landing()) {
            String attArmorDesc = text("INVASION_TROOP_ARMOR_DESC", transport.armorDesc(), transport.battleSuitDesc());
            String attShieldDesc = transport.shieldDesc();
            String attWpnDesc = transport.weaponDesc();
            String attLine = concat(attArmorDesc, attShieldDesc, attWpnDesc);
            g.setFont(narrowFont(28));
            // draw attacker15s
            x0 = s40;
            y0 = textY;
            drawTroops(g, totalAttackers, true, x0, y0-s40);
            // draw attacker info
            g.setFont(narrowFont(40));
            String str1 = text("INVASION_ATTACKERS_TITLE", str(transport.size()), transport.empire().raceName());
            drawBorderedString(g, str1, 2, x0, y0, Color.black, Color.white);
            scaledFont(g, attLine, w/2-s60, 22, 18);
            //g.setFont(narrowFont(22));
            y0 += s30;
            if (!attArmorDesc.isEmpty()) {
                int sw = g.getFontMetrics().stringWidth(attArmorDesc)+s10;
                drawBorderedString(g,attArmorDesc,2,x0,y0,Color.black, detailLineC);
                x0 += sw;
            }
            if (!attShieldDesc.isEmpty()) {
                int sw = g.getFontMetrics().stringWidth(attShieldDesc)+s10;
                drawBorderedString(g,attShieldDesc,2,x0,y0,Color.black, detailLineC);
                x0 += sw;
            }
            if (!attWpnDesc.isEmpty()) {
                drawBorderedString(g,attWpnDesc,2,x0,y0,Color.black, detailLineC);
            }
        }

        // draw defender info
        g.setFont(narrowFont(40));
        x0 = w-s40;
        y0 = textY;
        String str2;
        if (colony.empire() == transport.empire())
            str2 = text("INVASION_REBELS_TITLE", str(defense.troops()), colony.empire().raceName());
        else
            str2 = text("INVASION_DEFENDERS_TITLE", str(defense.troops()), colony.empire().raceName());
        int sw2 = g.getFontMetrics().stringWidth(str2);
        drawBorderedString(g, str2, 2, x0-sw2, textY, Color.black, Color.white);
        String defArmorDesc = text("INVASION_TROOP_ARMOR_DESC", defense.armorDesc(), defense.battleSuitDesc());
        String defShieldDesc = defense.personalShieldDesc();
        String defWpnDesc = defense.weaponDesc();
        String defLine = concat(defArmorDesc, defShieldDesc, defWpnDesc);
        scaledFont(g, defLine, w/2-s60, 22, 18);
        //g.setFont(narrowFont(22));
        y0 += s30;
        x0 = w-s30;
        if (!defWpnDesc.isEmpty()) {
            int sw = g.getFontMetrics().stringWidth(defWpnDesc)+s10;
            x0 -= sw;
            drawBorderedString(g,defWpnDesc,2,x0,y0,Color.black, detailLineC);
        }
        if (!defShieldDesc.isEmpty()) {
            int sw = g.getFontMetrics().stringWidth(defShieldDesc)+s10;
            x0 -= sw;
            drawBorderedString(g,defShieldDesc,2,x0,y0,Color.black, detailLineC);
        }
        if (!defArmorDesc.isEmpty()) {
            int sw = g.getFontMetrics().stringWidth(defArmorDesc)+s10;
            x0 -= sw;
            drawBorderedString(g,defArmorDesc,2,x0,y0,Color.black, detailLineC);
        }

        // draw title last (so it overlays any ship)
        int titleLineH = s40;
        g.setFont(narrowFont(40));
        List<String> lines = wrappedLines(g, title(), w*4/5);
        y0 = s10;
        for (String line: lines) {
            y0 += titleLineH;
            int sw = g.getFontMetrics().stringWidth(line);
            x0 = (w-sw)/2;
            drawBorderedString(g, line, 2, x0, y0, Color.black, Color.yellow);
        }
        // draw subtitle
        g.setFont(narrowFont(24));
        int sw = g.getFontMetrics().stringWidth(subtitle);
        x0 = (w-sw)/2;
        y0 += s30;
        drawBorderedString(g, subtitle, 1, x0, y0, Color.black, Color.yellow);
        
        drawSkipText(g, !battleInProgress());

        g.dispose();
    }
    @Override
    public void animate() {
        landingCount++;
        if (!landing()) {
            if (battleInProgress()) {
                allStartFiring(totalAttackers, totalDefenders);
                if (animationCount() % 3 == 0) {
                    attackerDead = colony.singleCombatAgainstTransports(transport);
                    deathThisTurn = assignRandomVictim(attackerDead);
                    if (!battleInProgress())
                        allStopFiring();
                }
            }
            else
                    return;
        }
        repaint();
    }
    private int assignRandomVictim(boolean attacker) {
        Integer deadIndex;
        if (attacker) {
            deadIndex = random(remainingAttackers);
            if (deadIndex < attackerState.length)
                attackerState[deadIndex] = BEGIN_DYING;
            remainingAttackers.remove(deadIndex);
            //log("Dead: Attacker - "+deadIndex+"   "+remainingAttackers.size()+" remaining");
        }
        else {
            deadIndex = random(remainingDefenders);
            if (deadIndex < defenderState.length)
                defenderState[deadIndex] = BEGIN_DYING;
            remainingDefenders.remove(deadIndex);
            //log("Dead: Defender - "+deadIndex+"   "+remainingDefenders.size()+" remaining");
        }
        return deadIndex;
    }
    private String title() {
        int year = galaxy().currentYear();
        String invaders = transport.empire().raceName();
        String defenders = colony.empire().raceName();
        String name = player().sv.name(colony.starSystem().id);
        if (colony.defense().troops() == 0)
            return text("INVASION_WIN", invaders, name);
        else if (transport.size() == 0)
            return text("INVASION_LOSS", defenders, name);
        else {
            if (colony.empire() == transport.empire())
                return text("INVASION_BATTLE_REBELS", str(year), invaders, name);
            else
                return text("INVASION_BATTLE", str(year), invaders, defenders, name);
        }
    }
    private boolean landing() {
        for (LandingShip ship: ships) {
            if (ship.landing())
                return true;
        }
        return false;
    }
    private boolean battleInProgress() {
        return colony.defense().troops() > 0 && transport.size() > 0;
    }
    private void advanceScreen() {
        if (landing()) {
            if (shipLanding != null)
                shipLanding.endPlaying();
            for (LandingShip ship: ships)
                ship.forceLand();
        }
        else if (battleInProgress()) {
            colony.completeDefenseAgainstTransports(transport);
            int defendersKilled = remainingDefenders.size() - colony.defense().troops();
            int attackersKilled = remainingAttackers.size() - transport.size();
            for (int i=0;i<attackersKilled;i++)
                assignRandomVictim(true);
            for (int i=0;i<defendersKilled;i++)
                assignRandomVictim(false);
            allStopFiring();
            log("ending with defenders:"+totalDefenders);
            repaint();
        }
        else {
            exited = true;
            repaint();
            session().resumeNextTurnProcessing();
        }
    }
    private void drawTroops(Graphics2D g, int troopCount, boolean attack, int x, int y) {
//        log("DrawTroops. count:"+troopCount+" attacker?"+attack+"  x:"+x+"  y:"+y);
        int MAX_PER_ROW = 25;
        int  NUM_ROWS = 6;
        int MAX_ROWS = 20;
        // try to do 6 rows evenly, only go to more when more than 6*MAX_PER_ROW
        int PER_ROW = (int) Math.ceil(1.0* troopCount / NUM_ROWS);
        while ((PER_ROW > MAX_PER_ROW) && (NUM_ROWS < MAX_ROWS)) {
            NUM_ROWS++;
            PER_ROW = (int) Math.ceil(1.0*troopCount / NUM_ROWS);
        }
        BufferedImage iconImg;
        int imgW = attack ? attackerImgW : defenderImgW;
        int imgH = attack ? attackerImgH : defenderImgH;
        int rowH = attack ? attackerYSpacing : defenderYSpacing;
        int iconWSpacing = attack ? attackerXSpacing : defenderXSpacing;

        int iconWIncr = attack ? iconWSpacing : -iconWSpacing;
        int rowStartX = attack ? x : x+(PER_ROW*iconWSpacing)-iconWSpacing;
        int numRows = (troopCount + PER_ROW - 1) / PER_ROW;
        int y0 = y - (4 * rowH);

        //log("troopSize:"+troopSize+"  w: "+imgW+" h:"+imgH+"   nuwRows:"+numRows);
        int count = 0;
        for (int row=0; row<numRows; row++) {
            int rowCount = Math.min(PER_ROW, troopCount);
            int rowAdj = row % 2 == 0 ? s8 : 0;
            int x0 = rowStartX+rowAdj;
            for (int troop=0; troop<rowCount; troop++) {
                int yAdj = troop % 2 == 0 ? s10 : 0;
                if (attack) {
                    int frame = attackerState[count];   // ATTACKER_END_DYING... 0... ATTACKER_END_FIRING
                    if (frame > BEGIN_DYING) 
                        iconImg = frame < attackerFrames.size() ? attackerFrames.get(frame) : null;
                    else {
                        int deathFrame = 0-frame-1;
                        iconImg = deathFrame < attackerDeathFrames.size() ? attackerDeathFrames.get(0-frame-1) : null;
                    }

                    attackerX[count] = x0;
                    attackerY[count] = y0+yAdj;
                    //log("attacker #"+count+" frame:"+frame+"  x:"+x0+"  y:"+(y0+yAdj)+"  w:"+imgW+"  h:"+imgH);
                    g.drawImage(iconImg, x0, y0+yAdj, imgW, imgH, null);
                    if (frame == attackerFiringFrame)
                        fireAtDefender(g, count);
                }
                else {
                    int frame = defenderState[count];  // DEFENDER_END_DYING... 0... DEFENDER_END_FIRING
                    if (frame > BEGIN_DYING)
                        iconImg = frame < defenderDeathFrames.size() ? defenderDeathFrames.get(frame) : null;
                    else {
                        int deathFrame = 0-frame-1;
                        iconImg = deathFrame < defenderDeathFrames.size() ? defenderDeathFrames.get(0-frame-1) : null;
                    } 

                    defenderX[count] = x0;
                    defenderY[count] = y0+yAdj;
                    //log("defender #"+count+" frame:"+frame+"  x:"+x0+"  y:"+(y0+yAdj)+"  w:"+imgW+"  h:"+imgH);
                    g.drawImage(iconImg, x0, y0+yAdj, imgW, imgH, null);
                    if (frame == defenderFiringFrame)
                        fireAtAttacker(g, count);
                }
                x0 += iconWIncr;
                count++;
            }
            troopCount -= rowCount;
            y0 += rowH;
        }
    }
    private int fireAtDefender(Graphics2D g, int n) {
        attackerFired = true;
        int gunX = attackerX[n]+attackerGunX;
        int gunY = attackerY[n]+attackerGunY;

        // if defender dies this turn, ensure that
        // predecided victim is fired on at least once
        int victim;
        if (!attackerDead && deathThisTurn > 0) {
                victim = deathThisTurn;
                deathThisTurn = 0;
        }
        else
                victim = random(remainingDefenders);

        int victimX = defenderX[victim]-(defenderImgW/2);
        int victimY = defenderY[victim]+(defenderImgH/2);

        //log("firing at defender#"+victim+"  from:"+gunX+"@"+gunY+"  to:"+victimX+"@"+victimY+"  gun:"+attackerGunX+"@"+attackerGunY);
        attackerWeapon.drawEffect(g, gunX, gunY, victimX, victimY);
        return victim;
    }
    private int fireAtAttacker(Graphics2D g, int n) {
        defenderFired = true;
        // defender images flipped horizontally, calc GunX from right side
        int gunX = defenderX[n]-defenderGunX;
        int gunY = defenderY[n]+defenderGunY;

        // if attacker dies this turn, ensure that
        // predetermined victim is fired on at least once
        int victim;
        if (attackerDead && deathThisTurn > 0) {
            victim = deathThisTurn;
            deathThisTurn = 0;
        }
        else
            victim = random(remainingAttackers);

        int victimX = attackerX[victim]+(attackerImgW/2);
        int victimY = attackerY[victim]+(attackerImgH/2);

        //log("firing at attacker#"+victim+"  from:"+gunX+"@"+gunY+"  to:"+victimX+"@"+victimY+"  gun:"+defenderGunX+"@"+defenderGunY);
        defenderWeapon.drawEffect(g, gunX, gunY, victimX, victimY);
        return victim;
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        advanceScreen();
    }
    private class LandingShip implements Base {
        int countDelay = 0;
        Race race;
        BufferedImage shipClosed;
        int shipX, shipEndY, dispW, dispH;
        int descIndex = 0;
        int openIndex = 0;
        int numLandingFrames = 10;
        int dropPerFrame = 1;
        int endTopY = 0;
        int startY = 0;

        public LandingShip(int n) {
            race = transport.empire().race();
            numLandingFrames = race.transportLandingFrames / 5;
            shipClosed = newBufferedImage(race.transportDescending());

            shipX = scaled(colony.planet().type().shipX(n+1));
            shipEndY = scaled(colony.planet().type().shipY(n+1));
            dispW = scaled(colony.planet().type().shipW(n+1));
            dispH = shipClosed.getHeight()*dispW/shipClosed.getWidth();

            dropPerFrame = (int) Math.ceil(1.0*shipEndY/numLandingFrames);
            endTopY = shipEndY-dispH;
            startY = endTopY-(dropPerFrame*(numLandingFrames+countDelay));
            //log("startY:"+startY+"  endTopY:"+endTopY+"  numFrames:"+numLandingFrames+"  dropPerFrame:"+dropPerFrame);
            countDelay = roll(0,20);
        }
        public void forceLand() {
            landingCount = max(landingCount, (numLandingFrames+countDelay));
        }
        public boolean landing() {
            return landingCount < (numLandingFrames+countDelay);
        }
        public void draw(Graphics g) {
            int thisY = min(endTopY, startY+(landingCount*dropPerFrame));
            // draw landing ship
            Image shipImg = thisY == endTopY ? nextOpeningShipImage() : nextDescendingShipImage();

            int imgW = shipImg.getWidth(null);
            int imgH = shipImg.getHeight(null);
            g.drawImage(shipImg, shipX, thisY, shipX+dispW, thisY+dispH, 0, 0, imgW, imgH, null);
        }
        private Image nextDescendingShipImage() {
            int frame = descIndex++;
            for (int i=0;i<descendingFrameRefs.size();i++) {
                if (frame < descendingFrameRefs.get(i))
                    return descendingFrames.get(i);
                frame -= descendingFrameRefs.get(i);
            }      
            return descendingFrames.get(descendingFrames.size()-1);
        }
        private Image nextOpeningShipImage() {
            int frame = openIndex++;
            for (int i=0;i<openingFrameRefs.size();i++) {
                if (frame < openingFrameRefs.get(i))
                    return openingFrames.get(i);
                frame -= openingFrameRefs.get(i);
            }      
            return openingFrames.get(openingFrames.size()-1);
        }
    }
}
