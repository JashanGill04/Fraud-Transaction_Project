import { Fragment, useEffect, useMemo, useState } from "react";
import StatusPill from "./StatusPill";
import PipelineDots from "./PipelineDots";
import TraceTimeline from "./TraceTimeline";
import DecisionLog from "./DecisionLog";
import { formatTime, reasonLabel } from "../lib/trace";
import "./Trace.css";

function formatAmount(amount, currency) {
  if (amount === undefined || amount === null) return "—";
  return `${currency ?? ""} ${Number(amount).toFixed(2)}`;
}

// The ledger status can lag behind the analysis; fall back to the trace's verdict.
function effectiveStatus(entry, trace) {
  if (entry.status !== "PENDING") return entry.status;
  if (!trace) return "PENDING";
  return trace.verdict === "FLAGGED" ? "FLAGGED" : "APPROVED";
}

export default function Ledger({ entries, traces, loading, onRefresh, focusId }) {
  const [view, setView] = useState("ledger");
  const [openId, setOpenId] = useState(null);

  const traceById = useMemo(
    () => new Map(traces.map((t) => [t.transactionId, t])),
    [traces],
  );

  // After a submit, open that row so its timeline appears when the analysis finishes.
  useEffect(() => {
    if (focusId) {
      setView("ledger");
      setOpenId(focusId);
    }
  }, [focusId]);

  function toggle(id) {
    setOpenId((current) => (current === id ? null : id));
  }

  return (
    <div className="ledger">
      <div className="ledger-head">
        <div className="tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={view === "ledger"}
            className={`tab ${view === "ledger" ? "tab-active" : ""}`}
            onClick={() => setView("ledger")}
          >
            Ledger
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={view === "log"}
            className={`tab ${view === "log" ? "tab-active" : ""}`}
            onClick={() => setView("log")}
          >
            Decision log
          </button>
        </div>
        <div className="ledger-head-right">
          <span className="ledger-head-count">
            {view === "ledger" ? `${entries.length} total` : `${traces.length} decisions`}
          </span>
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

      {view === "log" ? (
        <DecisionLog traces={traces} />
      ) : entries.length === 0 && !loading ? (
        <div className="ledger-empty">
          Nothing here yet. Run a transaction to see it land, then click its row to see why.
        </div>
      ) : (
        <table className="ledger-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Merchant</th>
              <th>Amount</th>
              <th>Checks</th>
              <th>Outcome</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => {
              const trace = traceById.get(entry.key);
              const open = openId === entry.key;
              const failed = trace?.verdict === "FLAGGED";

              return (
                <Fragment key={entry.key}>
                  <tr
                    className={`row-click ${open ? "row-open" : ""}`}
                    tabIndex={0}
                    aria-expanded={open}
                    onClick={() => toggle(entry.key)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        toggle(entry.key);
                      }
                    }}
                  >
                    <td>{formatTime(entry.timestamp)}</td>
                    <td>{entry.merchantId ?? "—"}</td>
                    <td>{formatAmount(entry.amount, entry.currency)}</td>
                    <td>
                      <PipelineDots trace={trace} />
                    </td>
                    <td className={failed ? "why why-fail" : "why"}>{reasonLabel(trace)}</td>
                    <td>
                      <StatusPill status={effectiveStatus(entry, trace)} />
                    </td>
                  </tr>
                  {open && (
                    <tr className="trace-row">
                      <td colSpan={6}>
                        <TraceTimeline trace={trace} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}