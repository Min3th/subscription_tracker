# Frontend Testing

The frontend test suite uses Vitest, Testing Library, user-event, and jest-axe.

## Commands

```bash
npm test
npm run test:watch
npm run test:coverage
npm run test:e2e
npm run test:e2e:headed
npm run test:e2e:ui
```

Run `npm run lint` and `npm run build` alongside the test suite before merging.
Pull requests that change `webapp/` run the test suite and production build through
`.github/workflows/frontend-ci.yaml`.

## Testing approach

- Unit-test branching, exact calculations, parsing, and state helpers.
- Component-test visible behavior and access rules.
- Query controls by their accessible role, name, label, or visible text.
- Use `user-event` for keyboard and pointer behavior.
- Run axe checks on important rendered components.
- Avoid asserting implementation details, CSS class names, component internals, or large snapshots.
- Keep end-to-end tests for a later suite covering a small number of critical full-stack workflows.

## End-to-end browser coverage

The Playwright suite covers:

- Public landing-page rendering and keyboard interaction.
- Anonymous rejection from protected routes.
- Authenticated subscription listing, searching, and creation.
- Reminder preference changes.
- Automated WCAG accessibility checks on public and authenticated pages.

`npm run test:e2e` runs against Playwright's Chromium, Firefox, and WebKit
projects. WebKit provides Safari-engine compatibility coverage; Playwright does
not automate the branded Safari application. Firefox uses Playwright's patched
Firefox build.

Google Chrome can be tested with:

```bash
npm run test:e2e:chrome
```

Brave uses Chromium but is not a built-in Playwright channel. To test an
installed Brave browser, provide its executable:

```powershell
$env:BRAVE_EXECUTABLE_PATH="C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe"
npm run test:e2e:brave
```

```bash
BRAVE_EXECUTABLE_PATH=/path/to/brave npm run test:e2e:brave
```

API calls are fulfilled by deterministic browser-level fixtures. These tests
verify frontend behavior independently of backend availability; a separate
deployed-environment smoke suite should cover real frontend/backend integration.

Automated accessibility checks catch only a subset of accessibility problems. Important flows still need keyboard, screen-reader, zoom, contrast, and browser spot checks.

## References

- Chromatic, “How to actually test frontend”: https://www.chromatic.com/frontend-testing-guide
- Testing Library guiding principles: https://testing-library.com/docs/guiding-principles/
- Vitest guide: https://vitest.dev/guide/
- Material UI testing guidance: https://mui.com/material-ui/guides/testing/
- W3C ARIA Authoring Practices: https://www.w3.org/WAI/ARIA/apg/practices/read-me-first/
