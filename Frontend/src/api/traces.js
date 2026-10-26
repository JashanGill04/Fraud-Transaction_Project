const BASE_URL = "http://localhost:8000/api/analysis/traces";

/**
 * Fetches the most recent decision traces (newest first).
 * Each trace holds the verdict, the reason, and every pipeline step.
 */
export async function fetchTraces(limit = 100) {
  const res = await fetch(`${BASE_URL}?limit=${limit}`);

  if (!res.ok) {
    throw new Error(`Failed to load decision traces (status ${res.status})`);
  }

  return res.json();
}