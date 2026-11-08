export default async function handler(req, res) {
  const { code } = req.query;

  if (!code) {
    return res.status(400).json({ error: "Missing code parameter" });
  }

  try {
    const response = await fetch("https://github.com/login/oauth/access_token", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({
        client_id: process.env.GITHUB_CLIENT_ID,
        client_secret: process.env.GITHUB_CLIENT_SECRET,
        code,
      }),
    });

    const data = await response.json();

    if (data.error) {
      return res.status(400).json({ error: data.error_description || data.error });
    }

    // Redireciona de volta para a app com o token
    // Deep link: axev3d://callback?token=xxx
    const appDeepLink = `axev3d://callback?token=${data.access_token}`;
    return res.redirect(302, appDeepLink);

  } catch (err) {
    return res.status(500).json({ error: "Internal server error", detail: err.message });
  }
}
