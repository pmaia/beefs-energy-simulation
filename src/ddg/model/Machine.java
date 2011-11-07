package ddg.model;

import java.util.ArrayList;
import java.util.List;

import ddg.emulator.EmulatorControl;
import ddg.emulator.event.machine.Sleep;
import ddg.emulator.event.machine.UserIdlenessStart;
import ddg.emulator.event.machine.WakeUp;
import ddg.kernel.Event;
import ddg.kernel.EventHandler;
import ddg.kernel.EventScheduler;
import ddg.kernel.Time;
import ddg.model.data.DataServer;

/**
 * Models a machine. This machine can hold a number of clients and data servers.
 * 
 * @author Ricardo Araujo Santos - ricardo@lsd.ufcg.edu.br
 * @author Thiago Emmanuel Pereira da Cunha Silva - thiagoepdc@lsd.ufcg.edu.br
 */
public class Machine extends EventHandler {

	/*
	 * The source of the values below is Lesandro's work: 
	 * "On the Impact of Energy-saving Strategies in Opportunistic Grids"
	 */
	public static final double TRANSITION_POWER_IN_WATTS = 140;
	public static final double ACTIVE_POWER_IN_WATTS = 140;
	public static final double STAND_BY_POWER_IN_WATTS = 3.33;
	public static final long TRANSITION_DURATION_IN_MILLISECONDS = 2500;
	
	private final List<DataServer> deployedDataServers; //FIXME this should be a Set
	private final List<DDGClient> clients; //FIXME this should be a Set
	
	private boolean sleeping;
	private Time lastStateTransition;
	/**
	 * The time in the simulation in which the idleness period ends
	 */
	private Time idlenessEnd;
	/**
	 * The amount of time this machine must wait idle before sleep
	 */
	private final Time timeBeforeSleep;
	
	private final String id;

	/**
	 * 
	 * @param scheduler
	 * @param id
	 * @param sleeping
	 * @param timeBeforeSleep the amount of time in seconds this machine should remain idle before sleep
	 */
	public Machine(EventScheduler scheduler, String id, long timeBeforeSleep) {
		super(scheduler);
		
		this.id = id;
		this.deployedDataServers = new ArrayList<DataServer>();
		this.clients = new ArrayList<DDGClient>();
		this.timeBeforeSleep = new Time(timeBeforeSleep * 1000);
		this.lastStateTransition = scheduler.now();
	}
	
	public boolean isSleeping() {
		return sleeping;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the deployedDataServers
	 */
	public List<DataServer> getDeployedDataServers() {
		return deployedDataServers;
	}

	/**
	 * @return
	 */
	public List<DDGClient> getDeployedClients() {
		return clients;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Machine other = (Machine) obj;
		if (id != other.id)
			return false;
		return true;
	}

	/**
	 * @param newClient
	 * @return
	 */
	public int bindClient(DDGClient newClient) {
		clients.add(newClient);
		return clients.indexOf(newClient);
	}

	/**
	 * @param newDataServer
	 * @return
	 */
	public int deploy(DataServer newDataServer) {
		deployedDataServers.add(newDataServer);
		return deployedDataServers.indexOf(newDataServer);
	}

	/**
	 * @param newDataServer
	 */
	public boolean isDeployed(DataServer dataServer) {
		return deployedDataServers.contains(dataServer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "machine" + id;
	}
	
	@Override
	public void handleEvent(Event event) {
		if(event instanceof UserIdlenessStart) {
			handleUserIdlenessStart((UserIdlenessStart) event);
		} else if(event instanceof WakeUp) {
			handleWakeUp((WakeUp) event);
		} else if(event instanceof Sleep) {
			handleSleep((Sleep) event);
		} else {
			throw new IllegalArgumentException();
		}
		
		EmulatorControl.getInstance().scheduleNext();
	}
	
	private void handleWakeUp(WakeUp event) {
		Time now = getScheduler().now();
		
		if(event.isUserIdle()) {
			if(now.plus(timeBeforeSleep).isEarlierThan(idlenessEnd)) {
				Time bedTime = now.plus(timeBeforeSleep);
				send(new Sleep(this, bedTime));
			}
		} else {
			this.idlenessEnd = null;
		}
		
		if(this.sleeping) {
			Aggregator.getInstance().
				aggregateSleepingDuration(getId(), now.minus(lastStateTransition).asMilliseconds());
			this.sleeping = false;
			this.lastStateTransition = now;
		}
	}
	
	private void handleUserIdlenessStart(UserIdlenessStart event) {
		Time idlenessDuration = new Time(event.getIdlenessDuration() * 1000);
		Time now = getScheduler().now();
		
		if(!idlenessDuration.isEarlierThan(timeBeforeSleep)) {
			Time bedTime = now.plus(timeBeforeSleep);
			send(new Sleep(this, bedTime));
			Time wakeUpTime = now.plus(idlenessDuration);
			send(new WakeUp(this, wakeUpTime, false));
		}
		
		this.idlenessEnd = now.plus(idlenessDuration);
	}
	
	private void handleSleep(Sleep event) {
		Time now = getScheduler().now();
		
		if(!this.sleeping) {
			Aggregator.getInstance().
				aggregateActiveDuration(getId(), now.minus(lastStateTransition).asMilliseconds());
			this.sleeping = true;
			this.lastStateTransition = now;
		}
	}
}
