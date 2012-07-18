package de.tudarmstadt.cfd.eventbus.test;

import java.util.Date;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.deploy.Verticle;

public class SourceTest implements ReadStream {
	private Handler<Buffer> dataHandler, backupDataHandler=null;

	public SourceTest(Vertx vertx){
		vertx.setPeriodic(-1, new Handler<Long>() {
			int i = 0;

			@Override
			public void handle(Long event) {
				final String msg = i++ + "";

				if(SourceTest.this.dataHandler != null){
					//System.out.println("-SOURCETEST read: "+msg);

					SourceTest.this.dataHandler.handle(new Buffer(msg));
				}
			}
		});
	}

	@Override
	public void dataHandler(Handler<Buffer> handler) {
		this.dataHandler = handler;
	}

	@Override
	public void pause() {
		this.backupDataHandler = this.dataHandler;
		this.dataHandler = null;
//		System.out.println("-SOURCETEST pause");
	}

	@Override
	public void resume() {
		this.dataHandler = backupDataHandler;
		this.backupDataHandler = null;
//		System.out.println("-SOURCETEST resume");
	}

	@Override
	public void exceptionHandler(Handler<Exception> handler) {

	}

	@Override
	public void endHandler(Handler<Void> endHandler) {

	}

}
