package simulation.beefs.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.EventSourceMultiplexer;
import manelsim.Time;
import manelsim.Time.Unit;
import manelsim.TimeInterval;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.model.Machine.State;

/**
 * 
 * @author Patrick Maia
 *
 */
public class MachineTransitionsFromIdleTest {
	
	private Machine machine;
	
	private Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private Time IDLENESS_DURATION = new Time(5*60, Unit.SECONDS);
	
	@Before
	public void setup() {
		machine = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		EventSourceMultiplexer eventsMultiplexer = new EventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);
		
		machine.setIdle(Time.GENESIS, IDLENESS_DURATION);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionFromIdleToIdle() {
		machine.setIdle(IDLENESS_DURATION, IDLENESS_DURATION);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testWakeOnLan() {
		machine.wakeOnLan(Time.GENESIS);
	}
	
	@Test
	public void testTransitionFromIdleToActive() {
		Time activityDuration = new Time(2, Unit.SECONDS);
		machine.setActive(IDLENESS_DURATION, activityDuration);
		
		assertEquals(State.ACTIVE, machine.state());
		assertEquals(0, machine.sleepIntervals().size());
		assertEquals(0, machine.transitionIntervals().size());
		assertEquals(1, machine.userActivityIntervals().size());
		assertEquals(1, machine.userIdlenessIntervals().size());
		
		TimeInterval expectedInterval = new TimeInterval(Time.GENESIS, IDLENESS_DURATION);
		assertTrue(machine.userIdlenessIntervals().contains(expectedInterval));
		expectedInterval = new TimeInterval(IDLENESS_DURATION, IDLENESS_DURATION.plus(activityDuration));
		assertTrue(machine.userActivityIntervals().contains(expectedInterval));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionFromIdleToActive() {
		Time oneSecond = new Time(1, Unit.SECONDS);
		machine.setActive(IDLENESS_DURATION.plus(oneSecond), oneSecond);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testTransitionFromIdleToSleep1() {
		Time sleepDuration = new Time(5*60, Unit.SECONDS);
		// this transition is illegal because the machine current idleness duration is less than TO_SLEEP_TIMEOUT
		machine.setSleeping(IDLENESS_DURATION, sleepDuration);
	}
	
	@Test
	public void testTransitionFromIdleToSleep2() {
		Machine machine = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machine.setIdle(Time.GENESIS, TO_SLEEP_TIMEOUT.plus(new Time(1, Unit.SECONDS)));
		
		//this transition is legal because the machine current idleness duration is greater than TO_SLEEP_TIMEOUT
		machine.setSleeping(TO_SLEEP_TIMEOUT, new Time(5*60, Unit.SECONDS));
		
		assertEquals(State.GOING_SLEEP, machine.state());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonContiguousTransitionFromIdleToSleep() {
		Time oneSecond = new Time(1, Unit.SECONDS);
		Machine machine = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machine.setIdle(Time.GENESIS, TO_SLEEP_TIMEOUT.plus(oneSecond));
		
		machine.setSleeping(TO_SLEEP_TIMEOUT.plus(oneSecond), oneSecond);
	}
	
}
