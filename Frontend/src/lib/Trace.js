// The pipeline stages, in the order the backend runs them.
export const STAGES = [
  { id: "VELOCITY", short: "V", label: "Velocity" },
  { id: "GEOLOCATION", short: "G", label: "Geolocation" },
  { id: "AI", short: "AI", label: "AI check" },
];

const REASONS = {
  VELOCITY_EXCEEDED: "Too many swipes",
  IMPOSSIBLE_TRAVEL: "Impossible travel",
  AI_ANOMALY: "Flagged by the AI",
};

export function reasonLabel(trace) {
  if (!trace) return "Analyzing…";
  if (!trace.reason) return "Passed every check";
  return REASONS[trace.reason] ?? trace.reason;
}

export function formatTime(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}