import TraceTimeline from "./TraceTimeline";

// Every decision, newest first, each with its full step-by-step explanation.
export default function DecisionLog({ traces }) {
  if (traces.length === 0) {
    return (
      <div className="ledger-empty">
        No decisions logged yet. Run a transaction and its full explanation will appear here.
      </div>
    );
  }

  return (
    <div className="log">
      {traces.map((trace) => (
        <div className="log-card" key={trace.transactionId}>
          <TraceTimeline trace={trace} />
        </div>
      ))}
    </div>
  );
}