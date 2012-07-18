package de.tudarmstadt.cfd.eventbus;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.deploy.Verticle;

public class SourcePumpStub implements Handler<Message<? extends Object>>, ReadStream{
	private final EventBus eb;
	private final String address_send;
	private final String address_recv;
	private final String ebId;
	private final Logger logger;
	
	private Message<? extends Object> freezedMsg = null;
	private Handler<Buffer> dataHandler = null, backupDataHandler = null;
	private final Handler<Buffer> dummyDataHandler = new Handler<Buffer>() {
		@Override
		public void handle(Buffer event) {
//			SourcePumpStub.this.logger.info("DummyDataHandler received data! should not happen! data="+event.toString());
		}
	};
	private Handler<Void> endHandler = null;
	
	private Handler<Exception> exceptionHandler = new Handler<Exception>() {
		@Override
		public void handle(Exception event) {
			event.printStackTrace();
		}
	};
	
	public SourcePumpStub(Verticle verticle, String address) {
		super();
		this.eb = verticle.getVertx().eventBus();
		this.logger = verticle.getContainer().getLogger();
		this.address_send = address + "." + TargetPumpStub.class.getCanonicalName();
		this.address_recv = address + "." + SourcePumpStub.class.getCanonicalName();
		
		this.ebId = this.eb.registerHandler(address_recv, this);
		
		this.pause();
		//this.logger.info("SOURCEPUMP send "+PumpStates.SOURCEPUMPREADY.name());
		this.eb.send(address_send, PumpStates.SOURCEPUMPREADY.name(), new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> event) {
//				SourcePumpStub.this.logger.info("SOURCEPUMP recv targetpump is ready");
				setTargetPumpReady();
			}
		});
	}

	@Override
	public void handle(Message<? extends Object> event) {
		final Object payload = event.body;
//		this.logger.info("SOURCEPUMP recv = "+payload);

		if(payload instanceof Buffer){
			handleDataBuffer(event, (Buffer) payload);
		}else if(payload instanceof String){
			if(PumpStates.END.name().equals(payload)){
				this.eb.unregisterHandler(ebId);
//				this.eb.send(address, PumpStates.END.name());
				endHandler.handle(null);
			}else if(PumpStates.TARGETPUMPREADY.name().equals(payload)){
				setTargetPumpReady();
				event.reply(null);
			}
		}else if(payload instanceof JsonObject){
			
		}else{
			exceptionHandler.handle(new IllegalStateException("event of type "+event.getClass().getCanonicalName()+" is unknown!"));
		}
	}
	
	private void handleDataBuffer(Message<? extends Object> msg, Buffer data) {
//		this.logger.info("SOURCEPUMP write");
		this.dataHandler.handle(data);
		if(dataHandler == dummyDataHandler){
			this.freezedMsg = msg;
		}else{
			msg.reply(null);
		}
	}

	/**
	 * is called if TargetPump is ready
	 * resume reading
	 */
	private void setTargetPumpReady(){
		resume();
	}

	@Override
	public void dataHandler(Handler<Buffer> handler) {
		dataHandler = handler;
	}

	@Override
	public void pause() {
//		this.logger.info("SOURCEPUMP pause");
		if(backupDataHandler == null){
//			this.eb.send(address_send, PumpStates.SOURCEPUMPISFULL.name());
			backupDataHandler = dataHandler;
		}
		dataHandler = dummyDataHandler;
	}

	@Override
	public void resume() {
//		this.logger.info("SOURCEPUMP resume");
		if(backupDataHandler != null){
			dataHandler = backupDataHandler;
			backupDataHandler = null;
//			this.eb.send(address_send, PumpStates.SOURCEPUMPISEMPTY.name());
		}
		
		if(freezedMsg != null){
			freezedMsg.reply(null);
			freezedMsg = null;
		}
	}

	@Override
	public void exceptionHandler(Handler<Exception> handler) {
		this.exceptionHandler = handler;
	}

	@Override
	public void endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
	}

}
