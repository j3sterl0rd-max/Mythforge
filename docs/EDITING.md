# Content editing reference

Edit `data/site.json`, run `npm run build`, then `npm run check`. Commit both source and generated output when publishing. Cloudflare rebuilds from source on each push.

## Release notes

`changelog` is an array, ordered newest first. Replace the example values with verified release information; this is a schema example only:

```json
{
  "version": "X.Y.Z",
  "date": "2026-10-13",
  "title": "Your verified release title",
  "additions": ["An actual addition"],
  "fixes": ["An actual fix"],
  "balance": [],
  "server": ["An actual server-specific change"]
}
```

Empty categories are hidden. Each release appears in semantic HTML even with JavaScript disabled. A searchable filter appears automatically when entries exist. Dates use ISO `YYYY-MM-DD`. Do not present the example as a real release.

## Supporter packages

Each `supporterPackages` item uses this shape:

```json
{
  "name": "Your package name",
  "tag": "SHORT CATEGORY LABEL",
  "description": "A short, accurate package description.",
  "features": ["A confirmed reward", "Its applicable limits"],
  "price": null,
  "url": null,
  "active": false
}
```

For a published package, set `active` to `true`, provide a valid HTTPS external checkout `url`, and set `price` to the exact display string or leave it `null` for “See external checkout.” A package without both active state and valid checkout URL is not offered for sale.

The current supporter introduction deliberately describes proposed packages. When launching real packages, update this explanatory copy in the `/support/` page template in `scripts/build.mjs` to reflect the finalized program, and publish the terms, privacy and refund policies. Do not leave “illustrative” wording around packages that are actually sold.

`links.store` sets the general store destination. It does not activate packages individually. Checkout stays off-site; this project does not receive payment details.

`playtimeUnlocks` is separate and contains objects with `title` and `text`. Replace proposed unlocks with confirmed thresholds and limits when ready. Explain clearly which functional rewards are also obtainable by playing.

## Rules

`rules` contains objects with `title` and `text`. They are rendered in order with automatic numbering and anchor links. Add, remove or reorder freely. Set `rulesDraft` to `false` only after the final rules are ready. PvP, abandoned-base and automation details should reflect real staff policy.

## Policies

Fill `policies.terms`, `policies.privacy` and `policies.refunds` with plain text. Use `\n\n` for paragraph breaks and `\n` for line breaks. Text is HTML-escaped. Unfilled policies remain labeled as awaiting publication.

## Styling

Edit `public/assets/site.css`, especially the `:root` variables:

- `--ink`, `--panel`, `--paper`, `--muted`: readable dark theme
- `--brass`, `--violet`: brand accents
- `--line`: separators
- `--max`, `--space`: content width and section rhythm
- `--serif`, `--sans`: local system font stacks

Rebuild after CSS or JavaScript changes. There is no external stylesheet or font service to configure.
