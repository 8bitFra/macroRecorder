package program;

import gui.UI;

public class MainProgram {
	public static String astart;
	public static void main(String[] args) {
		
		try
		{
			if(args[0].equals("-play"))
			{
				astart = System.getProperty("user.dir") + "\\" + args[1];
			}
		}
		catch(Exception e)
		{
			System.out.println("No arguments");
		}
		
		
		UI.run();
	}
}