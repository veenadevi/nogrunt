package nogrunt;

public class ApiThread implements Runnable{

    private String randomKey;
	private String token;
	private int companyId;
	private MySqlConn2 msc2;

	public ApiThread(String token, String randomKey, int companyId) {
        this.token = token;
        this.randomKey = randomKey;
        this.companyId = companyId;
        msc2 = new MySqlConn2(null);
    }

	@Override
	public void run() {
		msc2.triggerBulkApi(token, randomKey, companyId);
		msc2.closeDbConn();
	}

}
