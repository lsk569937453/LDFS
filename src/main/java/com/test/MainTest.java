package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import zmq.SocketBase;
import zmq.poll.PollItem;

import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicInteger;

public class MainTest {
	public static ZContext ctx = new ZContext();


	private static Logger logger = LoggerFactory.getLogger(MainTest.class);

	/**
	 * 存储当前的消息数
	 */
	public static AtomicInteger reqCount = new AtomicInteger(1);

	/**
	 * 允许处理的最大消息数目
	 */
	public static final int MAX_COUNT = 200;

	/**
	 * 创建一个屋
	 */
	//	public static ExecutorService threadPoolExecutor= Executors.newFixedThreadPool(30);
	public static void main(String []args) {
		try {

			constructWorker(ctx);

			Socket frontend = ctx.createSocket(ZMQ.ROUTER);
			Socket backend = ctx.createSocket(ZMQ.DEALER);

			frontend.setLinger(0);
			backend.setLinger(0);
			frontend.bind("tcp://*:5656");
			backend.bind("inproc://backend");

			PollItem[] items = new PollItem[2];

			items[0] = new PollItem(frontend.base(), zmq.ZMQ.ZMQ_POLLIN);
			items[1] = new PollItem(backend.base(), zmq.ZMQ.ZMQ_POLLIN);
			PollItem[] itemsout = new PollItem[2];

			itemsout[0] = new PollItem(frontend.base(), zmq.ZMQ.ZMQ_POLLOUT);
			itemsout[1] = new PollItem(backend.base(), zmq.ZMQ.ZMQ_POLLOUT);

			Selector selector = ctx.createSelector();

			int rc = 0;

			while (!Thread.currentThread().isInterrupted()) {
				//  Wait while there are either requests or replies to process.
				rc = zmq.ZMQ.poll(selector, items, -1);

				if (frontend != backend) {
					rc = zmq.ZMQ.poll(selector, itemsout, 0L);

				}

				//  Process a request.
				if (process(items[0], itemsout[1], frontend.base(), backend.base())) {
					//从9003端口收到消息
					ZMsg msg = ZMsg.recvMsg(frontend, ZMQ.SUB);

					msg.send(backend);

				}
				//  Process a reply.
				if (process(items[1], itemsout[0], frontend.base(), backend.base())) {
					ZMsg msg = ZMsg.recvMsg(backend, ZMQ.SUB);
					msg.send(frontend);
				}
			}

			//  We never get here but clean up anyhow
			frontend.close();
			backend.close();
		} catch (Exception e) {

			logger.error("", e);
		}

	}

	/**
	 * 构建永久work
	 * @param ctx
	 */
	public static void constructWorker(ZContext ctx) {

		//  Launch pool of worker threads, precise number is not critical
		for (int threadNbr = 0; threadNbr < 20; threadNbr++) {
			ServerWorkPerm serverWorker = new ServerWorkPerm(ctx);

			new Thread(serverWorker).start();
		}

	}

	private static boolean process(PollItem read, PollItem write, SocketBase frontend, SocketBase backend)
	{
		return read.isReadable() && (frontend == backend || write.isWritable());
	}
}

