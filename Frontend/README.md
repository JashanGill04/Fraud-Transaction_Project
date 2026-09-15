# Fraud Console

A minimal frontend for the credit card fraud detection system's Transaction
Gateway Service. Submits a transaction and shows what the gateway returns.

## Run it

```bash
npm install
npm run dev
```

Opens on http://localhost:5173 by default.

## Pointing it at your API

The API base URL is set in one place: `src/api/transactions.js`

```js
const BASE_URL = "http://localhost:8000/api/transactions";
```

Change this if your gateway (or a Spring Cloud Gateway in front of it) runs on
a different host/port or path.

## What it does

- `src/components/SwipeForm.jsx` — the transaction input form, with basic
  client-side validation matching the gateway's `TransactionRequest` rules
  (16-digit card number, positive amount, 3-letter currency, required lat/long).
  On submit, it `POST`s to the gateway, then triggers a reload of the ledger.
- `src/components/Ledger.jsx` — the full transaction list, loaded via
  `GET /api/transactions`. Has a manual "Refresh" button, since status
  updates from the fraud analysis pipeline happen asynchronously and won't
  show up until you reload the list.
- `src/components/StatusPill.jsx` — small status badge (pending / approved /
  flagged).
- `src/api/transactions.js` — `fetchTransactions()` (GET) and
  `submitTransaction()` (POST), both against the same base URL.

## Backend requirement

This expects a `GET /api/transactions` endpoint on the gateway that returns
an array of transactions (same shape as `TransactionResponse`: transactionId,
amount, currency, merchantId, status, timestamp). If your Transaction Gateway
Service doesn't have this endpoint yet, you'll need to add it — the ledger
will show an error banner until it does.

## Known limitation

There's no live push or polling — the ledger only updates on load and on
manual refresh. If you want it to reflect fraud-analysis outcomes as they
happen, the natural next step is either a polling interval or upgrading to
Server-Sent Events / WebSockets once the Alert Service exists.
