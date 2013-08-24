package simulation.beefs.event.filesystem;

import manelsim.Time;
import simulation.beefs.event.MachineDelaybleEvent;
import simulation.beefs.model.FileSystemClient;

public class Close extends MachineDelaybleEvent {
	
	private final String filePath;
	private final FileSystemClient client;

	public Close(FileSystemClient client, Time scheduledTime, String filePath) {
		super(client.host(), scheduledTime, true);
		
		this.filePath = filePath;
		this.client = client;
	}

	@Override
	public void process() {
		client.close(filePath);
	}

}
