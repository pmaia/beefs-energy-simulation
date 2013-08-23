package simulation.beefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import manelsim.Context;
import manelsim.Summarizer;
import simulation.beefs.energy.EnergyConsumptionModel;
import simulation.beefs.energy.EnergyState;
import simulation.beefs.energy.EnergyStateInterval;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.Machine.MachineStateInterval;
import simulation.beefs.model.Machine.State;

public class BeefsEnergySimulationSummarizer implements Summarizer {
	
	private List<EnergyStateInterval> combine(DataServer dataServer) { 
		List<EnergyStateInterval> combinedEnergyStatesIntervals = 
				convertStates(dataServer.getHost().getStateIntervals());
		
		return combinedEnergyStatesIntervals;
	}
	
	private List<EnergyStateInterval> convertStates(List<MachineStateInterval> stateIntervals) {
		List<EnergyStateInterval> energyStatesIntervals = new ArrayList<EnergyStateInterval>();
		
		for(MachineStateInterval machineStateInterval : stateIntervals) {
			energyStatesIntervals.add(new EnergyStateInterval(convertState(machineStateInterval.getState()), 
					machineStateInterval.getInterval()));
		}
		
		return energyStatesIntervals;
	}

	private EnergyState convertState(State state) {
		EnergyState converted;
		switch(state) {
			case ACTIVE: converted = EnergyState.ACTIVE; break;
			case GOING_SLEEP: 
			case WAKING_UP: converted = EnergyState.TRANSITIONING; break;
			case IDLE: converted = EnergyState.IDLE; break;
			case SLEEPING: converted = EnergyState.SLEEPING; break;
			
			default: 
				throw new IllegalArgumentException("Could not convert " + state);
		}
		return converted;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String summarize(Context context) {
		
		Set<DataServer> dataServers = (Set<DataServer>) context.get(BeefsEnergySimulationConstants.DATA_SERVERS);
		StringBuffer sb = new StringBuffer();
		
		for(DataServer dataServer : dataServers) {
			List<EnergyStateInterval> energyStatesIntervals = combine(dataServer);
			
			EnergyConsumptionModel energyConsumptionModel = 
					(EnergyConsumptionModel)context.get(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL);
			double kWh = energyConsumptionModel.getConsumption(energyStatesIntervals);
			
			sb.append(String.format("$%s\t%f\t%d\t%d\n", 
					dataServer.getHost().getName(), 
					kWh, 
					dataServer.getHost().getTransitionIntervals().size(),
					dataServer.freeSpace()));
		}
		
		return sb.toString();
	} 

}
