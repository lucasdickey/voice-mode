/**
 * API authentication middleware/utility
 * Used across Lambda functions to validate API keys and tokens
 */

const crypto = require('crypto');

class AuthenticationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'AuthenticationError';
  }
}

class AuthorizationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'AuthorizationError';
  }
}

/**
 * Validate API key from request headers
 */
function validateApiKey(event, expectedKey) {
  const authHeader = event.headers?.authorization || event.headers?.Authorization;

  if (!authHeader) {
    throw new AuthenticationError('Missing authorization header');
  }

  const [scheme, credentials] = authHeader.split(' ');

  if (scheme !== 'Bearer') {
    throw new AuthenticationError('Invalid authorization scheme');
  }

  if (credentials !== expectedKey) {
    throw new AuthorizationError('Invalid API key');
  }

  return true;
}

/**
 * Extract user ID from Cognito authorization
 */
function getUserFromCognito(event) {
  const claims = event.requestContext?.authorizer?.claims;

  if (!claims || !claims.sub) {
    return null;
  }

  return {
    userId: claims.sub,
    email: claims.email,
    username: claims['cognito:username']
  };
}

/**
 * Generate secure API key
 */
function generateApiKey(length = 32) {
  return crypto.randomBytes(length).toString('hex');
}

/**
 * Hash API key for storage
 */
function hashApiKey(apiKey) {
  return crypto
    .createHash('sha256')
    .update(apiKey)
    .digest('hex');
}

/**
 * Create CORS headers
 */
function getCorsHeaders(origin = '*') {
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Credentials': 'true'
  };
}

/**
 * Create error response
 */
function errorResponse(statusCode, message, details = null) {
  return {
    statusCode,
    body: JSON.stringify({
      error: message,
      details,
      timestamp: new Date().toISOString()
    }),
    headers: {
      'Content-Type': 'application/json',
      ...getCorsHeaders()
    }
  };
}

/**
 * Create success response
 */
function successResponse(data, statusCode = 200) {
  return {
    statusCode,
    body: JSON.stringify({
      success: true,
      data,
      timestamp: new Date().toISOString()
    }),
    headers: {
      'Content-Type': 'application/json',
      ...getCorsHeaders()
    }
  };
}

module.exports = {
  validateApiKey,
  getUserFromCognito,
  generateApiKey,
  hashApiKey,
  getCorsHeaders,
  errorResponse,
  successResponse,
  AuthenticationError,
  AuthorizationError
};
