const STYLES = {
  PENDING: { label: "Pending review", className: "pill pill-pending" },
  APPROVED: { label: "Approved", className: "pill pill-approved" },
  FLAGGED: { label: "Flagged", className: "pill pill-flagged" },
  REJECTED: { label: "Rejected", className: "pill pill-flagged" },
  ERROR: { label: "Failed", className: "pill pill-flagged" },
};

export default function StatusPill({ status }) {
  const style = STYLES[status] ?? STYLES.PENDING;
  return <span className={style.className}>{style.label}</span>;
}
