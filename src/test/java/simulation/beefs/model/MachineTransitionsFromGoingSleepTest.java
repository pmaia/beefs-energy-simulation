package simulation.beefs.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Before;
import org.junit.Test;

import simulation.beefs.event.machine.WakeOnLan;
import simulation.beefs.model.Machine.State;
import simulation.beefs.util.ObservableEventSourceMultiplexer;

/**
 * 
 * @author Patrick Maia
 *
 */
public class MachineTransitionsFromGoingSleepTest {
	
	private final Time TO_SLEEP_TIMEOUT = new Time(15*60, Unit.SECONDS);
	private final Time TRANSITION_DURATION = new Time(2500, Unit.MILLISECONDS);
	private final Time ONE_MINUTE = new Time(60, Unit.SECONDS);
	private final Time ONE_SECOND = new Time(1, Unit.SECONDS);

	private ObservableEventSourceMultiplexer eventsMultiplexer;

	private Machine machineGoingSleep;
	
	@Before
	public void setup() {
		eventsMultiplexer = new ObservableEventSourceMultiplexer(new EventSource[0]);
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, eventsMultiplexer);

		machineGoingSleep = new Machine("jurupoca", TO_SLEEP_TIMEOUT, TRANSITION_DURATION);
		machineGoingSleep.setIdle(Time.GENESIS, TO_SLEEP_TIMEOUT.plus(ONE_MINUTE));
		machineGoingSleep.setSleeping(TO_SLEEP_TIMEOUT, ONE_MINUTE);
		assertEquals(State.GOING_SLEEP, machineGoingSleep.state());
	}

	@Test(expected=IllegalStateException.class)
	public void testTransitionToIdle() {
		machineGoingSleep.setIdle(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), ONE_MINUTE);
	}

	@Test
	public void testTransitionToSleep() {
		machineGoingSleep.setSleeping(TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION), ONE_MINUTE);
		assertEquals(State.SLEEPING, machineGoingSleep.state());
	}

	@Test(expected=IllegalStateException.class)
	public void testTransitionToActive() { 
		Time activityStart = TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION.minus(ONE_SECOND));
		machineGoingSleep.setActive(activityStart, ONE_MINUTE);
	}
	
	@Test
	public void testWakeOnLan() {
		machineGoingSleep.wakeOnLan(TO_SLEEP_TIMEOUT.plus(ONE_SECOND));
		
		Time transitionEnd = TO_SLEEP_TIMEOUT.plus(TRANSITION_DURATION);
		WakeOnLan wakeOnLan = new WakeOnLan(machineGoingSleep, transitionEnd);
		assertTrue(eventsMultiplexer.contains(wakeOnLan));
		
		machineGoingSleep.wakeOnLan(TO_SLEEP_TIMEOUT.plus(ONE_SECOND));
		assertEquals(1, eventsMultiplexer.howManyOf(wakeOnLan));
	}
}
