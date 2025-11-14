/**
 * Simple API key authentication middleware
 */
function verifyApiKey(req, res, next) {
  const apiKey = req.headers['authorization']?.replace('Bearer ', '');

  if (!apiKey) {
    return res.status(401).json({ error: 'Missing API key' });
  }

  if (apiKey !== process.env.API_KEY) {
    return res.status(403).json({ error: 'Invalid API key' });
  }

  next();
}

module.exports = {
  verifyApiKey
};
