import "./Trace.css";
import { formatTime, reasonLabel } from "../lib/trace";

const ICON = { PASS: "✓", FAIL: "✕", SKIPPED: "–", ERROR: "!" };
const STATUS_TEXT = { PASS: "Passed", FAIL: "Failed", SKIPPED: "Skipped", ERROR: "Error" };

// Retrieved knowledge arrives as "[CASE - FRAUD] text"; split the tag from the text.
function ContextItem({ text }) {
  const match = /^\[(.+?)\]\s*([\s\S]*)$/.exec(text);
  const tag = match ? match[1] : "DOC";
  const body = match ? match[2] : text;
  const kind = tag.includes("FRAUD") ? "FRAUD" : tag.includes("LEGIT") ? "LEGIT" : "RULE";

  return (
    <li>
      <span className={`chip chip-${kind}`}>{tag}</span>
      <span>{body}</span>
    </li>
  );
}

function DetailValue({ label, value }) {
  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="trace-dim">nothing retrieved</span>;
    return (
      <ul className="ctx">
        {value.map((item, i) => (
          <ContextItem key={i} text={String(item)} />
        ))}
      </ul>
    );
  }
  if (/search text|response|error/i.test(label)) {
    return <span className="mono-block">{String(value)}</span>;
  }
  return <>{String(value)}</>;
}

function Step({ step }) {
  const entries = Object.entries(step.details ?? {});

  return (
    <li className={`step step-${step.status}`}>
      <span className="step-node" aria-hidden="true">
        {ICON[step.status] ?? "?"}
      </span>
      <div>
        <div className="step-head">
          <span className="step-title">{step.title}</span>
          <span className="step-status">{STATUS_TEXT[step.status] ?? step.status}</span>
        </div>
        <p className="step-summary">{step.summary}</p>
        {entries.length > 0 && (
          <dl className="kv">
            {entries.map(([label, value]) => (
              <div className="kv-row" key={label}>
                <dt>{label}</dt>
                <dd>
                  <DetailValue label={label} value={value} />
                </dd>
              </div>
            ))}
          </dl>
        )}
      </div>
    </li>
  );
}

export default function TraceTimeline({ trace }) {
  if (!trace) {
    return (
      <div className="trace">
        <p className="trace-dim">
          Waiting for the fraud analysis service. The first AI check can take about
          10 seconds while the model loads.
        </p>
      </div>
    );
  }

  const flagged = trace.verdict === "FLAGGED";

  return (
    <div className="trace">
      <div className="trace-meta">
        <span className={`pill ${flagged ? "pill-flagged" : "pill-approved"}`}>
          {flagged ? "Flagged" : "Approved"}
        </span>
        <strong>{reasonLabel(trace)}</strong>
        <span className="trace-dim">
          {trace.merchantId}, {trace.currency} {Number(trace.amount).toFixed(2)}
        </span>
        <span className="trace-dim">
          Decided in {trace.durationMs} ms at {formatTime(trace.decidedAt)}
        </span>
        <span className="trace-dim mono-id">{String(trace.transactionId).slice(0, 8)}</span>
      </div>

      <ol className="timeline">
        {(trace.steps ?? []).map((step) => (
          <Step key={step.stage} step={step} />
        ))}
      </ol>
    </div>
  );
}