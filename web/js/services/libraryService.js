import { SessionManager } from '../utils/session.js';

const SOAP_URL = 'http://localhost:8085/ws/booking';

/**
 * Helper to escape special XML characters.
 */
function escapeXml(unsafe) {
  if (!unsafe) return '';
  return unsafe.replace(/[<>&'"]/g, (c) => {
    switch (c) {
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '&': return '&amp;';
      case '\'': return '&apos;';
      case '"': return '&quot;';
      default: return c;
    }
  });
}

/**
 * Centralised SOAP request utility.
 */
async function soapRequest(action, payload) {
  const xmlBody = `
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://service.smartcampus/">
   <soapenv:Header/>
   <soapenv:Body>
      ${payload}
   </soapenv:Body>
</soapenv:Envelope>
  `.trim();

  try {
    const response = await fetch(SOAP_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'text/xml; charset=utf-8',
      },
      body: xmlBody,
    });

    const responseText = await response.text();
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(responseText, 'text/xml');

    // Check for SOAP Faults
    const faultNode = xmlDoc.getElementsByTagName('faultstring')[0];
    if (faultNode) {
      const faultMsg = faultNode.textContent.replace('SOAP_FAULT: ', '');
      throw new Error(faultMsg);
    }

    if (!response.ok) {
      throw new Error(`SOAP Request failed with status ${response.status}`);
    }

    return xmlDoc;
  } catch (error) {
    console.error(`SOAP Action '${action}' Error:`, error);
    throw error;
  }
}

function getElementText(parent, tagName) {
  const el = parent.getElementsByTagName(tagName)[0];
  return el ? el.textContent : '';
}

function parseBookLoans(xmlDoc) {
  const returnElements = xmlDoc.getElementsByTagName('return');
  const loans = [];
  for (let i = 0; i < returnElements.length; i++) {
    const element = returnElements[i];
    loans.push({
      id: parseInt(getElementText(element, 'id') || '0', 10),
      loanReference: getElementText(element, 'loanReference'),
      studentId: getElementText(element, 'studentId'),
      studentName: getElementText(element, 'studentName'),
      bookIsbn: getElementText(element, 'bookIsbn'),
      bookTitle: getElementText(element, 'bookTitle'),
      loanDate: getElementText(element, 'loanDate'),
      dueDate: getElementText(element, 'dueDate'),
      returnDate: getElementText(element, 'returnDate'),
      status: getElementText(element, 'status'),
      fineAmount: parseFloat(getElementText(element, 'fineAmount') || '0.0'),
    });
  }
  return loans;
}

export const LibraryService = {
  /**
   * Searches the library book catalog. (Open to students and admins)
   */
  async searchBooks(query) {
    const escapedQuery = escapeXml(query);
    const payload = `
      <ser:searchBooks>
         <query>${escapedQuery}</query>
      </ser:searchBooks>
    `;

    const xmlDoc = await soapRequest('searchBooks', payload);
    const returnElements = xmlDoc.getElementsByTagName('return');
    const books = [];

    for (let i = 0; i < returnElements.length; i++) {
      const element = returnElements[i];
      books.push({
        id: parseInt(getElementText(element, 'id') || '0', 10),
        isbn: getElementText(element, 'isbn'),
        title: getElementText(element, 'title'),
        author: getElementText(element, 'author'),
        category: getElementText(element, 'category'),
        status: getElementText(element, 'status'),
      });
    }

    return books;
  },

  /**
   * Adds a new book to the catalog. (ADMIN Only)
   */
  async addBook({ isbn, title, author, category }) {
    const token = SessionManager.getToken() || '';
    const payload = `
      <ser:addBook>
         <token>${escapeXml(token)}</token>
         <isbn>${escapeXml(isbn)}</isbn>
         <title>${escapeXml(title)}</title>
         <author>${escapeXml(author)}</author>
         <category>${escapeXml(category)}</category>
      </ser:addBook>
    `;

    const xmlDoc = await soapRequest('addBook', payload);
    const returnNode = xmlDoc.getElementsByTagName('return')[0];
    return returnNode ? returnNode.textContent === 'true' : false;
  },

  /**
   * Borrows a book and registers a loan transaction. (ADMIN Only)
   */
  async borrowBook({ studentId, studentName, isbn, dueDate }) {
    const token = SessionManager.getToken() || '';
    const payload = `
      <ser:borrowBook>
         <token>${escapeXml(token)}</token>
         <studentId>${escapeXml(studentId)}</studentId>
         <studentName>${escapeXml(studentName)}</studentName>
         <isbn>${escapeXml(isbn)}</isbn>
         <dueDate>${escapeXml(dueDate)}</dueDate>
      </ser:borrowBook>
    `;

    const xmlDoc = await soapRequest('borrowBook', payload);
    const returnNode = xmlDoc.getElementsByTagName('return')[0];
    if (!returnNode) {
      throw new Error('Invalid borrow response structure from server.');
    }
    return returnNode.textContent; // Returns Reference ID, e.g. LN-XXXXXXXX
  },

  /**
   * Returns a borrowed book back into the catalog. (ADMIN Only)
   */
  async returnBook(loanRef) {
    const token = SessionManager.getToken() || '';
    const payload = `
      <ser:returnBook>
         <token>${escapeXml(token)}</token>
         <loanRef>${escapeXml(loanRef)}</loanRef>
      </ser:returnBook>
    `;

    const xmlDoc = await soapRequest('returnBook', payload);
    const returnNode = xmlDoc.getElementsByTagName('return')[0];
    return returnNode ? returnNode.textContent === 'true' : false;
  },

  /**
   * Gets borrowing history of a specific book by ISBN. (ADMIN Only)
   */
  async getBookLoanHistory(isbn) {
    const token = SessionManager.getToken() || '';
    const payload = `
      <ser:getBookLoanHistory>
         <token>${escapeXml(token)}</token>
         <isbn>${escapeXml(isbn)}</isbn>
      </ser:getBookLoanHistory>
    `;

    const xmlDoc = await soapRequest('getBookLoanHistory', payload);
    return parseBookLoans(xmlDoc);
  },

  /**
   * Gets borrowing history of a specific student. (STUDENT or ADMIN)
   */
  async getStudentLoanHistory(studentId) {
    const token = SessionManager.getToken() || '';
    const payload = `
      <ser:getStudentLoanHistory>
         <token>${escapeXml(token)}</token>
         <studentId>${escapeXml(studentId)}</studentId>
      </ser:getStudentLoanHistory>
    `;

    const xmlDoc = await soapRequest('getStudentLoanHistory', payload);
    return parseBookLoans(xmlDoc);
  }
};
