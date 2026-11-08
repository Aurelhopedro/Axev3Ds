export default async function handler(req, res) {
  const baseUrl = `https://${req.headers.host}`;
  const { searchParams } = new URL(req.url, baseUrl);

  const code  = searchParams.get('code');
  const error = searchParams.get('error');

  if (error) {
    console.error('GitHub OAuth error:', error);
    return res.status(400).json({ error });
  }

  if (!code) {
    return res.status(400).json({ error: 'Missing code parameter' });
  }

  try {
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

    if (data.error || !data.access_token) {
      console.error('GitHub token exchange error:', JSON.stringify(data));
      return res.status(401).json({
        error: data.error || 'token_missing',
        description: data.error_description || 'No access_token in response',
      });
    }

    // CORRIGIDO: usar página HTML em vez de HTTP 302
    // O browser Android não consegue seguir redirects HTTP para custom schemes (axev3d://)
    const token = data.access_token;
    const deepLink = `axev3d://callback?token=${token}`;
    console.log('OAuth success, sending HTML redirect to deep link');

    res.setHeader('Content-Type', 'text/html');
    return res.status(200).send(`<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>AXEV 3D Studio</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body { font-family: sans-serif; background: #0d1117; color: #c9d1d9;
           display: flex; flex-direction: column; align-items: center;
           justify-content: center; height: 100vh; margin: 0; }
    h2 { font-size: 1.2rem; }
    p  { color: #8b949e; font-size: 0.9rem; text-align: center; padding: 0 20px; }
  </style>
</head>
<body>
  <h2>✅ Autenticação concluída</h2>
  <p>A redirecionar para a app AXEV 3D Studio...</p>
  <script>
    window.location.href = "${deepLink}";
    setTimeout(function() {
      document.querySelector('p').textContent =
        'Se a app não abriu automaticamente, abre a app AXEV 3D Studio manualmente.';
    }, 2000);
  </script>
</body>
</html>`);

  } catch (err) {
    console.error('Callback handler error:', err.message);
    return res.status(500).json({
      error: 'internal_server_error',
      detail: err.message,
    });
  }
}
