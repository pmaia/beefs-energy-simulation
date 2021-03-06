package simulation.beefs.event.filesystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import manelsim.Event;
import manelsim.EventSource;
import manelsim.Time;
import manelsim.Time.Unit;
import simulation.beefs.model.FileSystemClient;

public class FileSystemTraceEventSource implements EventSource {

	private final BufferedReader bufferedReader;

	private final FileSystemClient client;

	public FileSystemTraceEventSource(FileSystemClient client, InputStream traceStream) {
		this.bufferedReader = new BufferedReader(new InputStreamReader(traceStream));
		this.client = client;
	}

	@Override
	public Event getNextEvent() {

		String traceLine;

		try {
			traceLine = readNextLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (traceLine == null)
			return null;

		try {
			StringTokenizer tokenizer = new StringTokenizer(traceLine);
			String operation = tokenizer.nextToken();

			if (operation.equals("read")) {
				return parseReadEvent(tokenizer);
			} else if (operation.equals("write")) {
				return parseWriteEvent(tokenizer);
			} else if (operation.equals("close")) {
				return parseCloseEvent(tokenizer);
			} else if (operation.equals("unlink")) {
				return parseUnlinkEvent(tokenizer);
			} else {
				return getNextEvent();
			}
		} catch(Throwable t ) {
			System.err.println("Warning: Bad format line: " + traceLine);
			return getNextEvent();
		}
	}

	private String readNextLine() throws IOException {

		String readLine = null;

		while ((readLine = bufferedReader.readLine()) != null) {

			if (!readLine.trim().equals("") && !readLine.startsWith("#")) {
				return readLine;
			}

		}

		return readLine;
	}
	
	private Unlink parseUnlinkEvent(StringTokenizer tokenizer) {
		//begin-elapsed   fullpath
		
		Time time = parseTime(tokenizer.nextToken())[0];
		String targetPath = tokenizer.nextToken();
		
		return new Unlink(client, time, targetPath);
	}

	private Close parseCloseEvent(StringTokenizer tokenizer) {
		//begin-elapsed   fullpath

		Time time = parseTime(tokenizer.nextToken())[0];
		String targetPath = tokenizer.nextToken();

		return new Close(client, time, targetPath);
	}

	private final static int EXPECTED_NUM_TOKENS_READ = 3;
	
	private Read parseReadEvent(StringTokenizer tokenizer) {
		//begin-elapsed   fullpath        length
		
		//filePaths can have empty spaces
		int actualNumTokens = tokenizer.countTokens();
		
		Time [] timestampAndDuration = parseTime(tokenizer.nextToken());
		String filePath = parsePath(1 + (actualNumTokens - EXPECTED_NUM_TOKENS_READ), tokenizer);
		long length = Long.parseLong(tokenizer.nextToken());

		return new Read(client, timestampAndDuration[0], timestampAndDuration[1], filePath, length);
	}

	private final static int EXPECTED_NUM_TOKENS_WRITE = 4;
	
	private Write parseWriteEvent(StringTokenizer tokenizer) {
		//begin-elapsed   fullpath        bytes_transfered	file_size
		
		//filePaths can have empty spaces
		int actualNumTokens = tokenizer.countTokens();
		
		Time [] timestampAndDuration = parseTime(tokenizer.nextToken());
		
		String filePath = parsePath(1 + (actualNumTokens - EXPECTED_NUM_TOKENS_WRITE), tokenizer);
		long bytesTransfered = Long.parseLong(tokenizer.nextToken());
		long fileSize = Long.parseLong(tokenizer.nextToken());

		return new Write(client, timestampAndDuration[0], timestampAndDuration[1], filePath,
				bytesTransfered, fileSize); 
	}
	
	private String parsePath(int numTokens, StringTokenizer tokenizer) {
		
		if (numTokens < 0) {
			throw new IllegalArgumentException();
		}
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < numTokens; i++) {
			buffer.append(tokenizer.nextToken());
		}
		
		return buffer.toString();
	}
	
	private Time [] parseTime(String traceTimestamp) {
		Time [] parsedTimes = new Time[2];
		
		String [] timestampAndDuration = traceTimestamp.split("-");
		
		parsedTimes[0] = 
				new Time(Long.parseLong(timestampAndDuration[0]), Unit.MICROSECONDS);
		parsedTimes[1] =
				new Time(Long.parseLong(timestampAndDuration[1]), Unit.MICROSECONDS);
		
		return parsedTimes; 
	}

}
