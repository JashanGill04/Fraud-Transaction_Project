import { useEffect, useState, useCallback } from "react";
import SwipeForm from "./components/SwipeForm";
import Ledger from "./components/Ledger";
import { submitTransaction, fetchTransactions } from "./api/transactions";
import "./App.css";

export default function App() {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [banner, setBanner] = useState(null);

  const loadEntries = useCallback(async () => {
    try {
      const list = await fetchTransactions();
      const normalized = (list ?? []).map((txn) => ({
        key: txn.transactionId,
        transactionId: txn.transactionId,
        amount: txn.amount,
        currency: txn.currency,
        merchantId: txn.merchantId,
        status: txn.status ?? "PENDING",
        timestamp: txn.timestamp,
      }));
      // most recent first
      normalized.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      setEntries(normalized);
      setBanner(null);
    } catch (err) {
      setBanner(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  async function handleSubmit(payload) {
    setSubmitting(true);
    setBanner(null);

    try {
      await submitTransaction(payload);
      await loadEntries();
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
            Sends each swipe to the transaction gateway and shows what came back.
          </p>
        </div>
        <div className="app-endpoint">
          <span className="app-endpoint-dot" />
          localhost:8000/api/transactions
        </div>
      </header>

      {banner && (
        <div className="banner" role="alert">
          {banner}
        </div>
      )}

      <main className="app-main">
        <SwipeForm onSubmit={handleSubmit} submitting={submitting} />
        <Ledger entries={entries} loading={loading} onRefresh={loadEntries} />
      </main>

      <p className="app-note">
        The ledger reflects whatever the gateway currently returns from{" "}
        <code>GET /api/transactions</code> — refresh to pick up status changes
        made by the fraud analysis pipeline downstream.
      </p>
    </div>
  );
}
