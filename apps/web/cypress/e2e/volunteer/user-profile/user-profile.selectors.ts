const SELECTORS_USER_PROFILE = {
  CARDS: {
    USER_SETTINGS: 'Account Information',
    NOTIFICATION_SETTINGS: 'Notifications',
  },
  USER_SETTINGS: {
    name: '.card xsb-input-text:nth-of-type(1)',
    surname: '.card xsb-input-text:nth-of-type(2)',
    email: '.card xsb-input-text:nth-of-type(3)',
    username: '.card xsb-input-text:nth-of-type(4)',
    manageAccount: 'input[name="me-manage-account"]',
    signOut: 'input[name="me-sign-out"]',
  },
  NOTIFICATION_SETTINGS: {
    autoAssigned: '#me-notifications-VOLUNTEER_AUTO_ASSIGNED',
    tradeRequested: '#me-notifications-VOLUNTEER_TRADE_REQUESTED',
    tradeStatusChanged: '#me-notifications-VOLUNTEER_TRADES_AUCTIONS_REQUESTS_CHANGED',
    shiftReminder: '#me-notifications-VOLUNTEER_SHIFT_REMINDER',
  },

};
export default SELECTORS_USER_PROFILE;
