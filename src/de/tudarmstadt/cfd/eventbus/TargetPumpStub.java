package de.tudarmstadt.cfd.eventbus;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.WriteStream;
import org.vertx.java.deploy.Verticle;

public class TargetPumpStub implements Handler<Message<? extends Object>>, WriteStream{
	private final EventBus eb;
	private final String address_send;
	private final String address_recv;
	private final String ebId;

	private boolean isSourcePumpReady = false;
	private Buffer notReadyBuffer = null;
	
	private final Logger logger;

	private final Handler<Message<Buffer>> internalDrainHandler = new Handler<Message<Buffer>>() {
		@Override
		public void handle(Message<Buffer> event) {
			// Is called if target pump is ready
//			TargetPumpStub.this.logger.info("TARGETPUMP is empty");
			setTargetPumpEmpty();
		}
	};

	private final Handler<Void> endHandler = new Handler<Void>() {
		@Override
		public void handle(Void event) {
			TargetPumpStub.this.eb.send(address_send, PumpStates.END.name());
		}
	};


	private boolean targetPumpIsFull = true;
	private Handler<Void> drainHandler = null;

	private Handler<Exception> exceptionHandler = new Handler<Exception>() {
		@Override
		public void handle(Exception event) {
			event.printStackTrace();
		}
	};

	public TargetPumpStub(Verticle verticle, String address) {
		super();
		this.eb = verticle.getVertx().eventBus();
		this.logger = verticle.getContainer().getLogger();
		
		this.address_recv = address + "." + TargetPumpStub.class.getCanonicalName();
		this.address_send = address + "." + SourcePumpStub.class.getCanonicalName();
		this.ebId = this.eb.registerHandler(address_recv, this);

//		this.logger.info("TARGETPUMP send "+PumpStates.TARGETPUMPREADY.name());
		this.eb.send(address_send, PumpStates.TARGETPUMPREADY.name(), new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> event) {
				TargetPumpStub.this.setSourcePumpReady();
			}
		});
	}

	@Override
	public void handle(Message<? extends Object> event) {
		final Object payload = event.body;
//		this.logger.info("TARGETPUMP recv = "+payload);

		if(payload instanceof Buffer){
			writeBuffer((Buffer) payload);
		}else if(payload instanceof String){
			if(PumpStates.END.name().equals(payload)){
				this.eb.unregisterHandler(ebId);
				//				this.eb.send(address, PumpStates.END.name());
			}else if(PumpStates.SOURCEPUMPREADY.name().equals(payload)){
				this.setSourcePumpReady();
			}
			/*
			else if(PumpStates.SOURCEPUMPISFULL.name().equals(payload)){
				this.setTargetPumpFull();
			}else if(PumpStates.SOURCEPUMPISEMPTY.name().equals(payload)){
				this.setTargetPumpEmpty();
			}
			*/
		}else if(payload instanceof JsonObject){

		}else{
			exceptionHandler.handle(new IllegalStateException("event of type "+event.getClass().getCanonicalName()+" is unknown!"));
		}
	}

	private boolean isSourcePumpReady(){
		return isSourcePumpReady;
	}

	private void setSourcePumpReady(){
//		this.logger.info("TARGETPUMP sourcepump is ready");
		isSourcePumpReady = true;
		targetPumpIsFull = false;

		if(this.notReadyBuffer != null){
			wirteBufferOnEventbus(this.notReadyBuffer);
			this.notReadyBuffer = null;
		}
	}

	private void setTargetPumpFull(){
		this.targetPumpIsFull = true;
	}
	
	private void setTargetPumpEmpty() {
		TargetPumpStub.this.targetPumpIsFull = false;

		if(drainHandler != null){
			drainHandler.handle(null);
			drainHandler = null;
		}
	}

	@Override
	public void writeBuffer(Buffer data) {
//		this.logger.info("TARGETPUMP write");
		setTargetPumpFull();

		if(isSourcePumpReady()){
			wirteBufferOnEventbus(data);
		}else{
//			this.logger.info("TARGETPUMP cache data");
			this.notReadyBuffer = data;
		}
	}

	private void wirteBufferOnEventbus(Buffer data){
//		this.logger.info("TARGETPUMP write eventbus");
		this.eb.send(address_send, data, internalDrainHandler);
	}

	@Override
	public void setWriteQueueMaxSize(int maxSize) {
		//		this.eb.send(address, new JsonObject().putNumber(PumpStates.QUEUESIZE.name(), maxSize));
	}

	@Override
	public boolean writeQueueFull() {
		return this.targetPumpIsFull;
	}

	@Override
	public void drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
	}

	@Override
	public void exceptionHandler(Handler<Exception> handler) {
		this.exceptionHandler = handler;
	}

	public Handler<Void> getEndHandler(){
		return endHandler;
	}

}
