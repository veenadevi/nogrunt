using NUnit.Framework;
using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

using System.Diagnostics;
using selenium_c_sharp.Page_Objects;


namespace selenium_c_sharp.Test_Scripts
{  
    [TestFixture]
    public class SeleniumCSharpTemplate
    {
               
        IWebDriver driver;
        //page declarations
        

        [SetUp]
        public void init()
        {
            ChromeOptions options = new ChromeOptions();
            driver = new ChromeDriver(options);
            //page initializations
        }

		[Test]
        public void runLogic()
        {
            try
            {
				//Latest Step
                //Add from Here
            }
            catch (Exception ex)
            {
				Console.WriteLine(ex.ToString());
            }
        }

        [TearDown]
        public void cleanup()
        {
            //close the browser  
			driver.Close();
        }
    }
}
