package def;


public class Main{
	public static void main(String[] args) {
		/* Run the builder
		 * 
		 *    args : login,password,path
		 * example : login, pass, C:\Users\USERNAME\Desktop
		 * 
		 * 
		 * runs only with all arguments!
		 */
		if (args.length == 3){
			String login = args[0];
			String password = args[1];
			String path = args[2];
			run(login, password, path);
		}
	}
	private static void run(String login, String password, String path){
		/*
		 * Setting the EPG options, and building the object:
		 * 
		 *     LOGIN : your login
		 *  PASSWORD : your password
		 *      PATH : path to save the xml file				default set to Desktop
		 * 	makeLogos: download logos from sovok webserver  	default set to false
		 * 			   logos saved to Logos folder in your
		 * 			   path
		 * 
		 */
		if(login.length() > 0 && password.length() > 0){
		Epg epg = new Epg.Builder()
				.LOGIN(login)
				.PASSWORD(password)
				.PATH(path)
				//.makeLogos(true)
				.build();
		}
	}
}





