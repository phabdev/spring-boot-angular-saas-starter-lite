# Safe tasks for coding agents

Read AGENTS.md, then select one scoped task and state its expected behavior before editing.

1. Add a Customer entity using `docs/starter-kit/add-new-entity.md`; verify cross-user access returns 404.
2. Add project pagination on both API and UI; verify stable ordering and page bounds with more than one page of data.
3. In Pro, replace the SMTP adapter with a chosen provider; preserve generic forgot-password responses and test one-use token behavior.
4. In Pro, add a business permission through the backend permission map and UI visibility; verify server denial independent of hidden controls.
5. Prepare a deployment using `docs/starter-kit/deployment.md`; keep secrets outside Git and record a restore test.

Every handoff includes changed behavior, test commands/results, manual checks still needed and assumptions. Billing promotion from test to live requires a separate scoped design and validation task.
