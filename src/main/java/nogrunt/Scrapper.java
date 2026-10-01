package nogrunt;

public class Scrapper {
	
	int testCaseId = -1;
	String testCase = "";
	MySQlConn msc ;
	
	public static void main(String[] args) throws Exception {

		Tester tester = new Tester();
		SelGrid sg = new SelGrid();
		//sg.setup();	
		sg.chromeSetup();
		MySQlConn msc = new MySQlConn(null);
		sg.scrapper();        
        sg.tearDown();
		msc.closeDbConn();
	  }
	
	public void scrapper() {
		
	}



}
