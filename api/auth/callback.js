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

    // Ler resposta como texto puro primeiro para ver o que o GitHub realmente devolve
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

    return res.redirect(302, `axev3d://callback?token=${data.access_token}`);

  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
}
