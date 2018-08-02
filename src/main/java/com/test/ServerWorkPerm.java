package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

/**
 *
 *
 *
 *
 *
 * <p>
 * 修改历史:                                                                                    &lt;br&gt;
 * 修改日期             修改人员        版本                     修改内容
 * --------------------------------------------------------------------
 * 2018年08月02日 下午12:02   shikai.liu     1.0   初始化创建
 * </p>
 *
 * @author shikai.liu
 * @version 1.0
 * @since JDK1.7
 */
public class ServerWorkPerm implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(ServerWorkPerm.class);


	private ZContext ctx;

	public ServerWorkPerm(ZContext ctx) {
		this.ctx = ctx;
	}

	@Override public void run() {
		Socket worker = ctx.createSocket(ZMQ.REP);

		//	worker.connect("tcp://*:5556");
		worker.connect("inproc://backend");

		while (!Thread.currentThread().isInterrupted()) {
			try {
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame zFrame=msg.getLast();
				String reqBody= new String(zFrame.getData());

				msg.send(worker);

			} catch (Throwable e) {
				logger.error("ServerWorkForPool error ", e);
			}

		}

	}
}
