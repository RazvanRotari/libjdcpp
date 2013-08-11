package com.phinvader.libjdcpp;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import com.phinvader.libjdcpp.DCRevconnect.DownloadStatus;

public class UsersHandler implements DCCommand {

	/**
	 * Gets the list of all NICKS registered on the current server identified by
	 * param: handler
	 * 
	 * To use UserHandler: class MyUserHandler extends UserHandler implements
	 * DCCallback
	 * 
	 * 
	 * @author madhavan
	 * 
	 * @param handler
	 * @return
	 * @throws InterruptedException
	 */

	public ArrayBlockingQueue<DCMessage> nick_q = new ArrayBlockingQueue<>(1024);

	private MessageHandler handler ;
	
	public long getDownloadStatus(){
		return handler.get_dumped_bytes();
	}
	public long getDownloadFileFullSize(){
		return handler.get_filesize();
	}
	
	/**
	 * Add a user to the list-of-online-users Updated when there is a MYINFO
	 * Message from the server
	 * 
	 * @param nick
	 * @throws InterruptedException
	 */

	public void addNick(DCMessage nick) throws InterruptedException {
		nick_q.put(nick);
		return;
	}

	/**
	 * Delete a user from the list-of-online-users This is updated when there is
	 * a QUIT message from the server
	 * 
	 * @param nick
	 */

	public void deleteNick(String nick) {
		Iterator<DCMessage> it = nick_q.iterator();
		while (it.hasNext()) {
			DCMessage nick_obj = it.next();
			if (nick_obj.myinfo.nick.equals(nick)) {
				nick_q.remove(nick_obj);
				// DCLogger.Log("Removing from list : "+nick);
			}
		}
		return;
	}

	/**
	 * 
	 * @param nick
	 *            - Nick to download from
	 * @param s
	 *            - Socket connection
	 * @param remote_filename
	 *            - Path to the file on remote user, from where to be downloaded
	 * @param local_filename
	 *            - Path where the downloaded file should be saved
	 * @return
	 */

	private boolean download_file(DCUser myuser, Socket s, String fname,
			String save_file_name) {
		// Using REVCONNECT
		try {
			DCLogger.Log("Download Started" + s.toString());

			handler = new MessageHandler(s);
			handler.send_mynick(myuser);
			handler.send_lock();

			int NUMBER_OF_EXPECTED_REPLIES_FROM_SERVER = 4;
			// Nick, Lock, Direction, Key.

			DCMessage hisnick = null;
			DCMessage rlock = null;
			DCMessage key = null;
			DCMessage direction = null;
			for (int i = 0; i < NUMBER_OF_EXPECTED_REPLIES_FROM_SERVER;) {
				DCMessage msg = handler.getNextMessage();
				if (msg.command != null) {
					i++;
					if (msg.command.equals("MyNick")) {
						hisnick = msg;
					}
					if (msg.command.equals("Lock")) {
						rlock = msg;
					}
					if (msg.command.equals("Direction")) {
						direction = msg;
					}
					if (msg.command.equals("Key")) {
						key = msg;
					}
				}
			}

			handler.send_direction(true);
			handler.send_key(DCFunctions.convert_lock_to_key(rlock.lock_s
					.getBytes()));

			handler.send_msg("$Get " + fname + "$1");
			DCMessage msg2 = handler.getNextMessage();
			handler.send_msg("$Send");

			if (msg2.command == null || !msg2.command.equals("FileLength")) {
				DCLogger.Log("Quitting..");
				handler.close();
				return false;
			}
			handler.dump_remaining_stream(save_file_name, msg2.file_length);
			while (true) {
				DCMessage msg = handler.getNextMessage();
				if (msg.command != null && msg.command.equals("HubQuit"))
					break;
			}
			s.close();
			handler.close();
			DCLogger.Log("DOwnload Complete");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	/**
	 * Download Manager will do the downloading in a separate thread to keep the UI Responsive
	 * @author madhavan
	 *
	 */
	
	public class downloadManager implements Runnable {

		private Socket s;
		private DCRevconnect revCon;
		private DCUser myuser;
		private String remote_filename;
		private String local_filename;

		public downloadManager(DCRevconnect revCon, DCUser myuser, String remote_filename,
				String local_filename) {
			super();
			this.revCon = revCon;
			this.s = revCon.s;
			this.myuser = myuser;
			this.remote_filename = remote_filename;
			this.local_filename = local_filename;
		}

		@Override
		public void run() {
			if(download_file(myuser, s, remote_filename, local_filename)){
				revCon.setCurrentDownloadStatus(DownloadStatus.COMPLETED);
			}
			else{
				revCon.setCurrentDownloadStatus(DownloadStatus.INTERUPTED);
			}

		}

	}
	
	/**
	 * DCCommand interface override
	 * Should subscribe to MyInfo and Quit to the router.
	 * 
	 * @param msg
	 */

	@Override
	public void onCommand(DCMessage msg) {
		String command = msg.command;
		if (command.equals("Quit")) {
			String quit_command = msg.toString();
			String[] quit_arr = quit_command.split(":");
			try {
				String nick_to_delete = quit_arr[1].trim();
				deleteNick(nick_to_delete);
			} catch (ArrayIndexOutOfBoundsException e) {
				DCLogger.Log("ERROR (003-001)");
			}
		} else if (command.equals("MyINFO")) {
			try {
				addNick(msg);
			} catch (InterruptedException e) {
				DCLogger.Log("ERROR (003-002)");
			}
		}
	}

}
