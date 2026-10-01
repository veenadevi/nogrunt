package nogrunt.codegenNew;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.sql.ResultSet;

import java.util.Random;


import nogrunt.codegen.SelJava;
import nogrunt.*;

public class PomGen {
	
	final String classDef = "runLogic";
	final String packageDef = "//package";
	int waittime = 4000;
	SelJava sj = new SelJava();
	MySQlConn msc = null;
	MySqlConn2 msc2 = null;

	public PomGen(MySQlConn msc) {
		this.msc = msc;
		msc2 = new MySqlConn2(msc);
	}
	
	public void genCode(int companyId, ResultSet rs, String folderName, int prodid) {
		String genType = msc.getGetTypeForCompany(companyId);
		String generateMultipleLocators = msc.generateMultipleLocators(companyId);
		boolean isMultipleLocatorPom = false;
		if (generateMultipleLocators != null && generateMultipleLocators.equals("true")) isMultipleLocatorPom = true;
		
//		if(genType != null && genType.equals("pwts")) {
//			GenPWTSPomClass gppc = new GenPWTSPomClass(msc);
//			gppc.genPwtsPOM(companyId, rs, folderName, prodid);
//		} else if(genType != null && genType.equals("pwjava")) {
//				GenPWJavaPomClass gpwjavapc = new GenPWJavaPomClass(msc);
//				gpwjavapc.genPwJavaPOM(companyId, rs, folderName, prodid);
//		} else if(genType != null && genType.equals("selc#")) {
//			GenSelCSharpPomClass gSelCSharpPC = new GenSelCSharpPomClass(msc);
//			gSelCSharpPC.genSelCSharpPOM(companyId, rs, folderName, prodid, isMultipleLocatorPom);
//		} else {
//			GenJavaPomClass gjpc = new GenJavaPomClass(msc);
//			gjpc.genJavaPOM(companyId, rs, folderName, prodid);
//		}
	}	
}
