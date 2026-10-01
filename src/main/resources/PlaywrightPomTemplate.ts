// pages/CartPage.ts
import { Page, Locator } from '@playwright/test';

export class //classname {
  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  private async findElement(xpath: string | null): Promise<Locator | null> {
    let selector: string | null = null;
  
    try {
        if (xpath && xpath.includes('|')) {
            const parts = xpath.split('|');
            const xpath1 = parts[0].trim();
            const xpath2 = parts[1].trim();
  
            // Attempt to find element using first XPath
            try {
                selector = `xpath=${xpath1}`;
                const locator = this.page.locator(selector);
                await locator.waitFor({ timeout: 5000 });
                return locator;
            } catch {
                // If not found, try the second XPath
                try {
                    selector = `xpath=${xpath2}`;
                    const locator = this.page.locator(selector);
                    await locator.waitFor({ timeout: 5000 });
                    return locator;
                } catch {
                  return null;
                }
            }
        }
    } catch {
        return null;
    }
  
    return null;
  };

//Declaration
}
