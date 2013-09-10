package simulation.beefs.model;


public class FileReplica {
	private final DataServer dataServer;
	
	private final long size;
	
	private boolean deleted = false;
	
	private boolean consistent = true;
	
	public FileReplica(DataServer dataServer, long size) {
		this.dataServer = dataServer;
		this.size = size;
		
		dataServer.useDisk(size);
	}
	
	public boolean isConsistent() {
		return consistent;
	}
	
	public void invalidate() {
		consistent = false;
	}
	
	public void delete() {
		if(deleted) {
			throw new IllegalStateException("replica already deleted");
		}
		dataServer.cleanSpace(size);
		deleted = true;
	}
	
	public long size() {
		return size;
	}
	
	public DataServer dataServer() {
		return dataServer;
	}

}
