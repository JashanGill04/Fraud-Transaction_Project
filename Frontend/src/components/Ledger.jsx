import StatusPill from "./StatusPill";

function formatTime(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function formatAmount(amount, currency) {
  if (amount === undefined || amount === null) return "—";
  return `${currency ?? ""} ${Number(amount).toFixed(2)}`;
}

export default function Ledger({ entries, loading, onRefresh }) {
  return (
    <div className="ledger">
      <div className="ledger-head">
        <span className="ledger-head-title">Ledger</span>
        <div className="ledger-head-right">
          <span className="ledger-head-count">{entries.length} total</span>
          <button
            type="button"
            className="ledger-refresh"
            onClick={onRefresh}
            disabled={loading}
          >
            {loading ? "Loading…" : "Refresh"}
          </button>
        </div>
      </div>

      {entries.length === 0 && !loading ? (
        <div className="ledger-empty">
          Nothing here yet — run a transaction to see it land.
        </div>
      ) : (
        <table className="ledger-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Merchant</th>
              <th>Amount</th>
              <th>Transaction ID</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr key={entry.key}>
                <td>{formatTime(entry.timestamp)}</td>
                <td>{entry.merchantId ?? "—"}</td>
                <td>{formatAmount(entry.amount, entry.currency)}</td>
                <td className="mono-id">
                  {entry.transactionId ? entry.transactionId.slice(0, 8) : "—"}
                </td>
                <td>
                  <StatusPill status={entry.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
