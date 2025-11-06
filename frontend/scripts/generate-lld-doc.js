// This file is an ES module context; re-export to a CommonJS wrapper.
throw new Error('Use generate-lld-doc.cjs in ESM projects');

function heading(text, level = HeadingLevel.HEADING_2) {
  return new Paragraph({ text, heading: level });
}

function para(text) {
  return new Paragraph({ children: [new TextRun(text)] });
}

function bullet(text) {
  return new Paragraph({ text, bullet: { level: 0 } });
}

async function main() {
  const doc = new Document({
    sections: [
      {
        properties: {},
        children: [
          heading('Low-Level Design – Sections 2 and 3 (CyberScope)', HeadingLevel.TITLE),

          heading('2. Packages', HeadingLevel.HEADING_1),
          heading('Backend (Spring Boot, Java)'),

          heading('osint.config', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Security configuration (JWT filter chain, endpoint authorization), CORS.'),
          bullet('Key classes: SecurityConfig.'),

          heading('osint.controller', HeadingLevel.HEADING_3),
          bullet('Responsibilities: REST endpoints; bridge between HTTP and service layer.'),
          bullet('Key classes: AuthController, ApiKeysController, OsintController, HealthController.'),

          heading('osint.dto', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Request/response data transfer objects with validation.'),
          bullet('Key classes: LoginRequest, RegisterRequest, JwtResponse, ForgotPasswordRequest, ResetPasswordRequest, SmsMfaSetupRequest, SmsMfaVerifyRequest.'),

          heading('osint.model', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Persistent domain models.'),
          bullet('Key classes: User, Role, PasswordResetToken.'),

          heading('osint.repository', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Data access layer (Spring Data repositories).'),
          bullet('Key classes: UserRepository, RoleRepository, PasswordResetTokenRepository.'),

          heading('osint.service', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Business logic and third-party integrations.'),
          bullet('Key classes: AuthService, ApiKeyService, ShodanService, VirusTotalService, HaveIBeenPwnedService, SmsService.'),

          heading('osint.util', HeadingLevel.HEADING_3),
          bullet('Responsibilities: Cross-cutting helpers (e.g., JWT/TOTP utilities).'),
          bullet('Key classes: JwtUtil, TOTPUtil, TOTPVerifier.'),

          heading('resources', HeadingLevel.HEADING_3),
          bullet('application.yml: environment configuration (DB, security, API keys).'),
          bullet('db/migration: Flyway migrations (V1__init.sql).'),

          heading('Frontend (React + Vite)'),
          heading('src/components', HeadingLevel.HEADING_3),
          bullet('auth: Authentication components (MFASetup, MFAVerification, ForgotPassword, ResetPassword).'),
          bullet('layout: AppShell, Header, Sidebar.'),
          bullet('common/ui: Reusable UI elements (Button, Input, Card, RiskBadge, StatWidget).'),

          heading('src/features', HeadingLevel.HEADING_3),
          bullet('Modular screens/workflows: apikeys, auth, dashboard, entities (DomainIP, EmailBreach), scans, reports, notifications, settings, users.'),

          heading('src/store', HeadingLevel.HEADING_3),
          bullet('Global state: auth.js (token/user/MFA), ui.js (layout/notifications).'),

          heading('src/lib', HeadingLevel.HEADING_3),
          bullet('axios.js: API client and interceptors.'),
          bullet('utils.js: Utilities.'),

          heading('Dependency flow', HeadingLevel.HEADING_3),
          bullet('Backend: controller → service → repository → database; util is cross-cutting.'),
          bullet('Frontend: features/components → store → lib/axios → backend APIs.'),

          heading('Security/Contracts', HeadingLevel.HEADING_3),
          bullet('Authentication via JWT, MFA (SMS endpoints available).'),
          bullet('REST JSON contracts defined by DTOs.'),

          heading('Design principles/standards', HeadingLevel.HEADING_3),
          bullet('Separation of concerns, DTO boundary, repository pattern.'),
          bullet('Standards: REST/JSON, JSR-380 Validation, JWT (RFC 7519), TOTP (RFC 6238).'),

          heading('3. Class Interfaces', HeadingLevel.HEADING_1),

          heading('Backend — Controllers', HeadingLevel.HEADING_2),
          heading('AuthController', HeadingLevel.HEADING_3),
          bullet('POST /api/auth/register — Body: RegisterRequest — 201 Created'),
          bullet('POST /api/auth/login — Body: LoginRequest — 200 JwtResponse'),
          bullet('POST /api/auth/forgot-password — Body: ForgotPasswordRequest — 200 OK'),
          bullet('POST /api/auth/reset-password — Body: ResetPasswordRequest — 200 OK'),
          bullet('DELETE /api/auth/user/{email} — 200 OK'),
          bullet('POST /api/auth/refresh — Body: RefreshTokenRequest — 200 JwtResponse'),
          bullet('POST /api/auth/mfa/setup — Body: MfaSetupRequest — 200 MfaSetupResponse'),
          bullet('POST /api/auth/mfa/verify — Body: MfaVerifyRequest — 200 JwtResponse'),
          bullet('POST /api/auth/mfa/disable — Body: MfaSetupRequest — 200 OK'),

          heading('ApiKeysController', HeadingLevel.HEADING_3),
          bullet('GET /api/apikeys — returns availability flags for shodan, virustotal, hibp'),
          bullet('POST /api/apikeys — Body: { shodan?, virustotal?, hibp? } — 200 OK'),

          heading('OsintController', HeadingLevel.HEADING_3),
          bullet('GET /api/osint/shodan/host/{ip} — Shodan host info'),
          bullet('GET /api/osint/shodan/search?query=... — Shodan search'),
          bullet('GET /api/osint/shodan/domain/{domain} — Shodan domain info'),
          bullet('GET /api/osint/virustotal/ip/{ip} — VirusTotal IP report'),
          bullet('GET /api/osint/virustotal/domain/{domain} — VirusTotal domain report'),
          bullet('GET /api/osint/virustotal/url?url=... — VirusTotal URL report'),
          bullet('GET /api/osint/hibp/breach/{email} — HIBP email breach'),
          bullet('GET /api/osint/hibp/breach-details/{breachName} — HIBP breach details'),
          bullet('GET /api/osint/analyze/ip/{ip} — Combined Shodan + VirusTotal summary'),
          bullet('GET /api/osint/analyze/domain/{domain} — Combined Shodan + VirusTotal summary'),

          heading('HealthController', HeadingLevel.HEADING_3),
          bullet('GET /health — { status: "ok" }'),

          heading('Backend — Services', HeadingLevel.HEADING_2),
          heading('AuthService', HeadingLevel.HEADING_3),
          bullet('register(String name, String email, MultipartFile): Map'),
          bullet('login(String email, String rawPassword): JwtResponse'),
          bullet('refreshAccessToken(String refreshToken): JwtResponse'),
          bullet('forgotPassword(String email): void'),
          bullet('resetPassword(String token, String newPassword): void'),
          bullet('setupTotpMfa(String email): MfaSetupResponse'),
          bullet('verifyTotpMfa(String email, String totpToken): JwtResponse'),
          bullet('disableTotpMfa(String email): void'),

          heading('ApiKeyService', HeadingLevel.HEADING_3),
          bullet('getShodanKey(): String / setShodanKey(String): void'),
          bullet('getVirusTotalKey(): String / setVirusTotalKey(String): void'),
          bullet('getHibpKey(): String / setHibpKey(String): void'),

          heading('ShodanService', HeadingLevel.HEADING_3),
          bullet('getHostInfo(String ip): Mono<Map<String,Object>>'),
          bullet('searchHosts(String query): Mono<Map<String,Object>>'),
          bullet('getDomainInfo(String domain): Mono<Map<String,Object>>'),

          heading('VirusTotalService', HeadingLevel.HEADING_3),
          bullet('getIpReport(String ip): Mono<Map<String,Object>>'),
          bullet('getDomainReport(String domain): Mono<Map<String,Object>>'),
          bullet('getUrlReport(String url): Mono<Map<String,Object>>'),

          heading('HaveIBeenPwnedService', HeadingLevel.HEADING_3),
          bullet('checkEmailBreach(String email): Mono<Map<String,Object>>'),
          bullet('getBreachDetails(String breachName): Mono<Map<String,Object>>'),

          heading('Backend — Repositories (selected)', HeadingLevel.HEADING_2),
          bullet('UserRepository: findByEmail(String), existsByEmail(String), deleteByEmail(String).'),
          bullet('RoleRepository: findByName(String).'),
          bullet('PasswordResetTokenRepository: findByToken(String), deleteByToken(String).'),

          heading('Backend — Models (selected)', HeadingLevel.HEADING_2),
          bullet('User: id, email, passwordHash, roles, smsMfaEnabled?, phoneNumber.'),
          bullet('Role: id, name.'),
          bullet('PasswordResetToken: token, email, expiresAt.'),

          heading('Frontend — Store/Lib (selected)', HeadingLevel.HEADING_2),
          bullet('src/lib/axios.js: Axios instance with auth header handling.'),
          bullet('src/store/auth.js: token/user/MFA flags and auth actions.'),
          bullet('src/store/ui.js: layout and notification state.'),

          heading('Frontend — Components/Features (selected)', HeadingLevel.HEADING_2),
          bullet('Auth: Login, Register, ForgotPassword, ResetPassword, MFASetup, MFAVerification.'),
          bullet('OSINT features: DomainIP, EmailBreach; API Keys screen; Dashboard; Scans; Reports; Notifications; Settings; Users.'),
        ],
      },
    ],
  });

  const outDir = path.resolve(__dirname, '../../docs');
  if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
  }
  const outPath = path.join(outDir, 'Low-Level-Design-Sections-2-3.docx');
  const buffer = await Packer.toBuffer(doc);
  fs.writeFileSync(outPath, buffer);
  console.log('Generated:', outPath);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});


