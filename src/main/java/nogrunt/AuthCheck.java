package nogrunt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.json.simple.JSONObject;

import nogrunt.exceptions.AuthorizationException;

public class AuthCheck {

    MySQlConn msc = null;
    MySqlConn2 msc2 = null;
    public Connection testCon3;

    public AuthCheck(MySqlConn2 m) {
        msc2 = m;
        testCon3 = m.testCon2;
        msc = msc2.msc;
    }

    public void authCheck(boolean fail) {
        if (fail) {
            throw new AuthorizationException("fail=true");
        }
    }

    public void authCheck(PreparedStatement stmt) {
        ResultSet rs = null;
        boolean fail = false;
        try {
            rs = stmt.executeQuery();

            if (!rs.next()) {
                fail = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        } finally {
            try {
                rs.close();
                stmt.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

        authCheck(fail);
    }

    public void isUserAuthorizedForTSR(int tsrId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.test_suite as ts on ts.productid = prods.idproducts "
                + "join cta.test_suite_results as tsr on tsr.Test_Suite_Id = ts.idtest_suite "
                + "where tsr.idtest_suite_results = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tsrId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForTSuite(int tsId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.test_suite as ts on ts.productid = prods.idproducts "
                + "where ts.idtest_suite = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tsId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForTCR(int tcrId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");
//		companyId= 2;

        String sql = "SELECT company from cta.products as prods "
                + "join cta.modules as mods on mods.product = prods.idproducts "
                + "join cta.test_case as tc on tc.Module = mods.idmodules "
                + "join cta.test_case_results as tcr on tcr.Test_Case_Id = tc.idtest_case "
                + "where tcr.idtest_case_results = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tcrId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForTC(int tcId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.modules as mods on mods.product = prods.idproducts "
                + "join cta.test_case as tc on tc.Module = mods.idmodules "
                + "where tc.idtest_case = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tcId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForAPI(int apiId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.modules as mods on mods.product = prods.idproducts "
                + "join cta.api as api on api.moduleid = mods.idmodules "
                + "where api.idapi = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, apiId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForTS(int tsId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.modules as mods on mods.product = prods.idproducts "
                + "join cta.test_case as tc on tc.Module = mods.idmodules "
                + "join cta.test_step as ts on tc.idtest_case = ts.Test_Case_Id "
                + "where ts.idtest_step = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tsId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForTStepR(int tsrId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "	join cta.modules as mods on mods.product = prods.idproducts "
                + "	join cta.test_case as tc on tc.Module = mods.idmodules "
                + "	join cta.test_step as ts on tc.idtest_case = ts.Test_Case_Id "
                + " join cta.test_step_result as tsr on ts.idtest_step = tsr.Test_Step "
                + "	where tsr.idtest_step_result = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, tsrId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForProd(int prodId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "where prods.idproducts = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, prodId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForMods(int modId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        String sql = "SELECT company from cta.products as prods "
                + "join cta.modules as mods on mods.product = prods.idproducts "
                + "where mods.idmodules = ? and prods.company = ?";
        PreparedStatement stmt = null;
        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setInt(1, modId);
            stmt.setInt(2, companyId);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }

        authCheck(stmt);
    }

    public void isUserAuthorizedForCompany(int compId) {
        JSONObject userDetails = msc.getUserDetails();
        int companyId = (int) userDetails.get("companyid");

        if (compId != companyId) {
            authCheck(true);
        }
    }

    public boolean isUserAuthorizedForWTVideo(String uname) {
    	String sql = "SELECT createdDate from cta.userprofile " +
                "where email = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isAuthorized = false;

        try {
            stmt = testCon3.prepareStatement(sql);
            stmt.setString(1, uname);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp createdDate = rs.getTimestamp("createdDate");
                Timestamp currentDate = new Timestamp(System.currentTimeMillis());

                isAuthorized = createdDate.before(currentDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new AuthorizationException();
        }
        authCheck(stmt);
        return isAuthorized;
    }


}
