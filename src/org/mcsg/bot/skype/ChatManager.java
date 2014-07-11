package org.mcsg.bot.skype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.mcsg.bot.skype.util.FileUtils;
import org.mcsg.bot.skype.web.GistPaster;

import com.skype.Chat;
import com.skype.SkypeException;
import com.skype.User;

public class ChatManager {

	private static ConcurrentHashMap<Chat, List<String>> chats = new ConcurrentHashMap<>();
	private static final String ERROR_PREFIX = "\0!!!";

	public static void printThrowable(Chat chat, Throwable t){
		printError(chat, t.toString());
		for(StackTraceElement el : t.getStackTrace())
			printError(chat, "\t" + el.toString());

		Throwable e1 = t.getCause();
		if(e1 != null){
			printError(chat, e1.toString());
			for(StackTraceElement el : e1.getStackTrace())
				printError(chat, "\t" + el.toString());
		}

		for(int a  = 0; a < Settings.Root.Bot.chat.paste ; a++){
			chat(chat, "");
		}
	}

	private static void printError(Chat chat, String str){
		ChatManager.chat(chat, ERROR_PREFIX + str);
	}

	private static String getUserName(User user){
		try {return (user.getFullName().length() > 15) ? user.getFullName().substring(0,  15) : user.getFullName();} catch (Exception e) { return null; }
	}

	public static void chat(Chat chat, User user, String msg){
		chat(chat, "@"+getUserName(user)+": "+msg);
	}

	public static void chat(Chat chat, String msg){
		List<String> msgs = chats.get(chat);
		if(msgs == null){
			msgs = Collections.synchronizedList(new ArrayList<String>());
		}
		msgs.add(msg);
		chats.put(chat, msgs);
	}


	public static void start(){
		new Thread(){
			public void run(){
				while(true){
					try{
						HashMap<Chat, List<String>> copy = new HashMap<>(chats);
						for(Chat chat : copy.keySet()){
							try{
								StringBuilder sb = new StringBuilder();
								StringBuilder error = new StringBuilder();
								for(String msg : copy.get(chat)){
									if(msg.startsWith(ERROR_PREFIX)){
										error.append(msg.replaceFirst(ERROR_PREFIX, ""));
									} else {
										sb.append(msg.trim().startsWith("/") ? "." : "").append(msg).append("\n");
									}
								}
								sb.delete(sb.length() - 1, sb.length());
								if(chats.remove(chat).size() > Settings.Root.Bot.chat.paste){
									chat.send("Output: "+createPaste(sb.toString()));
								} else {
									try { 
										chat.send(sb.toString());
										if(error.length() > 0){
											chat.send("Error: "+createPaste(sb.toString()));
										}
									} catch (SkypeException e) { printThrowable(chat, e); }
								}
							} catch (Exception e){
								e.printStackTrace();
								printThrowable(chat, e);
							}
						}

						sleep(Settings.Root.Bot.chat.time * 1000);
					}catch (Exception e){
						e.printStackTrace();
					}

				}

			}
		}.start();
	}

	public static String createPaste(final String msg) throws Exception{
		if(Settings.Root.Bot.chat.pastemethod.equals("gist")){
			return GistPaster.paste(msg);
		} else {
			String paste = "files/paste"+System.currentTimeMillis();

			File file1 = new File(paste);
			FileUtils.writeFile(file1, msg);

			String script = "files/script" + System.currentTimeMillis();
			File file2 = new File(script);
			{
				PrintWriter pw1 = new PrintWriter(new FileWriter(file2));
				pw1.println("cat "+paste+" | pastebinit -a \"MC-SG.BOT Output\"");
				pw1.flush();
				pw1.close();
			}

			Process proc = Runtime.getRuntime().exec("bash "+script);
			String line = "";

			file1.deleteOnExit();
			file2.deleteOnExit();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));
			while ((line = in.readLine()) != null) {
				return line;
			}
			return line;
		}
	}


}
