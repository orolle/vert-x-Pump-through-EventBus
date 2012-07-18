package de.tudarmstadt.cfd.eventbus.test;

import org.vertx.java.core.streams.Pump;
import org.vertx.java.deploy.Verticle;

import de.tudarmstadt.cfd.eventbus.SourcePumpStub;
import de.tudarmstadt.cfd.eventbus.TargetPumpStub;

public class StartTest extends Verticle {

	@Override
	public void start() throws Exception {
		System.out.println("Start Pump through EventBus test.");
		
		SourceTest src = new SourceTest(StartTest.this.vertx);
		TargetTest tar = new TargetTest();
		String address = Math.random()+"";

		Pump p1 = Pump.createPump(src, new TargetPumpStub(StartTest.this, address));
		Pump p2 = Pump.createPump(new SourcePumpStub(StartTest.this, address), tar);

		p1.start();
		p2.start();

	}

}
