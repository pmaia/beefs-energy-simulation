/**
 * Copyright (C) 2009 Universidade Federal de Campina Grande
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ddg.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import ddg.emulator.EventSource;
import ddg.emulator.MultipleEventSource;
import ddg.emulator.event.filesystem.FileSystemEventParser;
import ddg.kernel.Event;
import ddg.kernel.EventScheduler;
import ddg.model.FileSystemClient;
import ddg.model.Machine;

/**
 * A suite of tests to the MultipleSourceEventParser class
 *
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class MultipleEventParserTest {
	
//	@Test
//	public void eventOrderingTest() {
//		EventSource [] parsers = new EventSource[3];
//		
//		InputStream trace1 = new FakeFileSystemTraceStream(0);
//		InputStream trace2 = new FakeFileSystemTraceStream(60);
//		InputStream trace3 = new FakeFileSystemTraceStream(30);
//		
//		EventScheduler scheduler = new EventScheduler();
//		
//		Machine machine1 = new Machine(scheduler, "cherne", 30 * 60);
//		Machine machine2 = new Machine(scheduler, "palhaco", 30 * 60);
//		Machine machine3 = new Machine(scheduler, "abelhinha", 30 * 60);
//		
//		FileSystemClient client1 = new FileSystemClient(scheduler, machine1, null);
//		FileSystemClient client2 = new FileSystemClient(scheduler, machine2, null);
//		FileSystemClient client3 = new FileSystemClient(scheduler, machine3, null);
//		
//		parsers[0] = new FileSystemEventParser(client1, trace1);
//		parsers[1] = new FileSystemEventParser(client2, trace2);
//		parsers[2] = new FileSystemEventParser(client3, trace3);
//		
//		EventSource multipleSourceParser = new MultipleEventSource(parsers);
//		
//		Event currentEvent = multipleSourceParser.getNextEvent();
//		Event nextEvent = null;
//		while((nextEvent = multipleSourceParser.getNextEvent()) != null) {
//			assertTrue(currentEvent.getScheduledTime().compareTo(nextEvent.getScheduledTime()) <= 0);
//			currentEvent = nextEvent;
//		}
//	}
//	
//	@Test
//	public void eventsDeliveredCountTest() {
//		EventSource [] parsers = new EventSource[3];
//		
//		InputStream trace1 = new FakeFileSystemTraceStream(50);
//		InputStream trace2 = new FakeFileSystemTraceStream(1000);
//		InputStream trace3 = new FakeFileSystemTraceStream(50);
//		
//		EventScheduler scheduler = new EventScheduler();
//		
//		Machine machine1 = new Machine(scheduler, "cherne", 30 * 60);
//		Machine machine2 = new Machine(scheduler, "palhaco", 30 * 60);
//		Machine machine3 = new Machine(scheduler, "abelhinha", 30 * 60);
//		
//		FileSystemClient client1 = new FileSystemClient(scheduler, machine1, null);
//		FileSystemClient client2 = new FileSystemClient(scheduler, machine2, null);
//		FileSystemClient client3 = new FileSystemClient(scheduler, machine3, null);
//		
//		parsers[0] = new FileSystemEventParser(client1, trace1);
//		parsers[1] = new FileSystemEventParser(client2, trace2);
//		parsers[2] = new FileSystemEventParser(client3, trace3);
//		
//		EventSource multipleSourceParser = new MultipleEventSource(parsers);
//		
//		int eventCount = 0;
//		while(multipleSourceParser.getNextEvent() != null) {
//			eventCount++;
//		}
//		
//		assertEquals(1100, eventCount);
//	}
//	 
//	public static void main(String[] args) throws IOException {
//		FakeFileSystemTraceStream res = new FakeFileSystemTraceStream(1000000);
//		BufferedReader br = new BufferedReader(new InputStreamReader(res));
//		String line;
//		while((line = br.readLine()) != null) {
//			System.out.println(line);
//		}
//	}
	
}
