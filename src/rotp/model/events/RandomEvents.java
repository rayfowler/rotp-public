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
package rotp.model.events;

import rotp.model.empires.Empire;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEvents implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final float START_CHANCE = 0.0f;
    private static final float CHANCE_INCR = 0.01f;
    private static final float MAX_CHANCE_INCR = 0.05f;
    public static final int START_TURN = 50;
    private List<RandomEvent> events;
    private List<RandomEvent> activeEvents;
    private float eventChance = START_CHANCE;

    public RandomEvents() {
        loadEvents();
    }
    public void addActiveEvent(RandomEvent ev)     { activeEvents.add(ev); }
    public void removeActiveEvent(RandomEvent ev)  { activeEvents.remove(ev); }
    public void nextTurn() {
        if (options().disableRandomEvents()) 
            return;

        // possible that next-turn logic may remove an active event
        List<RandomEvent> tempEvents = new ArrayList<>(activeEvents);
        for (RandomEvent ev: tempEvents)
            ev.nextTurn();

        int turnNum = galaxy().currentTurn();
        if (turnNum < START_TURN)
            return;

        if (events.isEmpty())
            return;

        eventChance = min(MAX_CHANCE_INCR, eventChance + CHANCE_INCR);
        if (random() > eventChance)
            return;

        RandomEvent triggeredEvent = random(events);
        
        if (turnNum < triggeredEvent.minimumTurn())
            return;
        
        events.remove(triggeredEvent);
        eventChance = START_CHANCE;

        Empire affectedEmpire = triggeredEvent.goodEvent() ? empireForGoodEvent() : empireForBadEvent();
        triggeredEvent.trigger(affectedEmpire);
    }
    public RandomEvent activeEventForKey(String key) {
        for (RandomEvent ev: activeEvents) {
            if (ev.systemKey().equals(key))
                return ev;
        }
        return null;
    }
    private void loadEvents() {
        activeEvents = new ArrayList<>();
        events = new ArrayList<>();
        events.add(new RandomEventDonation());
        events.add(new RandomEventDepletedPlanet());
        events.add(new RandomEventEnrichedPlanet());
        events.add(new RandomEventFertilePlanet());
        events.add(new RandomEventComputerVirus());
        events.add(new RandomEventEarthquake());
        events.add(new RandomEventIndustrialAccident());
        events.add(new RandomEventRebellion());
        events.add(new RandomEventAncientDerelict());
        events.add(new RandomEventAssassination());
        events.add(new RandomEventPlague());
        events.add(new RandomEventSupernova());
        events.add(new RandomEventPiracy());
        events.add(new RandomEventComet());
        events.add(new RandomEventSpaceAmoeba());
        events.add(new RandomEventSpaceCrystal());
    }
    private Empire empireForBadEvent() {
        // chance of empires for bad events is based power for each empire
        Empire[] emps = galaxy().empires();
        float[] vals = new float[emps.length];
        float total = 0.0f;
        for (int i=0;i<emps.length;i++) {
            Empire emp = emps[i];
            float power = emp.extinct() ? 0 : emp.industrialPowerLevel(emp);
            vals[i] = power;
            total += power;
        }

        float r = total * random();
        for (int i=0;i<emps.length;i++) {
            if (r <= vals[i])
                return emps[i];
            r -= vals[i];
        }

        // should never get here... if we do, have event affect the player
        return player();
    }
    private Empire empireForGoodEvent() {
        // chance of empires for good events is based 1/power for each empire
        Empire[] emps = galaxy().empires();
        float[] vals = new float[emps.length];
        float total = 0.0f;
        for (int i=0;i<emps.length;i++) {
            Empire emp = emps[i];
            float power = emp.extinct() ? 0 : emp.industrialPowerLevel(emp);
            if (power > 0)
                power = 1/power;
            vals[i] = power;
            total += power;
        }

        float r = total * random();
        for (int i=0;i<emps.length;i++) {
            if (r <= vals[i])
                return emps[i];
            r -= vals[i];
        }

        // should never get here... if we do, have event affect the player
        return player();
    }
}
