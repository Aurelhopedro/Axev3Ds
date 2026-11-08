export default async function handler(req, res) {
  const baseUrl = `https://${req.headers.host}`;
  const { searchParams } = new URL(req.url, baseUrl);

  const code  = searchParams.get('code');
  const error = searchParams.get('error');

  if (error) {
    return res.status(400).json({ error });
  }

  if (!code) {
    return res.status(400).json({ error: 'Missing code parameter' });
  }

  // DEBUG: confirmar que as variáveis chegam
  const clientId     = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    return res.status(500).json({
      error: 'env_missing',
      hasClientId: !!clientId,
      hasClientSecret: !!clientSecret,
    });
  }

  try {
    const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        client_id:     clientId,
        client_secret: clientSecret,
        code,
        redirect_uri:  'https://provedoraxev3ds.vercel.app/api/auth/callback',
      }),
    });

    const data = await tokenRes.json();

    // DEBUG: mostrar resposta completa do GitHub no browser
    if (data.error || !data.access_token) {
      return res.status(401).json({
        debug_github_response: data,
        client_id_prefix: clientId.substring(0, 6) + '...',
      });
    }

    const deepLink = `axev3d://callback?token=${data.access_token}`;
    return res.redirect(302, deepLink);

  } catch (err) {
    return res.status(500).json({
      error: 'internal_server_error',
      detail: err.message,
    });
  }
}
