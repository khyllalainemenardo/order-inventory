import { clock } from '../api.js'

const KIND = {
  ORDER_CONFIRMED: { className: 'confirmed', label: 'Confirmed' },
  ORDER_REJECTED: { className: 'rejected', label: 'Rejected' },
  ORDER_CANCELLED: { className: 'cancelled', label: 'Cancelled' },
  LOW_STOCK: { className: 'low-stock', label: 'Reorder' }
}

export default function ActivityFeed({ entries }) {
  return (
    <section className="feed" aria-labelledby="feed-heading">
      <h2 id="feed-heading">Activity</h2>
      {entries.length === 0 ? (
        <p className="hint">Nothing yet. Order confirmations, rejections and reorder alerts appear here.</p>
      ) : (
        <ol className="feed-list">
          {entries.map((entry) => {
            const kind = KIND[entry.type] ?? { className: '', label: entry.type }
            return (
              <li key={entry.notificationId} className={`feed-entry ${kind.className}`}>
                <p className="feed-meta">
                  <span className="feed-kind">{kind.label}</span>
                  <time dateTime={entry.createdAt}>{clock(entry.createdAt)}</time>
                </p>
                <p className="feed-message">{entry.message}</p>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}
