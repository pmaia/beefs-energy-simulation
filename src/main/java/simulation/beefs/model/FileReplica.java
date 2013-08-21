package simulation.beefs.model;


public class FileReplica {
	private final DataServer dataServer;
	
	private final long size;
	
	private boolean deleted = false;
	
	public FileReplica(DataServer dataServer, long size) {
		this.dataServer = dataServer;
		this.size = size;
		
		dataServer.useDisk(size);
	}
	
	public void delete() {
		if(deleted) {
			throw new IllegalStateException("replica already deleted");
		}
		dataServer.cleanSpace(size);
		deleted = true;
	}
	
	public DataServer dataServer() {
		return dataServer;
	}

}
