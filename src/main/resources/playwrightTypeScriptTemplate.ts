import { test, expect, Locator, chromium, Browser, BrowserContext, Page } from '@playwright/test';
//import

let browser: Browser;
let context: BrowserContext;
let page: Page;

test.beforeEach(async () => {
	browser = await chromium.launch({ headless: false });
	context = await browser.newContext();
	page = await context.newPage();
  });


test('//title', async () => {
	page.setDefaultTimeout(6000);
	//page constants
	
	//Latest Step
	//Add from Heree
});


test.afterEach(async () => {
	await browser.close();
  });