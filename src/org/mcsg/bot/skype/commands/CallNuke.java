package org.mcsg.bot.skype.commands;

import com.skype.Chat;
import com.skype.SkypeException;
import com.skype.User;

public class CallNuke implements SubCommand{

	@Override
	public void execute(Chat chat, User sender, String[] args)
			throws SkypeException {
		if("drew.foland".equalsIgnoreCase(sender.getId())){
			//chat.
		}
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
