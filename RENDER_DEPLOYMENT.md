# Render deployment notes

## Before deploying
- Rotate any MongoDB Atlas or SMTP credentials that were ever committed to Git. Never commit secrets.
- Create an Atlas database user, allow network access from Render (for a demo you may temporarily use `0.0.0.0/0`; restrict it for production), and copy the `mongodb+srv://...` URI.
- The existing `DataInitializer` seeds the application when its core collections are empty. It creates three demo accounts: `admin@company.com`, `hr@company.com`, and `finance@company.com`; default demo password is `demo123`. Set `DEMO_ADMIN_PASSWORD` to a unique secret before first deploy. Do not expose these demo accounts publicly in a real production system.

## Backend Web Service
- Render > New > Web Service; connect the repository; choose **Docker**; root directory `/` (repository root).
- Dockerfile path: `Dockerfile`.
- Environment:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SPRING_DATA_MONGODB_URI` = your Atlas connection string
  - `FRONTEND_URL=https://your-frontend-name.onrender.com`
  - `JWT_SECRET` = a long random secret (at least 32 random bytes)
  - `DEMO_ADMIN_PASSWORD` = strong password set before initial seed
  - Optional mail: `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_FROM`
- Render supplies `PORT`; the Docker entrypoint binds Spring Boot to it. The default is 8080.
- Health check path can be `/actuator/health`.

## Frontend Static Site
- Render > New > Static Site; connect same repo.
- Root directory: `frontend`
- Build command: `npm ci && npm run build`
- Publish directory: `dist`
- Environment variable (available at build time): `VITE_API_URL=https://your-backend-name.onrender.com`
- Redeploy after changing this variable. Vite variables are embedded during build.
- If using React Router, add a rewrite: source `/*`, destination `/index.html`, action `Rewrite`.

## CORS
`FRONTEND_URL` must be the exact origin, including `https://` and without a path. The backend's SecurityConfig reads this environment variable.

## Important notes
- `DataInitializer` is an existing seeder for multiple ERP collections (departments, positions, employees, accounts, expenses, invoices, payroll, users, etc.); it is not a separate minimal four-collection seeder because this repository's domain model is broader.
- It skips seeding if any of its core collections already contains data. If you have existing data, it will not automatically add the demo admin.
- Demo credentials are not safe for production. Disable/remove the demo seeder or set a strong `DEMO_ADMIN_PASSWORD` before first deployment and rotate it after logging in.
