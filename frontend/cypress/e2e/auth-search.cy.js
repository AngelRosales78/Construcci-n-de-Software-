describe('S&P 500 Platform - Auth and Search Flow', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('should login successfully', () => {
    cy.get('input#username').type('testuser')
    cy.get('input#password').type('Str0ng!Pass#2024')
    cy.get('button[type="submit"]').click()
    cy.url().should('include', '/dashboard')
  })

  it('should navigate to search page', () => {
    cy.get('input#username').type('testuser')
    cy.get('input#password').type('Str0ng!Pass#2024')
    cy.get('button[type="submit"]').click()
    cy.url().should('include', '/dashboard')
    cy.visit('/search')
    cy.url().should('include', '/search')
  })

  it('should search for a company', () => {
    cy.get('input#username').type('testuser')
    cy.get('input#password').type('Str0ng!Pass#2024')
    cy.get('button[type="submit"]').click()
    cy.visit('/search')
    cy.get('.search-input').type('AAPL')
    cy.get('.search-button').click()
    cy.get('.company-card').should('have.length.at.least', 1)
  })

  it('should select a company and view profile', () => {
    cy.get('input#username').type('testuser')
    cy.get('input#password').type('Str0ng!Pass#2024')
    cy.get('button[type="submit"]').click()
    cy.visit('/search')
    cy.get('.search-input').type('AAPL')
    cy.get('.search-button').click()
    cy.get('.company-card').first().click()
    cy.url().should('include', '/company/')
    cy.get('.company-profile').should('be.visible')
  })

  it('should show financial health indicator', () => {
    cy.get('input#username').type('testuser')
    cy.get('input#password').type('Str0ng!Pass#2024')
    cy.get('button[type="submit"]').click()
    cy.visit('/search')
    cy.get('.search-input').type('AAPL')
    cy.get('.search-button').click()
    cy.get('.company-card').first().click()
    cy.get('.health-indicator').should('be.visible')
  })
})
