import { Fragment } from "react";
import "./Trace.css";
import { STAGES } from "../lib/trace";

// Three dots (velocity, geolocation, AI) showing how far a transaction got and where it failed.
export default function PipelineDots({ trace }) {
  return (
    <span className="pipe">
      {STAGES.map((stage, i) => {
        const step = trace?.steps?.find((s) => s.stage === stage.id);
        const state = !trace ? "WAIT" : (step?.status ?? "SKIPPED");
        const text = state === "WAIT" ? "waiting" : state.toLowerCase();

        return (
          <Fragment key={stage.id}>
            {i > 0 && <span className="pipe-link" />}
            <span
              className={`pipe-dot pipe-dot-${state}`}
              title={`${stage.label}: ${text}`}
            >
              {stage.short}
            </span>
          </Fragment>
        );
      })}
    </span>
  );
}