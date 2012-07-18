package de.tudarmstadt.cfd.eventbus.test;

import java.util.Formatter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;
import org.vertx.java.deploy.Verticle;

public class TargetTest implements WriteStream {
	int written = 0;
	int maxWritte = 500;
	
	long begin, end;
			
	@Override
	public void writeBuffer(Buffer data) {
		if(written==0){
			begin = System.currentTimeMillis();
		}
		
		//System.out.println("-TARGETTEST write: "+data.toString());
		written ++;
	}

	@Override
	public void setWriteQueueMaxSize(int maxSize) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean writeQueueFull() {
		boolean isAtEnd = written >= maxWritte;
		
		if(isAtEnd){
			end = System.currentTimeMillis();
			long timeMs = end - begin;
			double callsPerSec = ((double) written / (double) timeMs) * 1000;
			
			Formatter format = new Formatter();
			
			System.out.println("writeBuffer() called " + written + " times. " + format.format("%.2f", callsPerSec) + " writeBuffer() calls per second");
			System.out.println("Test finished.");
		}
		
		return isAtEnd;
	}

	@Override
	public void drainHandler(Handler<Void> handler) {
		// TODO Auto-generated method stub
		System.out.println("-TARGETTEST set drainHandler");
	}

	@Override
	public void exceptionHandler(Handler<Exception> handler) {
		// TODO Auto-generated method stub
		
	}
}
