# Disposable Mail — Backend (Railway)

Spring Boot REST API. Deployed to Railway. Frontend lives in a separate repo, deployed to Netlify.

## ⚠️ Read this first — the SMTP limitation

Railway (and Render, Heroku, Vercel, Netlify) **blocks inbound port 25**. The
embedded SMTP listener (`SmtpListener.java`) physically cannot receive real
email on Railway — this isn't a config bug, it's a platform-level restriction
every PaaS enforces to stop spam relays.

**`smtp.enabled` defaults to `false`** in this repo for that reason. Two ways forward:

### Option A — Inbound email via a relay provider (works on Railway)
Use a service that receives SMTP on your behalf and forwards each message to
your app over HTTPS. `InboundEmailController.java` already exposes
`POST /api/inbound-email` for this.

1. Sign up for **Mailgun** (has a free tier) or SendGrid Inbound Parse.
2. Add + verify your domain in their dashboard.
3. Point your domain's **MX records** to the provider (they'll give you the values).
4. Create a route: recipient matches `@yourdomain.com` → forward to
   `https://<your-railway-app>.up.railway.app/api/inbound-email`.
5. Leave `SMTP_ENABLED=false` in Railway.

### Option B — Self-host on a VPS instead of Railway
If you want the original embedded-SMTP + Postfix setup working as-is, it
needs a real Linux VPS (DigitalOcean, Linode, EC2) where you control port 25.
`DEPLOYMENT.sh` in this repo still does that. Railway is not compatible with
this approach — pick one or the other for the backend.

---

## Deploying to Railway

1. Push this repo to GitHub.
2. In Railway: **New Project → Deploy from GitHub repo** → select this repo.
3. **Add a MySQL database**: New → Database → MySQL. Railway auto-injects
   `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
   into your app's environment — you don't need to set these manually.
4. In your app's **Variables** tab, add:
   ```
   MAIL_DOMAIN=yourdomain.com
   SMTP_ENABLED=false
   ALLOWED_ORIGINS=https://your-frontend.netlify.app
   ```
5. Railway auto-builds using `nixpacks.toml` (Java 17 + Maven) and starts
   with the command in `railway.json`.
6. Once deployed, Railway gives you a public URL like
   `https://disposable-mail-backend-production.up.railway.app`. Copy it —
   the frontend repo needs it.
7. Run the schema once against the Railway MySQL database (Railway's DB tab
   has a "Connect" button with a `mysql` CLI command, or use a GUI client
   like TablePlus/DBeaver with the credentials shown there):
   ```bash
   mysql -h <MYSQLHOST> -P <MYSQLPORT> -u <MYSQLUSER> -p<MYSQLPASSWORD> <MYSQLDATABASE> < src/main/resources/schema.sql
   ```

## Connecting the frontend

In Railway → your app → Variables, set `ALLOWED_ORIGINS` to your exact
Netlify URL (no trailing slash), e.g.:
```
ALLOWED_ORIGINS=https://disposable-mail.netlify.app
```
Multiple origins (e.g. custom domain + Netlify subdomain) are comma-separated:
```
ALLOWED_ORIGINS=https://mail.example.com,https://disposable-mail.netlify.app
```

## Local development

```bash
cp .env.example .env
# edit .env with local MySQL credentials
export $(cat .env | xargs)
mysql -u root -p < src/main/resources/schema.sql
mvn spring-boot:run
```

## API

See the frontend repo's README, or:
- `POST /api/inbox/generate`
- `GET  /api/inbox/{address}/emails?search=`
- `GET  /api/email/{id}`
- `DELETE /api/email/{id}`
- `DELETE /api/inbox/{id}`
- `POST /api/inbound-email` (webhook for Mailgun/SendGrid, see above)
