// tests/etsy.spec.ts
import { test, expect, Page, Locator } from '@playwright/test';
//import

// Utility function to find an element using XPath
const findElement = async (page: Page, xpath: string | null, searchString: string | null, altpath: string | null): Promise<Locator | null> => {
  let selector: string | null = null;

  try {
      if (xpath && xpath.includes('|')) {
          const parts = xpath.split('|');
          const xpath1 = parts[0].trim();
          const xpath2 = parts[1].trim();

          // Attempt to find element using first XPath
          try {
              selector = `xpath=${xpath1}`;
              const locator = page.locator(selector);
              await locator.waitFor({ timeout: 5000 });
              return locator;
          } catch {
              // If not found, try the second XPath
              try {
                  selector = `xpath=${xpath2}`;
                  const locator = page.locator(selector);
                  await locator.waitFor({ timeout: 5000 });
                  return locator;
              } catch {
                  // If still not found, try searching by text
                  if (searchString) {
                      selector = `text=${searchString}`;
                      try {
                          const locator = page.locator(selector);
                          await locator.waitFor({ timeout: 5000 });
                          return locator;
                      } catch {
                          // If not found by text, try alternative path
                          if (altpath) {
                              selector = `text=${altpath}`;
                              try {
                                  const locator = page.locator(selector);
                                  await locator.waitFor({ timeout: 5000 });
                                  return locator;
                              } catch {
                                  return null;
                              }
                          }
                      }
                  }
              }
          }
      }
  } catch {
      return null;
  }

  return null;
};

test.describe('E2E Tests', () => {

  test('//title', async ({ page }) => {
    //pageconstants

    //Latest Step
    //Add from Heree
  });
});
