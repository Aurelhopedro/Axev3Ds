export default async function handler(req, res) {
  // Usar WHATWG URL API em vez de url.parse() (depreciado DEP0169)
  const baseUrl = `https://${req.headers.host}`;
  const { searchParams } = new URL(req.url, baseUrl);

  const code  = searchParams.get('code');
  const error = searchParams.get('error');

  // 1. GitHub devolveu erro (utilizador recusou, app suspensa, etc.)
  if (error) {
    console.error('GitHub OAuth error:', error);
    return res.status(400).json({ error });
  }

  // 2. Code em falta
  if (!code) {
    return res.status(400).json({ error: 'Missing code parameter' });
  }

  try {
    // 3. Trocar o code pelo access_token
    const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        client_id:     process.env.GITHUB_CLIENT_ID,
        client_secret: process.env.GITHUB_CLIENT_SECRET,
        code,
        redirect_uri:  'https://provedoraxev3ds.vercel.app/api/auth/callback',
      }),
    });

    const data = await tokenRes.json();

    // 4. Tratar erros que o GitHub devolve mesmo com HTTP 200
    if (data.error || !data.access_token) {
      console.error('GitHub token exchange error:', JSON.stringify(data));
      return res.status(401).json({
        error: data.error || 'token_missing',
        description: data.error_description || 'No access_token in response',
      });
    }

    // 5. Sucesso — redirecionar para o deep link Android
    const deepLink = `axev3d://callback?token=${data.access_token}`;
    console.log('OAuth success, redirecting to deep link');
    return res.redirect(302, deepLink);

  } catch (err) {
    console.error('Callback handler error:', err.message);
    return res.status(500).json({
      error: 'internal_server_error',
      detail: err.message,
    });
  }
}
