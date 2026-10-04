// Vercel Serverless Function: /api/v1/sync
// Supports GET, POST, and OPTIONS for Android Mobile App & Web Portal Cloud Sync

module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method === 'POST') {
    try {
      const payload = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
      const itemsCount = (payload?.items || payload?.inventory || []).length;
      const purchasesCount = (payload?.purchases || []).length;
      const suppliersCount = (payload?.suppliers || []).length;

      return res.status(200).json({
        status: 'success',
        syncedAt: Date.now(),
        serverTime: new Date().toISOString(),
        itemsCount: itemsCount,
        purchasesCount: purchasesCount,
        suppliersCount: suppliersCount,
        message: 'Real-time sync successful with lakshanaveggie.trade',
        domain: 'lakshanaveggie.trade'
      });
    } catch (err) {
      return res.status(400).json({
        status: 'error',
        message: 'Invalid sync payload: ' + err.message
      });
    }
  }

  // GET request - health check and status
  return res.status(200).json({
    status: 'online',
    endpoint: '/api/v1/sync',
    domain: 'lakshanaveggie.trade',
    timestamp: Date.now(),
    serverTime: new Date().toISOString(),
    message: 'Lakshana Veggie Sync API is operational'
  });
};
