package simulation.beefs.model;

import java.util.ArrayList;
import java.util.List;

import manelsim.EventScheduler;
import manelsim.Time;
import manelsim.TimeInterval;
import simulation.beefs.event.machine.Sleep;
import simulation.beefs.event.machine.UserActivity;
import simulation.beefs.event.machine.UserIdleness;
import simulation.beefs.event.machine.WakeOnLan;

public class Machine {
	
	public enum State {
		IDLE,
		ACTIVE,
		SLEEPING,
		GOING_SLEEP,
		WAKING_UP,
		BOOTSTRAP
	}
	
	public static class MachineStateInterval {
		private final State state;
		private final TimeInterval interval;
		public MachineStateInterval(State state, TimeInterval interval) {
			this.state = state;
			this.interval = interval;
		}
		public State getState() {
			return state;
		}
		public TimeInterval getInterval() {
			return interval;
		}
	}
	
	private interface MachineState {
		MachineState toActive(TimeInterval interval);
		MachineState toIdle(TimeInterval interval);
		MachineState toSleep(TimeInterval interval);
		MachineState wakeOnLan(Time when);
		State state();
		Time transitionTime();
	}

	private MachineState currentState;
	
	private Time currentDelay = Time.GENESIS;
	
	private final String hostname;
	
	private final Time toSleepTimeout;

	private final Time transitionDuration;
	
	private final List<MachineStateInterval> stateIntervals = new ArrayList<MachineStateInterval>();
	
	public Machine(String hostname, Time toSleepTimeout, Time transitionDuration) {
		this.hostname = hostname;
		this.toSleepTimeout = toSleepTimeout;
		this.transitionDuration = transitionDuration;
		this.currentState = new Bootstrap();
	}
	
	public String name() {
		return hostname;
	}

	public List<TimeInterval> userActivityIntervals() {
		State [] states = {State.ACTIVE};
		return getIntervals(states);
	}
	
	public List<TimeInterval> userIdlenessIntervals() {
		State [] states = {State.IDLE};
		return getIntervals(states);
	}

	public List<TimeInterval> transitionIntervals() {
		State [] states = {State.GOING_SLEEP, State.WAKING_UP};
		return getIntervals(states);
	}

	public List<TimeInterval> sleepIntervals() {
		State [] states = {State.SLEEPING};
		return getIntervals(states);
	}
	
	public List<MachineStateInterval> stateIntervals() {
		return new ArrayList<MachineStateInterval>(stateIntervals);
	}
	
	private List<TimeInterval> getIntervals(State [] states) {
		List<TimeInterval> intervals = new ArrayList<TimeInterval>();
		for(MachineStateInterval stateInterval : stateIntervals) {
			for(State state : states) {
				if(stateInterval.getState() == state) {
					intervals.add(stateInterval.getInterval());
					break;
				}
			}
		}
		return intervals;
	}
	
	public boolean isReachable() {
		return (currentState.state() == State.ACTIVE || currentState.state() == State.IDLE);
	}
	
	public Time lastTransitionTime() {
		return currentState.transitionTime();
	}

	public Time transitionDuration() {
		return transitionDuration;
	}
	
	public Time currentDelay() {
		return currentDelay;
	}

	public void setActive(Time begin, Time duration) {
		TimeInterval interval = new TimeInterval(begin, begin.plus(duration));
		currentState = currentState.toActive(interval);
	}
	
	public void setIdle(Time begin, Time duration) {
		TimeInterval interval = new TimeInterval(begin, begin.plus(duration));
		currentState = currentState.toIdle(interval);
	}
	
	public void setSleeping(Time begin, Time duration) {
		TimeInterval interval = new TimeInterval(begin, begin.plus(duration));
		currentState = currentState.toSleep(interval);
	}
	
	public void wakeOnLan(Time when) {
		currentState = currentState.wakeOnLan(when);
	}
	
	public State state() {
		return currentState.state();
	}
	
	private void checkContinuity(TimeInterval next) {
		TimeInterval last = stateIntervals.get(stateIntervals.size() - 1).getInterval();
		if(!last.isContiguous(next)) {
			String msg = String.format("The interval duration of the next state must be contiguous to the " +
					"interval duration of the current state. Current interval is %s. You tried this %s. %s", 
					last, next, machineInformation());
			throw new IllegalArgumentException(msg);
		}
	}
	
	private String machineInformation() {
		return String.format("Machine: %s. Current delay %s", hostname, currentDelay);
	}
	
	// the next four methods are used by State implementations to schedule new events
	private void scheduleSleep(Time begin, Time duration) {
		EventScheduler.schedule(new Sleep(this, begin, duration));
	}
	
	private void scheduleUserActivity(Time begin, Time duration) {
		EventScheduler.schedule(new UserActivity(this, begin, duration, false));
	}
	
	private void scheduleUserIdleness(Time begin, Time duration) {
		EventScheduler.schedule(new UserIdleness(this, begin, duration, false));
	}
	
	private void scheduleWakeOnLan(Time when) {
		EventScheduler.schedule(new WakeOnLan(this, when));
	}
	//
	
	private class Bootstrap implements MachineState {
		public MachineState toActive(TimeInterval interval) {
			return new Active(interval);
		}
		public MachineState toIdle(TimeInterval interval) {
			return new Idle(interval);
		}
		public MachineState toSleep(TimeInterval interval) {
			throw new IllegalStateException("transition to IDLE is expected. " + machineInformation());
		}
		public MachineState wakeOnLan(Time when) {
			throw new IllegalStateException("transition to IDLE is expected. " + machineInformation());
		}
		public State state() {
			return State.BOOTSTRAP;
		}
		public Time transitionTime() {
			return null;
		}
	}
	
	private class Idle implements MachineState {
		
		private boolean sleepIsExpected = false;
		private final Time transitionTime;
		
		public Idle(TimeInterval interval) {
			if(toSleepTimeout.isEarlierThan(interval.delta())) { // then, schedule a sleep event on now + toSleepTimeout
				Time sleepBegin = interval.begin().plus(toSleepTimeout);
				scheduleSleep(sleepBegin, interval.end().minus(sleepBegin));
				sleepIsExpected = true;
				interval = new TimeInterval(interval.begin(), interval.begin().plus(toSleepTimeout));
			}
			transitionTime = interval.begin();
			stateIntervals.add(new MachineStateInterval(State.IDLE, interval));
		}
		public MachineState toActive(TimeInterval interval) {
			if(sleepIsExpected) {
				throw new IllegalStateException("transition to SLEEP is expected. " + machineInformation());
			}
			checkContinuity(interval); 
			return new Active(interval);
		}
		public MachineState toIdle(TimeInterval interval) {
			throw new IllegalStateException("This machine is already IDLE. " + machineInformation());
		}
		public MachineState toSleep(TimeInterval interval) {
			if(!sleepIsExpected) {
				throw new IllegalStateException("transition to ACTIVE is expected " + machineInformation());
			}
			checkContinuity(interval);
			
			Time sleepDuration =  Time.max(interval.delta().minus(transitionDuration), Time.GENESIS);
			scheduleSleep(interval.begin().plus(transitionDuration), sleepDuration);
			
			Time delayIncrement = Time.max(transitionDuration.minus(interval.delta()), Time.GENESIS); 
			
			return new GoingSleep(interval.begin(), delayIncrement);
		}
		public MachineState wakeOnLan(Time when) {
			throw new IllegalStateException("This machine is not sleeping. " + machineInformation());
		}
		public State state() {
			return State.IDLE;
		}
		public Time transitionTime() {
			return transitionTime;
		}
	}
	
	private class Active implements MachineState {
		private final Time transitionTime;
		public Active(TimeInterval interval) {
			transitionTime = interval.begin();
			stateIntervals.add(new MachineStateInterval(State.ACTIVE, interval));
		}
		public MachineState toActive(TimeInterval interval) {
			throw new IllegalStateException("This machine is already ACTIVE. " + machineInformation());
		}
		public MachineState toIdle(TimeInterval interval) {
			checkContinuity(interval);
			return new Idle(interval);
		}
		public MachineState toSleep(TimeInterval interval) {
			throw new IllegalStateException("Transition to IDLE is expected. " + machineInformation());
		}
		public MachineState wakeOnLan(Time when) {
			throw new IllegalStateException("This machine is not sleeping. " + machineInformation());
		}
		public State state() {
			return State.ACTIVE;
		}
		public Time transitionTime() {
			return transitionTime;
		}
	}
	
	private class Sleeping implements MachineState {
		private final Time transitionTime;
		public Sleeping(TimeInterval interval) {
			transitionTime = interval.begin();
			stateIntervals.add(new MachineStateInterval(State.SLEEPING, interval));
		}
		public MachineState toActive(TimeInterval interval) {
			checkContinuity(interval);
			scheduleUserActivity(interval.begin().plus(transitionDuration), interval.delta());
			return new WakingUp(interval.begin(), transitionDuration, false);
		}
		public MachineState toIdle(TimeInterval interval) {
			throw new IllegalStateException("Transition to ACTIVE or WakeOnLan are expected. " + machineInformation());
		}
		public MachineState toSleep(TimeInterval interval) {
			throw new IllegalStateException("Transition to ACTIVE or WakeOnLan are expected. " + machineInformation());
		}
		public MachineState wakeOnLan(Time now) {
			/*
			 *  adjusts the time interval the machine really slept
			 */
			int lastElementIndex = stateIntervals.size() - 1;
			TimeInterval shouldSleepInterval = stateIntervals.get(lastElementIndex).getInterval();
			if(shouldSleepInterval.end().isEarlierThan(now)) {
				throw new IllegalStateException("This machine should already be awake. " + machineInformation());
			}
			stateIntervals.remove(lastElementIndex);
			TimeInterval interval = new TimeInterval(shouldSleepInterval.begin(), now);
			stateIntervals.add(new MachineStateInterval(State.SLEEPING, interval));

			Time remainingSleepTime = shouldSleepInterval.end().minus(now);
			Time idlenessDuration = Time.max(remainingSleepTime.minus(transitionDuration), Time.GENESIS);
			
			if(idlenessDuration.equals(Time.GENESIS)) {
				Time delayIncrement = transitionDuration.minus(remainingSleepTime);
				return new WakingUp(now, delayIncrement, false);
			} else {
				/* 
				 * schedules a new UserIdleness event starting after the transition ends and lasting the same time this 
				 * machine should remain sleeping (before being disturbed) minus the transition duration.
				 */
				scheduleUserIdleness(now.plus(transitionDuration), idlenessDuration);
				return new WakingUp(now, Time.GENESIS, true);
			}
		}
		public State state() {
			return State.SLEEPING;
		}
		public Time transitionTime() {
			return transitionTime;
		}
	}
	
	private class GoingSleep implements MachineState {
		
		private final TimeInterval interval;
		private boolean wakeOnLanScheduled = false;
		
		public GoingSleep(Time time, Time delayIncrement) {
			interval = new TimeInterval(time, time.plus(transitionDuration));
			stateIntervals.add(new MachineStateInterval(State.GOING_SLEEP, interval));
			
			currentDelay = currentDelay.plus(delayIncrement);
		}
		public MachineState toActive(TimeInterval interval) {
			throw new IllegalStateException("Transition to SLEEPING is expected. " + machineInformation());
		}
		public MachineState toIdle(TimeInterval interval) {
			throw new IllegalStateException("Transition to SLEEPING is expected. " + machineInformation());
		}
		public MachineState toSleep(TimeInterval interval) {
			checkContinuity(interval);
			return new Sleeping(interval);
		}
		public MachineState wakeOnLan(Time when) {
			if(!wakeOnLanScheduled) {
				scheduleWakeOnLan(interval.end());
				wakeOnLanScheduled = true;
			}
			return this;
		}
		public State state() {
			return State.GOING_SLEEP;
		}
		public Time transitionTime() {
			return interval.begin();
		}
	}
	
	private class WakingUp implements MachineState {
		private final Time transitionTime;
		private final boolean expectTransitionToIdle;
		
		public WakingUp(Time time, Time delayIncrement, boolean expectTransitionToIdle) {
			transitionTime = time;
			TimeInterval transitionInterval = new TimeInterval(time, time.plus(transitionDuration));
			stateIntervals.add(new MachineStateInterval(State.WAKING_UP, transitionInterval));
			this.expectTransitionToIdle = expectTransitionToIdle;
			
			currentDelay = currentDelay.plus(delayIncrement);
		}
		public MachineState toActive(TimeInterval interval) {
			if(expectTransitionToIdle) {
				throw new IllegalStateException("Transition to IDLE is expected. " + machineInformation());
			}
			checkContinuity(interval);
			return new Active(interval);
		}
		public MachineState toIdle(TimeInterval interval) {
			if(!expectTransitionToIdle) {
				throw new IllegalStateException("Transition to ACTIVE is expected. " + machineInformation());
			}
			checkContinuity(interval);
			return new Idle(interval);
		}
		public MachineState toSleep(TimeInterval interval) {
			String nextState = expectTransitionToIdle ? "IDLE" : "ACTIVE";
			throw new IllegalStateException(String.format("Transition to %s is expected. %s", nextState, machineInformation()));
		}
		public MachineState wakeOnLan(Time when) {
			return this;
		}
		public State state() {
			return State.WAKING_UP;
		}
		public Time transitionTime() {
			return transitionTime;
		}
	}
	
}
