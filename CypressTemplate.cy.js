
require('cypress-xpath');

describe('template spec', () => {
  it('passes', () => {
    
let waitime = 1000;

Cypress.Commands.add('takeScreenshot', (ssfilename) => {
  if (!Cypress.env('takeSS')) {
    return;
  }

  cy.screenshot(ssfilename, { capture: 'runner' }); // 'runner' option captures the screenshot in the Cypress Test Runner

  // You can specify the directory to save the screenshot, or it will be saved in the default Cypress screenshots directory.
  // Example: cy.screenshot({ capture: 'runner', screenshotFolder: 'cypress/screenshots' });

  // You can also add additional logic to handle the screenshot file as needed.
});

Cypress.on('uncaught:exception', (err, runnable) => {
	// returning false here prevents Cypress from
	// failing the test
	return false
  })

// Cypress equivalent of findElement function
function findElement(xpath, searchString, altpath) {
  let we = null;
  let xpath1 = null;
  let xpath2 = null;

  try {
    if (xpath != null && xpath !== '' && xpath.includes('|')) {
      xpath1 = xpath.substring(0, xpath.indexOf('|') - 1);
      xpath2 = xpath.substring(xpath.indexOf('|') + 2, xpath.length);
    } else {
      throw new Error('Xpath1 not valid');
    }

    we = cy.xpath(xpath1);
    return we;
  } catch (expath1) {
    try {
      if (xpath2 === null) {
        throw new Error('Xpath2 not valid');
      }
      we = cy.xpath(xpath2);
      return we;
    } catch (expath2) {
      try {
        if (searchString != null && searchString !== '') {
          const xsearch = `//*[text()='${searchString}']`;
          we = cy.xpath(xsearch);
          return we;
        } else {
          throw new Error('No search path');
        }
      } catch (ealtpath) {
        try {
          if (altpath != null && altpath !== '') {
            const xalt = `//*[text()='${altpath}']`;
            we = cy.xpath(xalt);
            return we;
          }
        } catch (e) {
            cy.log(e.message);
          return null;
        }
      }
    }
  }
}

// Cypress equivalent of func function
function runfunc() {
  try {
	  cy.wait(waitime);
    //Latest Step
	//Add from Here
  } catch (e) {
     cy.log(e.message);
  } finally {
    // Cypress automatically handles browser cleanup
  }
}

// Execute the test function
runfunc();

  })
})
