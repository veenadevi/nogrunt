package nogrunt.integrations;

public class LLMIntegration {
	
	public static String gherkinGenericScenario =
			"Test Scenario: \n" + 
			"User logs in by accessing url https://test.acviss.co/login/ " +
			"and provides username as Demo and password as Demo. " + 
			"When successfully logged in " + 
			"and I am in the Code Management Section " +
			"and I am on the History Screen " + 
			"then export all the codes in Excel format. " +
			"Outcome: Codes should be exported in excel format.\n" +
			"------------------\n";
	
	public static String gherkinExportCodeScenario =
		"Test Scenario: \n" + 
		"User logs in by accessing url https://test.acviss.co/login/ " +
		"and provides username as Demo and password as Demo. " + 
		"When successfully logged in " + 
		"and I am in the Code Management Section " +
		"and I am on the History Screen " + 
		"then export all the codes in Excel format. " +
		"Outcome: Codes should be exported in excel format.\n" +
		"------------------\n";

	public static String gherkinSubCustomerScenario = 
		"Test Scenario: \n" +
		"User logs in by accessing url https://test.acviss.co/login/ " +
		"and provides username as Demo and password as Demo. " + 
		"When successfully logged in " +
		"then on the Sub Customer Screen " + 
		"And I enter values for Customer name, Short code, phone number, " +
		"Email and Address and click on Add sub customer. " +
		"Outcome: A sub customer should be created.\n" +
		"------------------\n";

	public static String gherkinProdDeletionScenario =
		"Test Scenario: \n" +
		"User logs in by accessing url https://test.acviss.co/login/ " +
		"and provides username as Demo and password as Demo. " + 
		"When successfully logged in " +
		"then on the product details page " +
		"I delete a product. " +
		"Outcome: Product should be deleted.\n" +
		"------------------\n";

	public static String gherkinViewDataScenario =
		"Test Scenario: \n" +
		"User logs in by accessing url https://test.acviss.co/login/ " +
		"and provides username as Demo and password as Demo. " + 
		"When successfully logged in " +
		"then on the Uploaded Variable Data page " +
		"I select a date range. " +
		"Outcome: I should be able to view the data.\n" +
		"------------------\n";

	public static String applicationPages = 
		"Application Pages and their description: \n" +
		"01. Login Page - Authenticates users, only successfully authenticated users will be able to access the rest of the application. " +
		"02. Landing Page - This is the application landing page, from here you can access all the functionality of the application. " +
		"03. Dashboard - Provides metrics and insights about the system and its usage. " +
		"04. Customer management - This is the section where all of the customer related functionality can be accessed. " +
		"05. Code management - This is the section where all of the functionality related to codes can be accessed. " +
		"06. Product management - This is the section landing page for all functionality related to products. " +
		"07. Variable Data - This is the section where you can perform bulk operations on codes and perform linking of codes in bulk. " +
		"08. User Management - This section handles all the functionality related to users who have been provisioned to access the system. " +
		"09. Consumer Engagement - This is the section where you can view data and insights related to consumers and their engagement with your codes. " +
		"10. Loyalty system - This is the section where you manage your loyalty programs. " +
		"11. License Management - In this section you can access all functionality related to licenses that customers require to use the Acviss product. " +
		"12. Loyalty Insights - This is the section where you view data and insights about how your loyalty program is performing. " +
		"13. Loyalty Bonus Points - This is the section where you manage how bonus points are allotted. " +
		"14. Store Management - This is the section where you access functionality about your store. " +
		"15. Add Customer - New customers are added into the system on this page. " +
		"16. Edit Customer - You can edit existing customer details on this page. " +
		"17. Attributes - You customize the messaging for each customer for the code usage individually. " +
		"18. Batch Wise Attribute - You customize the messaging for each customer for the code usage in a batch. " +
		"19. Sub Customer - You add sub customers on this page. " +
		"20. MI10Code - Generates codes of type MI10. " +
		"21. Generate Codes - Generates generic codes. " +
		"22. Generate Advanced Codes - Generates advanced codes. " +
		"23. Activate/deactivate Codes - Selected codes can be activated or deactivated. " +
		"24. Export Codes - Code can be exported out of the Acviss system. " +
		"25. History - View a list of all codes that have been generated. " +
		"26. Text Parameters - Associate codes with parameters. " +
		"27. Hologram Parameters - Define hologram parameters for a code. " +
		"28. Category Details - Ability to view product and their categories. " +
		"29. Product Details - Page where you can add new products or delete existing products and edit existing product information and view all of its attributes. " +
//		"30. Product Hide Show - Page where an admin can set whether a product is visible or not. This does not delete the product from the system but just makes it invisble to the customers " +
		"31. Export - Page to export all products. " +
		"32. Product Serial Mapping - Page to associate codes to product in bulk. " +
		"33. Product Serial Demapping - Page to disassociate codes to product in bulk.\n" +
		"------------------\n";

	public static String applicationPageElements = 
		"Application Pages and their elements - : \n" +
		"the data is in the format page name - html element type - Element label  - Observed behaviour on the UI. \n" +
		"the pagename is the page on which this element exists and this is the page you should return for this element. \n" +		
		"It is implied that an element on a page can only act on the entity(entities) that are present on that page : \n" +
		"01. Login Page - Input - Username - Fills Value. " + 
		"02. Login Page - Input - Password - Fills Value. " + 
		"03. Login Page - Button - LOGIN - Triggers authentication functionality on the backend, if successful takes user to landing page, else displays error message on login page. " +
		"04. Landing Page - Button - Dashboard - Takes user to Dashboard Page. " +
		"05. Landing Page - Button - Customer management - Takes user to Customer management Page. " +
		"06. Landing Page - Button - Code Management - Takes user to Code management page. " +
		"07. Landing Page - Button - Product Management - Takes user to Product management page. " +
		"08. Landing Page - Button - Variable Data - Takes user to Variable Data page. " +
		"09. Landing Page - Button - User Management - Takes user to User Management page. " +
		"10. Landing Page - Button - Consumer Engagement - Takes user to Consumer Engagement page. " +
		"11. Landing Page - Button - Loyalty system - Takes user to Loyalty page. " +
		"12. Landing Page - Button - License Management - Takes user to License Management page. " +
		"13. Landing Page - Button - Loyalty Insights - Takes user to Loyalty Insights page. " +
		"14. Landing Page - Button - Loyalty Bonus Points - Takes user to Loyalty Bonus Points page. " +
		"15. Landing Page - Button - Store Management - Takes user to Store Management page. " +
		"16. Customer Management - Button - Add Customer - Takes user to Add Customer page. " +
		"17. Customer Management - Button - Edit Customer - Takes user to Edit Customer page. " +
		"18. Customer Management - Button - Attributes - Takes user to Attributes page. " +
		"19. Customer Management - Button - Batch Wise Attribute - Takes user to Batch Wise Attribute page. " +
		"20. Customer Management - Button - Sub Customer - Takes user to Sub Customer page. " +
		"21. Code Management - Button - MI10Code - Takes user to MI10Code definition page. " +
		"22. Code Management - Button - Generate Codes - Takes user to Generate Codes page. " +
		"23. Code Management - Button - Generate Advanced Codes - Takes user to Generate Advanced Codes page. " +
		"24. Code Management - Button - Activate/deactivate Codes - Takes user to Activate/deactivate Codes page. " +
		"25. Code Management - Button - Export Codes - Takes user to Export Codes page. " +
		"26. Code Management - Button - History - Takes user to History page. " +
		"27. Code Management - Button - Text Parameters - Takes user to Text Parameters page. " +
		"28. Code Management - Button - Hologram Parameters - Takes user to Hologram Parameters page. " +
		"29. Product Management - Button - Category Details - Takes user to Category Details page. " +
		"30. Product Management - Button - Product Details - Takes user to Product Details page. " +
		"31. Product Management - Button - Product Hide Show - Takes user to Product Hide Show page. " +
		"32. Product Management - Button - Export - Takes user to Export page. " +
		"33. Product Management - Button - Product Serial Mapping - Takes user to Product Serial Mapping page. " +
		"34. Product Management - Button - Product Serial Demapping - Takes user to Product Serial Demapping page. " +
		"35. History - Button - Excel - Downloads code history as an XLS file. " +
		"36. History - Button - CSV - Downloads code history as an csv file. " +
		"37. History - Input - Search - Searches for a batch name of codes. " +
		"38. History - Button - Load Count - Shows the number of codes. " +
		"39. History - Button - Delete Raw Codes - Deletes the codes. " +
		"40. Sub Customer - Input - Customer Name - Fills Value. " +
		"41. Sub Customer - Input - Customer Short Code - Fills Value. " +
		"42. Sub Customer - Input - Phone Number - Fills Value. " +
		"43. Sub Customer - Input - Email - Fills Value. " +
		"44. Sub Customer - Input - Address - Fills Value. " +
		"45. Sub Customer - Button - Add Sub Customer - Creates sub customers with the filled in details. " +
		"46. Product details - Input - Search - Searches for product. " +
		"47. Product details - Button - Edit - Takes user to Edit Product page. " +
		"48. Product details - Button - Delete - Deletes a product. " +
		"49. Product details - Button - Hide - Hides the product. " +
		"50. Uploaded Variable Data - Input - Start Date - Start date for applying filter. " +
		"51. Uploaded Variable Data - Input - End Date - End date for applying filter. " +
		"52. Uploaded Variable Data - DropDown - Filter User By - list of users. " +
		"53. Uploaded Variable Data - Button - Submit - Dispalys all the entries that match the filter info entered in to the other fields on this page." +
		"54. Generate Codes - DropDown - Type of Code - Select type of code. " +
		"55. Generate Codes - DropDown - MI10 Set Name - Select the applicable MI10 format. " +
		"56. Generate Codes - Input - Batch Name - fills value. " +
		"57. Generate Codes - Input - Batch Code - fills value. " +
		"58. Generate Codes - Input - Number of Codes - fills value. " +
		"59. Generate Codes - DropDown - Verify Hologram Color - How many colors to verify." +
		"60. Generate Codes - Button - Submit - Generates the codes. \n" +
		"------------------\n";
	
	public static String applicationPages360 = 
			"Application Pages and their description: \n" +
			  "01. Login Page - Authentication page for Document360 that provides access to the application. \r\n"
			+ "02. Projects Dashboard - Main landing page after login that displays all projects accessible to the user. \r\n"
			+ "03. Knowledge Base Portal - Document management interface that allows users to create and publish articles. \r\n"
	
			+ "04. All Articles Page - Grid view of all articles in the knowledge base with bulk action capabilities.\r\n"
			+ "05. New Article Panel - You enter all the details on this panel that decsribes an article and is step1 in creating an article.\r\n"
			+ "06. Article Editor - Rich text editor interface for creating and editing knowledge base articles, with publishing controls and "
			+ "article configuration settings. This is step 2 in creating an article\r\n"
			+ "\n" +
			"------------------\n";
	
	public static String applicationPageElements360 = 
			"Application Pages and their elements - : \n" +
			"the data is in the format page name - html element type - Element label  - Observed behaviour on the UI. " +
			"the pagename is the page on which this element exists and this is the page you should return for this element. " +		
			"It is implied that an element on a page can only act on the entity(entities) that are present on that page : " +
			"01. Login Page - Input - Email - Fills email value for authentication. "
			+ "02. Login Page - Input - Password - Fills password value with masked characters. "
			+ "03. Login Page - Checkbox - Remember me - Enables persistent login functionality. "
			+ "04. Login Page - Link - Forgot password? - Navigates to password recovery page. "
			+ "05. Login Page - Button - Login - Triggers authentication with entered credentials. "
			+ "06. Login Page - Link - Sign up - Navigates to account creation page for new users. "
			+ "07. Login Page - Text - Welcome to Document360 - Displays main heading. "
			+ "08. Login Page - Text - Login to your account - Displays secondary heading. "
			+ "09. Login Page - Image - Document360 Logo - Displays company branding. "
			+ "10. Login Page - Section - Advanced WYSIWYG editor - Promotional section showing new editor features. "
			+ "11. Projects Dashboard - Text - Your projects - Displays dashboard heading. "
//			+ "12. Projects Dashboard - Button - + Project - Creates a new project. "
			+ "13. Projects Dashboard - Button - HELP - Opens help documentation. "
			+ "14. Projects Dashboard - Icon - Profile - Displays user profile menu. "
			+ "15. Projects Dashboard - Card - Project Card - Displays project information including logo and name. "
			+ "16. Projects Dashboard - Badge - Trial / 6 days - Shows trial status and remaining days. "
			+ "17. Projects Dashboard - Icon - Private - Indicates project privacy status. "
			+ "18. Projects Dashboard - Link - Document360 Logo - Returns to dashboard home. "
			+ "19. Projects Dashboard - Link - Project Name - Displays project name. "
			+ "20. Knowledge Base Portal - Dropdown - V1 - Version selector for documentation. "
			+ "21. Knowledge Base Portal - Dropdown - Create (Top) - Contains options for content creation: "
			+ "   - Article - Creates a new blank article "
			+ "   - Category - Creates a new category "
			+ "   - Article from template - Creates article using predefined template "
			+ "   - Import articles - Imports articles from external sources "
			+ "22. Knowledge Base Portal - Dropdown - Create (Category) - Contains category-specific options: "
			+ "   - Article - Creates new article in selected category "
			+ "   - Sub category - Creates nested category under current category "
			+ "   - Article from template - Creates templated article in category "
			+ "   - Import articles - Imports articles into selected category "
			+ "23. Knowledge Base Portal - More Options Menu - Category context menu with actions: "
			+ "   - Create article - Adds new article to category "
			+ "   - Create sub category - Creates nested subcategory "
			+ "   - Change type - Modifies category type "
			+ "   - Set drive folder - Configures associated drive folder "
			+ "   - Security - Manages category access permissions "
			+ "   - More - Additional category options "
			+ "24. Knowledge Base Portal - Button - Create - Opens creation menu for new content. "
			+ "25. Knowledge Base Portal - Button - TRIAL ENDS IN 6 DAYS - Displays trial status. "
			+ "26. Knowledge Base Portal - Button - OPEN SITE - Opens public view of documentation. "
			+ "27. Knowledge Base Portal - Button - HELP - Opens help documentation. "
			+ "28. Knowledge Base Portal - Section - Site builder - Navigation item for site building tools. "
			+ "29. Knowledge Base Portal - Section - Content tools - Navigation item for content management. "
			+ "30. Knowledge Base Portal - Section - CATEGORIES & ARTICLES - Navigation section for document hierarchy. "
			+ "31. Knowledge Base Portal - Folder - Getting started guides - Category folder in navigation. "
			+ "32. Knowledge Base Portal - Folder - How-to guides - Category folder in navigation. "
			+ "33. Knowledge Base Portal - Folder - API Documentation - Category folder in navigation. "
			+ "34. Knowledge Base Portal - Table - Content List - Displays documents with Title, Tags, Updated on, and Status columns. "
			+ "35. Knowledge Base Portal - Text - 1 - 1 of 1 items - Displays pagination information. "
			+ "36. Knowledge Base Portal - Checkbox - Title Selector - Selects individual documents in the list. "
			+ "37. Knowledge Base Portal - Dropdown - Create - Opens menu with options including 'Article'. "
			+ "38. Knowledge Base Portal - Panel - Create new article - Dialog for creating new articles with name and category inputs. "
			+ "39. Knowledge Base Portal - Icon - Flywheel - Appears on hover below articles for quick article creation. "
			+ "40. Knowledge Base Portal - Icon - More - Shows additional options menu for categories. "
			+ "41. Knowledge Base Portal - Button - Create article - Creates new article in selected category. "
			+ "42. Knowledge Base Portal - Icon - Status Indicator - Displays colored circle (green for new, yellow for updated) next to article names. "
			+ "43. Article Editor - Button - Publish - Opens publish confirmation dialog with workflow status change to Published. "
			+ "44. Article Editor - Dialog - Publish confirmation - Shows article name and version info (e.g., \"Revision: V1\"). "
			+ "45. Article Editor - Input - Comment - Optional comment field for documenting publish changes. "
			+ "46. Article Editor - Dropdown - Configure article settings - Expands publishing settings panel. "
			+ "47. Article Editor - Section - Add tags - Tag management with minimum 50 words requirement for AI generation. "
			+ "48. Article Editor - Button - Ask Eddy AI (Tags) - AI assistance for generating relevant tags. "
			+ "49. Article Editor - Section - SEO description - SEO metadata management section. "
			+ "50. Article Editor - Checkbox - Exclude from external search engine results - Controls search engine indexing. "
			+ "51. Article Editor - Input - SEO description - Field for entering SEO description. "
			+ "52. Article Editor - Button - Ask Eddy AI (SEO) - AI assistance for generating SEO description. "
			+ "53. Article Editor - Section - Related articles - Section for managing article relationships. "
			+ "54. Article Editor - Input - Search related articles - Search field for finding related content. "
			+ "55. Article Editor - Button - Ask Eddy AI (Related) - AI assistance for suggesting related articles. "
			+ "56. Article Editor - Section - Status indicator - Controls for article status management. "
			+ "57. Article Editor - Dropdown - Select status - Sets article status (None, New, Updated, etc.). "
			+ "58. Article Editor - Button - No - Cancels the publish operation. "
			+ "59. Article Editor - Button - Yes - Confirms and executes the publish operation. "
			+ "60. Article Editor - Text - WORKFLOW STATUS - Displays current article status (e.g., Draft). "
			+ "61. Article Editor - Badge - NEW - Indicates new article status in the navigation. "
			+ "62. All Articles Page - Button - All articles - Shows complete article listing. "
			+ "63. All Articles Page - Checkbox - Article selector - Enables bulk selection of articles. "
			+ "64. All Articles Page - Button - Publish - Publishes selected articles in bulk. "
			+ "65. New Article Panel - Input - Name - Enter article name.  "
			+ "66. New Article Panel - Dropdown - Category - Select category with search functionality. "
			+ "67. New Article Panel - Input - Search Category - Search field for finding a category. "
			+ "68. New Article Panel - Radio - Category Options - Selects from available categories. "
			+ "68. New Article Panel - Button - Cancel - Dismisses the dialog. "
			+ "68. New Article Panel - Button - Create - Creates a new article. "
//			+ "09. Create New Article Dialog to Article Editor - After clicking Create button:\r\n"
//			+ "   - Editor appears with \"NEW\" badge in breadcrumb\r\n"
//			+ "   - Shows \"Press Ctrl+Spacebar to ask Eddy AI or / for commands\" placeholder\r\n"
//			+ "   - Displays Preview option in top bar\r\n"
//			+ "   - Shows Draft status in workflow\r\n"
//			+ "   - Includes AI assistant access via purple robot icon\r\n"
//			+ "   - Icon - Help/Info - Shows article information\r\n"
//			+ "   - Icon - Lock - Indicates article access permissions\r\n"
//			+ "   - Icon - Preview - Enables preview mode\r\n"
//			+ "   - Text - Workflow Status - Shows current status (e.g., \"Draft\")\r\n"
//			+ "   - Button - Publish - Opens publish options\r\n"
//			+ "   - Icon - Settings - Additional article settings\r\n"
//			+ "   - Icon - Comments - Article discussion/feedback\r\n"
			+ "------------------\n";

	public static String applicationPageTransitions = 
		"Application Page Transitions: \n" +
		"02. Landing Page to Dashboard " +
		"03. Landing Page to Customer management " +
		"04. Landing Page to Code Management " +
		"05. Landing Page to Product Management " +
		"06. Landing Page to Variable Data " +
		"07. Landing Page to User Management " +
		"08. Landing Page to Consumer Engagement " +
		"09. Landing Page to Loyalty system " +
		"10. Landing Page to License Management " +
		"11. Landing Page to Loyalty Insights " +
		"12. Landing Page to Loyalty Bonus Points " +
		"13. Landing Page to Store Management " +
		"14. Customer Management to Add Customer " +
		"15. Customer Management to Edit Customer " +
		"16. Customer Management to Attributes " +
		"17. Customer Management to Batch Wise Attribute " +
		"18. Customer Management to Sub Customer " +
		"19. Code Management to MI10Code " +
		"20. Code Management to Generate Codes " +
		"21. Code Management to Generate Advanced Codes " +
		"22. Code Management to Activate/deactivate Codes " +
		"23. Code Management to Export Codes " +
		"24. Code Management to History " +
		"25. Code Management to Text Parameters " +
		"26. Code Management to Hologram Parameters " +
		"27. Product Management to Category Details " +
		"28. Product Management to Product Details " +
		"29. Product Management to Product Hide Show " +
		"30. Product Management to Export " +
		"31. Product Management to Product Serial Mapping " +
		"32. Product Management to Product Serial Demapping\n" +
		"------------------\n";
	
	public static String applicationPageTransitions360 = 
			"Possible Application Page Transitions: "
			+ "This data is in the format source page to destination page and page element that triggers the transistion \n" +
			"01. Base URL to Login Page "
			+ "02. Login Page to Projects Dashboard  "
			+ "03. Login Page to Forgot Password Page  "
			+ "04. Login Page to Sign Up Page "
			+ "05. Projects Dashboard to New Project Creation "
			+ "06. Projects Dashboard to Help Documentation "
			+ "07. Projects Dashboard to Knowledge Base Portal - Click on Project Name"
			+ "08. Knowledge Base Portal to Create New Article Dialog "
			+ "09. Knowledge Base Portal to New Article Panel "
			+ "10. Knowledge Base Portal to New Category Panel  "
			+ "11. New Article Panel to Article Editor  "
//			+ "11. Knowledge Base Portal to Article Template "
//			+ "12. Knowledge Base Portal to Import Interface "
//			+ "13. Knowledge Base Portal to Category Type Settings "
//			+ "14. Knowledge Base Portal to Drive Folder Settings "
//			+ "15. Knowledge Base Portal to Security Settings "
//			+ "16. Knowledge Base Portal to Sub-category Creation "
//			+ "17. Knowledge Base Portal to Site Builder "
//			+ "18. Knowledge Base Portal to Content Tools "
//			+ "19. Knowledge Base Portal to Public Site "
			+ "20. Knowledge Base Portal to Help Documentation "
//			+ "21. Knowledge Base Portal to Create New Content "
			+ "22. Knowledge Base Portal to Article Editor "
			+ "23. Knowledge Base Portal to All Articles Page "
			+ "24. Article Editor to Published State "
			+ "25. All Articles Page to Bulk Published State "
			+ "Based on mistakes you have made these are not possible page transistions "
			+ "and you should not be including these "
			+ "Negative 01. Login Page to Knowledge Base Portal. "
			+"------------------\n";
	
//	public static String applicationPageTransitions360 = 
//			"Possible Application Page Transitions: "
//			+ "This data is in the format source page to destination page and page element that triggers the transistion \n" +
//			"01. Base URL to Login Page - Enter URL in the browser address bar"
//			+ "02. Login Page to Projects Dashboard  - click on the login button"
//			+ "03. Login Page to Forgot Password Page - Click on Forgot password "
//			+ "04. Login Page to Sign Up Page - Click on Login Button"
//			+ "05. Projects Dashboard to New Project Creation - Click on Project Button"
//			+ "06. Projects Dashboard to Help Documentation - Click on Help Link"
//			+ "07. Projects Dashboard to Knowledge Base Portal - Click on Project Name"
//			+ "08. Knowledge Base Portal to Create New Article Dialog - Click on Create Button"
//			+ "09. Knowledge Base Portal to New Article Panel - Select Article"
//			+ "10. Knowledge Base Portal to New Category Panel - - Select Category "
//			+ "11. New Article Panel to Article Editor - Click Create Button "
////			+ "11. Knowledge Base Portal to Article Template "
////			+ "12. Knowledge Base Portal to Import Interface "
////			+ "13. Knowledge Base Portal to Category Type Settings "
////			+ "14. Knowledge Base Portal to Drive Folder Settings "
////			+ "15. Knowledge Base Portal to Security Settings "
////			+ "16. Knowledge Base Portal to Sub-category Creation "
////			+ "17. Knowledge Base Portal to Site Builder "
////			+ "18. Knowledge Base Portal to Content Tools "
////			+ "19. Knowledge Base Portal to Public Site "
//			+ "20. Knowledge Base Portal to Help Documentation - Click on Help Link"
////			+ "21. Knowledge Base Portal to Create New Content "
//			+ "22. Knowledge Base Portal to Article Editor - Click on Article"
//			+ "23. Knowledge Base Portal to All Articles Page - Click on Article category"
//			+ "24. Article Editor to Published State - Click on Publsh Button "
//			+ "25. All Articles Page to Bulk Published State - Click on Publish"
//			+ "Based on mistakes you have made these are not possible page transistions "
//			+ "and you should not be including these "
//			+ "Negative 01. Login Page to Knowledge Base Portal. "
//			+"------------------\n";

	public static String getPromptScenarioTransition(String ghScenario) {
		String instruction = 
			"The test scenario, application pages and application page transitions information are provided below. " +
			"You need to determine the list of transistions that are required to achieve the test scenario. " +
			"Strictly follow the provided page transitions information. " +  
			"The recommended transistions should be the shortest linear path to go " +
			"from starting page to the page where the goal is achieved. " +
			"There should not be any pages that are visited and back tracked. " +
			"Ensure that the destination of the previous transition is always the source for the next transition. " +
			"The recommended transitions should be output as a json string whose sample is given: " +
			" \"transitions\": [ {\"from page\": \"page name\", \"to page\": \"page name\"}, {\"from page\":\"\", \"to page\":\"\"}] \n";
			
			String prompt = instruction + ghScenario + applicationPages + applicationPageTransitions;
			return prompt;
	}

	public static String getPromptPageElementAction(String ghScenario, String pageTransitions, String appPagesElements) {
		String instruction = 
			"I have provided the test scenario, page transitions and the application pages with their elements. " +
			"In order to accomplish the test scenario for each of the page transitions provided, " +
			"identify the elements on each from page and the associated action with which we need to interact in sequence. \n";
						
			String prompt = instruction + ghScenario + pageTransitions + appPagesElements + element_interaction_formating;
			return prompt;
	}
	
	 public static String element_interaction_formating = "can you provide the response as a json object "
	 		+ "where each of the element interactions are represented in the "
     		+ "format 'interactions':[{'On page':'page name','element label':'','element  type':'', "
     		+ "'action':'','data':'' }] every element type will always have a element label - "
     		+ "so the corresponding element label should be populated in your response "
     		+ "where action is the html action that you need to perform on the element and "
     		+ "and data is the value (usually in double quotes username as \"deva@nogrunt.com\", "
     		+ "in this case \"deva@nogrunt.com\" is the value "
     		+ "for the username element) that is specified in the gherkin syntax for the element."
     		+ "Before you provide the answers - also provide the steps to validate that the"
     		+ "desired outcome has been achieved in the format " 
     		+ "'validation':[{'On page':'page name','element label':'','element  type':'', \"\r\n"
     		+ "     		+ \"'action':'','data':'' }]";
}
