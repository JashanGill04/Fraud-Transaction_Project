import { useState } from "react";

const HOME = { latitude: "31.6340", longitude: "74.8723" };
const MUMBAI = { latitude: "19.0760", longitude: "72.8777" };

const initialForm = {
  cardNumber: "",
  amount: "",
  merchantId: "",
  currency: "INR",
  latitude: "",
  longitude: "",
};

const initialErrors = {};

function randomCard() {
  // 8 fixed digits + 8 random digits = 16 digits, which the gateway requires
  return "41111111" + String(Math.floor(10000000 + Math.random() * 89999999));
}

function randomBetween(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2);
}

export default function SwipeForm({ onSubmit, submitting }) {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState(initialErrors);

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: undefined }));
    }
  }

  function validate() {
    const next = {};
    if (!/^\d{16}$/.test(form.cardNumber)) {
      next.cardNumber = "16 digits required";
    }
    if (!form.amount || Number(form.amount) <= 0) {
      next.amount = "Enter an amount above 0";
    }
    if (!form.merchantId.trim()) {
      next.merchantId = "Required";
    }
    if (!/^[A-Za-z]{3}$/.test(form.currency)) {
      next.currency = "3-letter code";
    }
    if (form.latitude === "" || Number.isNaN(Number(form.latitude))) {
      next.latitude = "Required";
    }
    if (form.longitude === "" || Number.isNaN(Number(form.longitude))) {
      next.longitude = "Required";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function toPayload(values) {
    return {
      cardNumber: values.cardNumber,
      amount: Number(values.amount),
      merchantId: values.merchantId.trim(),
      currency: values.currency.toUpperCase(),
      latitude: Number(values.latitude),
      longitude: Number(values.longitude),
    };
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!validate()) return;
    onSubmit(toPayload(form));
  }

  function fillSample() {
    setForm({
      cardNumber: randomCard(),
      amount: randomBetween(50, 900),
      merchantId: "MERCH-" + Math.floor(100 + Math.random() * 899),
      currency: "INR",
      ...HOME,
    });
    setErrors({});
  }

  // Scenarios keep the card you already typed, so velocity and travel checks can build on it.
  function currentCard() {
    return /^\d{16}$/.test(form.cardNumber) ? form.cardNumber : randomCard();
  }

  function scenario(kind) {
    const cardNumber = currentCard();
    const base = { cardNumber, currency: "INR", merchantId: "MERCH-102", ...HOME };

    if (kind === "normal") {
      setForm({ ...base, amount: randomBetween(100, 900) });
    } else if (kind === "big") {
      setForm({ ...base, merchantId: "MERCH-731", amount: randomBetween(40000, 90000) });
    } else if (kind === "travel") {
      setForm({ ...base, amount: randomBetween(100, 900), ...MUMBAI });
    }
    setErrors({});
  }

  async function burst() {
    const values = {
      cardNumber: currentCard(),
      currency: "INR",
      merchantId: "MERCH-102",
      ...HOME,
    };
    setForm((prev) => ({ ...prev, cardNumber: values.cardNumber }));
    // Five swipes in a row on one card: the velocity rule should trip on the 4th
    for (let i = 0; i < 5; i++) {
      await onSubmit(toPayload({ ...values, amount: randomBetween(100, 900) }));
    }
  }

  return (
    <form className="slip" onSubmit={handleSubmit} noValidate>
      <div className="slip-head">
        <span className="slip-head-title">New swipe</span>
        <button type="button" className="slip-sample" onClick={fillSample}>
          Fill sample
        </button>
      </div>

      <div className="slip-scenarios">
        <span className="slip-scenarios-label">Try a scenario</span>
        <button type="button" className="slip-sample" onClick={() => scenario("normal")}>
          Normal purchase
        </button>
        <button type="button" className="slip-sample" onClick={() => scenario("big")}>
          Big spend
        </button>
        <button type="button" className="slip-sample" onClick={() => scenario("travel")}>
          Swipe in Mumbai
        </button>
        <button type="button" className="slip-sample" onClick={burst} disabled={submitting}>
          Send 5 quickly
        </button>
      </div>

      <Field label="Card number" error={errors.cardNumber} htmlFor="cardNumber">
        <input
          id="cardNumber"
          inputMode="numeric"
          maxLength={16}
          placeholder="4111 1111 1111 1234"
          value={form.cardNumber}
          onChange={(e) => update("cardNumber", e.target.value.replace(/\D/g, ""))}
        />
      </Field>

      <div className="slip-row">
        <Field label="Amount" error={errors.amount} htmlFor="amount">
          <input
            id="amount"
            inputMode="decimal"
            placeholder="0.00"
            value={form.amount}
            onChange={(e) => update("amount", e.target.value)}
          />
        </Field>
        <Field label="Currency" error={errors.currency} htmlFor="currency">
          <input
            id="currency"
            maxLength={3}
            placeholder="INR"
            value={form.currency}
            onChange={(e) => update("currency", e.target.value.toUpperCase())}
          />
        </Field>
      </div>

      <Field label="Merchant ID" error={errors.merchantId} htmlFor="merchantId">
        <input
          id="merchantId"
          placeholder="MERCH-001"
          value={form.merchantId}
          onChange={(e) => update("merchantId", e.target.value)}
        />
      </Field>

      <div className="slip-row">
        <Field label="Latitude" error={errors.latitude} htmlFor="latitude">
          <input
            id="latitude"
            inputMode="decimal"
            placeholder="31.6340"
            value={form.latitude}
            onChange={(e) => update("latitude", e.target.value)}
          />
        </Field>
        <Field label="Longitude" error={errors.longitude} htmlFor="longitude">
          <input
            id="longitude"
            inputMode="decimal"
            placeholder="74.8723"
            value={form.longitude}
            onChange={(e) => update("longitude", e.target.value)}
          />
        </Field>
      </div>

      <button type="submit" className="slip-submit" disabled={submitting}>
        {submitting ? "Sending…" : "Run transaction"}
      </button>
    </form>
  );
}

function Field({ label, error, htmlFor, children }) {
  return (
    <label className="field" htmlFor={htmlFor}>
      <span className="field-label">{label}</span>
      {children}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}