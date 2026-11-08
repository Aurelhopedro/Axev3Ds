export default async function handler(req, res) {
  const baseUrl = `https://${req.headers.host}`;
  const { searchParams } = new URL(req.url, baseUrl);

  const code  = searchParams.get('code');
  const error = searchParams.get('error');

  if (error) return res.status(400).json({ error });
  if (!code)  return res.status(400).json({ error: 'Missing code' });

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
    const body = JSON.stringify({
      client_id:     clientId,
      client_secret: clientSecret,
      code,
      redirect_uri: 'https://provedoraxev3ds.vercel.app/api/auth/callback',
    });

    const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body,
    });

    const rawText = await tokenRes.text();
    let data;
    try {
      data = JSON.parse(rawText);
    } catch {
      data = { raw: rawText };
    }

    if (data.error || !data.access_token) {
      return res.status(401).json({
        github_status: tokenRes.status,
        github_response: data,
        client_id_prefix: clientId.substring(0, 8),
        client_secret_prefix: clientSecret.substring(0, 6),
        code_length: code.length,
        redirect_uri_sent: 'https://provedoraxev3ds.vercel.app/api/auth/callback',
      });
    }

    const token = data.access_token;
    const deepLink = `axev3d://callback?token=${token}`;

    // Página HTML que faz o redirect para o deep link da app Android.
    // Um HTTP 302 não funciona com custom schemes — o browser precisa de JS para isso.
    res.setHeader('Content-Type', 'text/html');
    return res.status(200).send(`<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>AXEV 3D Studio — Autenticando...</title>
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
    return res.status(500).json({ error: err.message });
  }
}
