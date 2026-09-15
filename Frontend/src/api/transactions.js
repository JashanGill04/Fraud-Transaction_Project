const BASE_URL = "http://localhost:8000/api/transactions";

/**
 * Fetches all transactions currently known to the gateway.
 * Throws with a readable message on non-2xx responses.
 */
export async function fetchTransactions() {
  const res = await fetch(BASE_URL, { method: "GET" });

  if (!res.ok) {
    throw new Error(`Failed to load transactions (status ${res.status})`);
  }

  return res.json();
}

/**
 * Submits a transaction to the gateway service.
 * Throws with a readable message on non-2xx responses.
 */
export async function submitTransaction(payload) {
  const res = await fetch(BASE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    const message =
      data?.fieldErrors
        ? Object.entries(data.fieldErrors)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(" · ")
        : data?.message || `Request failed with status ${res.status}`;
    throw new Error(message);
  }

  return data;
}
