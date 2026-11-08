export default async function handler(req, res) {
  const baseUrl = `https://${req.headers.host}`;
  const { searchParams } = new URL(req.url, baseUrl);

  const code  = searchParams.get('code');
  const error = searchParams.get('error');

  if (error) {
    return res.status(400).json({ step: 'github_error', error });
  }

  if (!code) {
    return res.status(400).json({ step: 'missing_code' });
  }

  const clientId     = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;

  // DIAGNÓSTICO: mostrar estado das env vars e do code recebido
  const diag = {
    step: 'before_token_exchange',
    code_length: code.length,
    code_prefix: code.substring(0, 6),
    client_id_full: clientId,
    client_secret_length: clientSecret ? clientSecret.length : 0,
    client_secret_prefix: clientSecret ? clientSecret.substring(0, 10) : 'MISSING',
    redirect_uri_sent: 'https://provedoraxev3ds.vercel.app/api/auth/callback',
  };

  if (!clientId || !clientSecret) {
    return res.status(500).json({ ...diag, step: 'env_vars_missing' });
  }

  try {
    const requestBody = {
      client_id:     clientId,
      client_secret: clientSecret,
      code,
      redirect_uri:  'https://provedoraxev3ds.vercel.app/api/auth/callback',
    };

    const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(requestBody),
    });

    const rawText = await tokenRes.text();
    let data;
    try { data = JSON.parse(rawText); } catch { data = { raw: rawText }; }

    // DIAGNÓSTICO COMPLETO: mostrar tudo
    if (data.error || !data.access_token) {
      return res.status(401).json({
        ...diag,
        step: 'token_exchange_failed',
        http_status: tokenRes.status,
        http_status_text: tokenRes.statusText,
        response_headers: Object.fromEntries(tokenRes.headers.entries()),
        github_response: data,
        request_body_sent: {
          client_id: requestBody.client_id,
          client_secret_length: requestBody.client_secret.length,
          client_secret_chars_1_to_10: requestBody.client_secret.substring(0, 10),
          client_secret_chars_last_6: requestBody.client_secret.substring(requestBody.client_secret.length - 6),
          code: requestBody.code,
          redirect_uri: requestBody.redirect_uri,
        },
      });
    }

    // Sucesso
    const token = data.access_token;
    const deepLink = `axev3d://callback?token=${token}`;
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
    return res.status(500).json({
      ...diag,
      step: 'exception',
      error: err.message,
      stack: err.stack,
    });
  }
}
