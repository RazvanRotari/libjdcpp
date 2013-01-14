package com.phinvader.libjdcpp;

/**
 * This is a class just to store all related constants
 * 
 * @author phinfinity
 * 
 */
public class DCConstants {
	public static String version = "libjdcpp v1.0";

	/**
	 * This is the number of bytes per chunk sent while transfering files over
	 * the TCP connection
	 */
	public static long data_chunk_size = 40906;
	/**
	 * This is the maximum number of messages that may be Queued in a given
	 * MessageHandler Instance. If more messages are arrive then the TCP Socket
	 * will block and wait till the message Queue is freed
	 */
	public static long max_message_queue_size = 1024;
}