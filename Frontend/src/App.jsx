import { useEffect, useState, useCallback, useRef } from "react";
import SwipeForm from "./components/SwipeForm";
import Ledger from "./components/Ledger";
import { submitTransaction, fetchTransactions } from "./api/transactions";
import { fetchTraces } from "./api/traces";
import "./App.css";

const POLL_INTERVAL_MS = 2500;
const POLL_WINDOW_MS = 90000; // the first AI call can be slow while the model loads

export default function App() {
  const [entries, setEntries] = useState([]);
  const [traces, setTraces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [banner, setBanner] = useState(null);
  const [focusId, setFocusId] = useState(null);
  const pollUntil = useRef(0);

  const loadAll = useCallback(async () => {
    // allSettled: if the trace endpoint is down, the ledger still loads
    const [txResult, traceResult] = await Promise.allSettled([
      fetchTransactions(),
      fetchTraces(),
    ]);

    const problems = [];
    let normalized = null;
    let traceList = null;

    if (txResult.status === "fulfilled") {
      normalized = (txResult.value ?? []).map((txn) => {
        // the gateway returns the entity (id), the create call returns transactionId
        const id = txn.transactionId ?? txn.id;
        return {
          key: id,
          transactionId: id,
          amount: txn.amount,
          currency: txn.currency,
          merchantId: txn.merchantId,
          status: txn.status ?? "PENDING",
          timestamp: txn.timestamp,
        };
      });
      normalized.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      setEntries(normalized);
    } else {
      problems.push(txResult.reason.message);
    }

    if (traceResult.status === "fulfilled") {
      traceList = traceResult.value ?? [];
      setTraces(traceList);
    } else {
      problems.push(`Decision details unavailable: ${traceResult.reason.message}`);
    }

    setBanner(problems.length > 0 ? problems.join(". ") : null);
    setLoading(false);

    // Stop polling once nothing is waiting for its analysis
    if (normalized && traceList) {
      const traced = new Set(traceList.map((t) => t.transactionId));
      const waiting = normalized.some((e) => e.status === "PENDING" && !traced.has(e.key));
      if (!waiting) pollUntil.current = 0;
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  useEffect(() => {
    const timer = setInterval(() => {
      if (Date.now() < pollUntil.current) loadAll();
    }, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [loadAll]);

  async function handleSubmit(payload) {
    setSubmitting(true);
    setBanner(null);

    try {
      const created = await submitTransaction(payload);
      setFocusId(created?.transactionId ?? null);
      pollUntil.current = Date.now() + POLL_WINDOW_MS;
      await loadAll();
    } catch (err) {
      setBanner(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Fraud Console</h1>
          <p className="app-subtitle">
            Send a swipe and watch each check decide: velocity, geolocation, then the AI.
            Click any row to see why it passed or failed.
          </p>
        </div>
        <div className="app-endpoint">
          <span className="app-endpoint-dot" />
          localhost:8000/api
        </div>
      </header>

      {banner && (
        <div className="banner" role="alert">
          {banner}
        </div>
      )}

      <main className="app-main">
        <SwipeForm onSubmit={handleSubmit} submitting={submitting} />
        <Ledger
          entries={entries}
          traces={traces}
          loading={loading}
          onRefresh={loadAll}
          focusId={focusId}
        />
      </main>

      <p className="app-note">
        New swipes refresh on their own for about a minute while the analysis runs.
        Rule checks finish instantly; the AI check can take several seconds.
      </p>
    </div>
  );
}